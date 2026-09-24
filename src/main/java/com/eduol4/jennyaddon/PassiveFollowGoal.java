package com.eduol4.jennyaddon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Seguir passivo. Selvagem segue o jogador mais proximo (folga 18); domesticada segue
 * o DONO (folga 12) e se teletransporta ate ele quando fica longe demais (estilo lobo).
 * Fica inativa se estiver sentada ou com um alvo de combate (provocada).
 */
public class PassiveFollowGoal extends Goal {

    private static final double SEARCH_RADIUS = 32.0D;                 // so selvagem (jogador mais proximo)
    private static final double SPEED = 0.64D;                         // 0.75 x a velocidade normal
    private static final float  STOP_WILD  = 18.0F;                    // folga selvagem
    private static final float  STOP_TAMED = 12.0F;                    // folga domesticada
    private static final double GIVE_UP_DISTANCE_SQR = 40.0D * 40.0D;  // selvagem desiste; domada nao
    private static final double TELEPORT_DISTANCE_SQR = 30.0D * 30.0D; // domada se teletransporta ao dono

    private final PathfinderMob mob;
    private Player following;
    private int recalcCooldown;

    public PassiveFollowGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean isTamed() {
        return this.mob.getPersistentData().getBoolean(JennyAddon.TAG_TAMED);
    }

    private boolean isSitting() {
        return this.mob.getPersistentData().getBoolean(JennyAddon.TAG_SITTING);
    }

    private float stopDistance() {
        return this.isTamed() ? STOP_TAMED : STOP_WILD;
    }

    /** Domada segue o dono (a qualquer distancia); selvagem segue o jogador mais proximo no raio. */
    private Player resolveFollowTarget() {
        CompoundTag data = this.mob.getPersistentData();
        if (data.getBoolean(JennyAddon.TAG_TAMED)) {
            if (!data.hasUUID(JennyAddon.TAG_OWNER)) {
                return null;
            }
            UUID ownerId = data.getUUID(JennyAddon.TAG_OWNER);
            Player owner = this.mob.level().getPlayerByUUID(ownerId);
            if (owner == null || owner.isSpectator() || !owner.isAlive()) {
                return null;
            }
            return owner;
        }
        Player p = this.mob.level().getNearestPlayer(this.mob, SEARCH_RADIUS);
        if (p == null || p.isSpectator() || !p.isAlive()) {
            return null;
        }
        return p;
    }

    @Override
    public boolean canUse() {
        if (this.isSitting() || this.mob.getTarget() != null) {
            return false;
        }
        Player p = this.resolveFollowTarget();
        if (p == null) {
            return false;
        }
        float stop = this.stopDistance();
        if (this.mob.distanceToSqr(p) <= (double) (stop * stop)) {
            return false;
        }
        this.following = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.isSitting() || this.mob.getTarget() != null) {
            return false;
        }
        Player p = this.resolveFollowTarget();
        if (p == null) {
            return false;
        }
        this.following = p;
        float stop = this.stopDistance();
        double d = this.mob.distanceToSqr(p);
        if (d <= (double) (stop * stop)) {
            return false;
        }
        if (!this.isTamed() && d >= GIVE_UP_DISTANCE_SQR) {
            return false; // selvagem desiste; domada usa teletransporte
        }
        return true;
    }

    @Override
    public void start() {
        this.recalcCooldown = 0;
    }

    @Override
    public void stop() {
        this.following = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.following == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(this.following, 10.0F, (float) this.mob.getMaxHeadXRot());

        // Teletransporte: so domada, quando o dono fica longe demais.
        if (this.isTamed() && this.mob.distanceToSqr(this.following) > TELEPORT_DISTANCE_SQR) {
            this.tryTeleportToOwner(this.following);
            return;
        }

        if (--this.recalcCooldown <= 0) {
            this.recalcCooldown = this.adjustedTickDelay(10);
            float stop = this.stopDistance();
            if (this.mob.distanceToSqr(this.following) > (double) (stop * stop)) {
                this.mob.getNavigation().moveTo(this.following, SPEED);
            } else {
                this.mob.getNavigation().stop();
            }
        }
    }

    // ---- teletransporte (estilo lobo) ----

    private void tryTeleportToOwner(Player owner) {
        BlockPos op = owner.blockPosition();
        for (int i = 0; i < 10; i++) {
            int dx = this.mob.getRandom().nextInt(7) - 3; // -3..3
            int dy = this.mob.getRandom().nextInt(3) - 1; // -1..1
            int dz = this.mob.getRandom().nextInt(7) - 3; // -3..3
            if (this.tryTeleportTo(op.getX() + dx, op.getY() + dy, op.getZ() + dz)) {
                return;
            }
        }
    }

    private boolean tryTeleportTo(int x, int y, int z) {
        Level lvl = this.mob.level();
        BlockPos below = new BlockPos(x, y - 1, z);
        if (!lvl.getBlockState(below).isFaceSturdy(lvl, below, Direction.UP)) {
            return false; // precisa de chao solido embaixo
        }
        double tx = x + 0.5D;
        double ty = (double) y;
        double tz = z + 0.5D;
        AABB dest = this.mob.getDimensions(this.mob.getPose()).makeBoundingBox(tx, ty, tz);
        if (!lvl.noCollision(this.mob, dest)) {
            return false; // precisa de espaco livre para o corpo (grande) dela
        }
        this.mob.moveTo(tx, ty, tz, this.mob.getYRot(), this.mob.getXRot());
        this.mob.getNavigation().stop();
        return true;
    }
}
