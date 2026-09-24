package com.eduol4.jennyaddon;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Faz a Jenny passiva seguir o jogador mais proximo, mantendo certa distancia,
 * sem nunca atacar. Fica inativa enquanto ela tiver um alvo de combate (ou seja,
 * quando estiver provocada), deixando o combate assumir.
 */
public class PassiveFollowGoal extends Goal {

    private static final double SEARCH_RADIUS = 32.0D;      // ate onde ela "detecta" o jogador
    private static final double SPEED = 0.64D;              // 0.75 x a velocidade normal (0.85)
    private static final float  STOP_DISTANCE = 18.0F;      // para de se aproximar a 18 blocos (a "folga")
    private static final double GIVE_UP_DISTANCE_SQR = 40.0D * 40.0D; // desiste se ficar longe demais

    private final PathfinderMob mob;
    private Player following;
    private int recalcCooldown;

    public PassiveFollowGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() != null) {
            return false; // provocada: quem manda e o combate
        }
        Player p = this.mob.level().getNearestPlayer(this.mob, SEARCH_RADIUS);
        if (p == null || p.isSpectator() || !p.isAlive()) {
            return false;
        }
        if (this.mob.distanceToSqr(p) < (double) (STOP_DISTANCE * STOP_DISTANCE)) {
            return false; // ja esta perto o suficiente
        }
        this.following = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mob.getTarget() != null) {
            return false;
        }
        if (this.following == null || !this.following.isAlive() || this.following.isSpectator()) {
            return false;
        }
        double d = this.mob.distanceToSqr(this.following);
        return d > (double) (STOP_DISTANCE * STOP_DISTANCE) && d < GIVE_UP_DISTANCE_SQR;
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
            if (this.mob.distanceToSqr(this.following) > (double) (STOP_DISTANCE * STOP_DISTANCE)) {
                this.mob.getNavigation().moveTo(this.following, SPEED);
            } else {
                this.mob.getNavigation().stop();
            }
        }
    }
}
