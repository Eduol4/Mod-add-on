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
 *
 * Miramos o metodo pelos dois nomes possiveis (legivel via refmap, ou o nome ofuscado
 * direto), e usamos require = 0: se por acaso nenhum nome casar em tempo de execucao,
 * o Mixin simplesmente NAO se aplica (o jogo NAO trava) — apenas o "dormir" fica sem efeito.
 */
@Mixin(Monster.class)
public class MonsterMixin {

    @Inject(
        method = {"isPreventingPlayerRest", "m_5843_"},
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void jennyaddon$allowSleepNearTamedJenny(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (JennyCheck.isTamedJenny((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
