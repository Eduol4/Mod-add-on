package com.eduol4.jennyaddon.mixin;

import com.eduol4.jennyaddon.JennyCheck;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Faz uma Jenny domesticada deixar de "impedir o descanso" (dormir).
 * Por padrao todo Monster impede o jogador de dormir se estiver por perto; aqui,
 * se o monstro for uma Jenny domada, respondemos que ela NAO impede.
 */
@Mixin(Monster.class)
public class MonsterMixin {

    @Inject(method = "isPreventingPlayerRest", at = @At("HEAD"), cancellable = true)
    private void jennyaddon$allowSleepNearTamedJenny(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (JennyCheck.isTamedJenny((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
