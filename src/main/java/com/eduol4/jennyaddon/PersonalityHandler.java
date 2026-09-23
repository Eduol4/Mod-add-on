package com.eduol4.jennyaddon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cerebro do add-on. Reconhece a Jenny pelo identificador, sorteia a personalidade
 * (persistida na propria entidade), reconfigura as metas de IA das passivas e cuida
 * da virada para hostil (ao apanhar do jogador) e da volta a calma.
 */
@Mod.EventBusSubscriber(modid = JennyAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PersonalityHandler {

    private static final ResourceLocation JENNY_ID = new ResourceLocation("jenny_dweller", "jenny_dweller");

    // Guardados no espaco de dados persistente da entidade (salva/carrega sozinho).
    private static final String TAG_ASSIGNED = "JennyAddonAssigned";
    private static final String TAG_PASSIVE  = "JennyAddonPassive";

    // ~25 segundos (20 ticks por segundo) sem apanhar -> volta a ser passiva.
    private static final int PROVOKE_TICKS = 500;

    // id da entidade -> ticks restantes de "provocada". Estado de tempo de execucao (nao precisa persistir).
    private static final Map<Integer, Integer> provoked = new HashMap<>();

    private PersonalityHandler() {}

    private static boolean isJenny(Entity ent) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(ent.getType());
        return JENNY_ID.equals(key);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity ent = event.getEntity();
        if (!isJenny(ent) || !(ent instanceof PathfinderMob mob)) {
            return;
        }

        CompoundTag data = ent.getPersistentData();
        if (!data.contains(TAG_ASSIGNED)) {
            // Sorteio unico por criatura: 80% passiva, 20% normal.
            boolean passive = mob.getRandom().nextInt(5) < 4;
            data.putBoolean(TAG_PASSIVE, passive);
            data.putBoolean(TAG_ASSIGNED, true);
        }

        if (data.getBoolean(TAG_PASSIVE)) {
            configurePassive(mob);
        }
        // Personalidade "normal": nao mexemos em nada; a IA padrao do mod roda intacta.
    }

    private static void configurePassive(PathfinderMob mob) {
        // Sem stare, sem fuga, e sem a perseguicao-com-ataque padrao dela.
        removeGoalsByName(mob.goalSelector,
                "JennyDwellerStareGoal", "JennyDwellerFleeGoal", "JennyDwellerChaseGoal");
        // Nao e proativamente hostil: remove as metas que a fazem mirar o jogador sozinha.
        removeGoalsByName(mob.targetSelector,
                "JennyDwellerTargetSeesMeGoal", "JennyDwellerTargetTooCloseGoal");

        // Seguir pacificamente (prioridade 2, acima do "vagar" que e 3).
        if (!hasGoalByName(mob.goalSelector, "PassiveFollowGoal")) {
            mob.goalSelector.addGoal(2, new PassiveFollowGoal(mob));
        }
        // Ataque so entra em acao quando ela tiver um alvo (ou seja, quando provocada).
        if (!hasGoalByName(mob.goalSelector, "MeleeAttackGoal")) {
            mob.goalSelector.addGoal(1, new MeleeAttackGoal(mob, 1.2D, true));
        }
        // Mantida: CustomHurtByTargetGoal -> ao apanhar, ela mira quem a atacou.
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Entity victim = event.getEntity();
        if (victim.level().isClientSide() || !isJenny(victim)) {
            return;
        }
        if (!victim.getPersistentData().getBoolean(TAG_PASSIVE)) {
            return;
        }
        if (event.getSource().getEntity() instanceof Player) {
            // Provoca (ou renova o cronometro). A meta "revidar" ja define o alvo automaticamente.
            provoked.put(victim.getId(), PROVOKE_TICKS);
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        Entity ent = event.getEntity();
        if (ent.level().isClientSide() || !(ent instanceof PathfinderMob mob) || !isJenny(ent)) {
            return;
        }
        if (!ent.getPersistentData().getBoolean(TAG_PASSIVE)) {
            return;
        }
        Integer left = provoked.get(ent.getId());
        if (left != null) {
            left = left - 1;
            if (left <= 0) {
                provoked.remove(ent.getId());
                mob.setTarget(null); // acalmou: para de atacar e volta a seguir
            } else {
                provoked.put(ent.getId(), left);
            }
        }
    }

    // ---- utilitarios ----

    private static void removeGoalsByName(GoalSelector selector, String... simpleNames) {
        List<Goal> toRemove = new ArrayList<>();
        for (WrappedGoal wrapped : selector.getAvailableGoals()) {
            String name = wrapped.getGoal().getClass().getSimpleName();
            for (String want : simpleNames) {
                if (name.equals(want)) {
                    toRemove.add(wrapped.getGoal());
                    break;
                }
            }
        }
        for (Goal g : toRemove) {
            selector.removeGoal(g);
        }
    }

    private static boolean hasGoalByName(GoalSelector selector, String simpleName) {
        for (WrappedGoal wrapped : selector.getAvailableGoals()) {
            if (wrapped.getGoal().getClass().getSimpleName().equals(simpleName)) {
                return true;
            }
        }
        return false;
    }
}
