package com.eduol4.jennyaddon;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Faz a Jenny passiva seguir o jogador mais proximo, mantendo distancia, sem atacar.
 * Fica inativa se ela estiver sentada ou se tiver um alvo de combate (provocada).
 * A folga e 18 blocos quando selvagem e 12 quando domesticada.
 */
public class PassiveFollowGoal extends Goal {

    private static final double SEARCH_RADIUS = 32.0D;
    private static final double SPEED = 0.64D;                 // 0.75 x a velocidade normal (0.85)
    private static final float  STOP_WILD  = 18.0F;           // folga quando selvagem
    private static final float  STOP_TAMED = 12.0F;           // folga quando domesticada
    private static final double GIVE_UP_DISTANCE_SQR = 40.0D * 40.0D;

    private final PathfinderMob mob;
    private Player following;
    private int recalcCooldown;

    public PassiveFollowGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private float stopDistance() {
        return this.mob.getPersistentData().getBoolean(JennyAddon.TAG_TAMED) ? STOP_TAMED : STOP_WILD;
    }

    private boolean isSitting() {
        return this.mob.getPersistentData().getBoolean(JennyAddon.TAG_SITTING);
    }

    @Override
    public boolean canUse() {
        if (this.isSitting() || this.mob.getTarget() != null) {
            return false;
        }
        Player p = this.mob.level().getNearestPlayer(this.mob, SEARCH_RADIUS);
        if (p == null || p.isSpectator() || !p.isAlive()) {
            return false;
        }
        float stop = this.stopDistance();
        if (this.mob.distanceToSqr(p) < (double) (stop * stop)) {
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
        if (this.following == null || !this.following.isAlive() || this.following.isSpectator()) {
            return false;
        }
        float stop = this.stopDistance();
        double d = this.mob.distanceToSqr(this.following);
        return d > (double) (stop * stop) && d < GIVE_UP_DISTANCE_SQR;
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
}
