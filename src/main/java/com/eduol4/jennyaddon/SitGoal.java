package com.eduol4.jennyaddon;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Quando a Jenny domesticada esta "sentada", ela fica parada e exibe a animacao de agachar.
 * Cede a vez se ela tiver um alvo (ou seja, se for atacada, ela levanta para se defender).
 *
 * A flag de agachar e reforcada a cada tick porque o proprio mod a reescreve todo tick;
 * como a IA roda depois dessa escrita, o nosso set no tick() e o ultimo do tick e prevalece.
 */
public class SitGoal extends Goal {

    private final PathfinderMob mob;

    public SitGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.mob.getPersistentData().getBoolean(JennyAddon.TAG_TAMED)
            && this.mob.getPersistentData().getBoolean(JennyAddon.TAG_SITTING)
            && this.mob.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
        JennyCompat.setCrouching(this.mob, true);
    }

    @Override
    public void stop() {
        JennyCompat.setCrouching(this.mob, false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.mob.getNavigation().stop();
        JennyCompat.setCrouching(this.mob, true); // reforca a cada tick para vencer a escrita do mod
    }
}
