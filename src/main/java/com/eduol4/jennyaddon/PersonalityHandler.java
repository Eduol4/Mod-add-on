package com.eduol4.jennyaddon;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cerebro do add-on: personalidade por spawn, IA das passivas, virada para hostil ao apanhar,
 * domesticacao (versao passiva), imunidade do dono e Golems ignorando a domada.
 */
@Mod.EventBusSubscriber(modid = JennyAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PersonalityHandler {

    private static final ResourceLocation JENNY_ID = new ResourceLocation("jenny_dweller", "jenny_dweller");

    private static final String TAG_ASSIGNED = "JennyAddonAssigned";

    private static final int PROVOKE_TICKS = 500; // ~25 s

    private static final double SPEED_ATTACK = 0.85D; // normal
    private static final double SPEED_WANDER = 0.43D; // 0.5 x normal

    // Itens de domesticacao -> taxa de sucesso.
    private static final Map<Item, Double> TAME_ITEMS = new HashMap<>();
    static {
        TAME_ITEMS.put(Items.CAKE, 1.000D);         // Bolo
        TAME_ITEMS.put(Items.PUMPKIN_PIE, 0.750D);  // Torta de abobora
        TAME_ITEMS.put(Items.COOKIE, 0.300D);       // Biscoito
        TAME_ITEMS.put(Items.GLOW_BERRIES, 0.250D); // Bagas douradas (glow berries)
        TAME_ITEMS.put(Items.SWEET_BERRIES, 0.125D);// Bagas doces (sweet berries)
        TAME_ITEMS.put(Items.HONEYCOMB, 0.200D);    // Favo de mel
        TAME_ITEMS.put(Items.HONEY_BOTTLE, 0.200D); // Garrafa de mel
    }

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
            boolean passive = mob.getRandom().nextInt(5) < 4; // 80% passiva
            data.putBoolean(JennyAddon.TAG_PASSIVE, passive);
            data.putBoolean(TAG_ASSIGNED, true);
        }

        if (data.getBoolean(JennyAddon.TAG_PASSIVE)) {
            configurePassive(mob);
        }
        if (data.getBoolean(JennyAddon.TAG_TAMED)) {
            mob.setPersistenceRequired();
        }
    }

    private static void configurePassive(PathfinderMob mob) {
        removeGoalsByName(mob.goalSelector,
                "JennyDwellerStareGoal", "JennyDwellerFleeGoal",
                "JennyDwellerChaseGoal", "JennyDwellerStrollGoal");
        removeGoalsByName(mob.targetSelector,
                "JennyDwellerTargetSeesMeGoal", "JennyDwellerTargetTooCloseGoal");

        if (!hasGoalByName(mob.goalSelector, "SitGoal")) {
            mob.goalSelector.addGoal(0, new SitGoal(mob));
        }
        if (!hasGoalByName(mob.goalSelector, "MeleeAttackGoal")) {
            mob.goalSelector.addGoal(1, new MeleeAttackGoal(mob, SPEED_ATTACK, true));
        }
        if (!hasGoalByName(mob.goalSelector, "PassiveFollowGoal")) {
            mob.goalSelector.addGoal(2, new PassiveFollowGoal(mob));
        }
        if (!hasGoalByName(mob.goalSelector, "WaterAvoidingRandomStrollGoal")) {
            mob.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(mob, SPEED_WANDER));
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Entity target = event.getTarget();
        if (!isJenny(target) || !(target instanceof PathfinderMob mob)) {
            return;
        }
        CompoundTag data = target.getPersistentData();
        if (!data.getBoolean(JennyAddon.TAG_PASSIVE)) {
            return;
        }
        Player player = event.getEntity();

        // Ja domesticada: o dono alterna entre seguir e sentar.
        if (data.getBoolean(JennyAddon.TAG_TAMED)) {
            if (isOwner(data, player)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                if (!target.level().isClientSide()) {
                    boolean nowSitting = !data.getBoolean(JennyAddon.TAG_SITTING);
                    data.putBoolean(JennyAddon.TAG_SITTING, nowSitting);
                    mob.getNavigation().stop();
                    // Aviso na action bar (acima da hotbar), como o "voce nao pode dormir".
                    player.displayClientMessage(
                        Component.literal(nowSitting ? "Jenny is now sitting!" : "Jenny is now following!"),
                        true
                    );
                }
            }
            return;
        }

        // Ainda selvagem: tentar domesticar com o item na mao.
        ItemStack held = event.getItemStack();
        Double chance = TAME_ITEMS.get(held.getItem());
        if (chance == null) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (target.level().isClientSide()) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        if (mob.getRandom().nextDouble() < chance) {
            data.putBoolean(JennyAddon.TAG_TAMED, true);
            data.putUUID(JennyAddon.TAG_OWNER, player.getUUID());
            data.putBoolean(JennyAddon.TAG_SITTING, false);
            mob.setPersistenceRequired();
            mob.setTarget(null);
            provoked.remove(mob.getId());
            spawnParticles(mob, ParticleTypes.HEART);
        } else {
            spawnParticles(mob, ParticleTypes.SMOKE);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Entity victim = event.getEntity();
        if (victim.level().isClientSide() || !isJenny(victim)) {
            return;
        }
        CompoundTag data = victim.getPersistentData();
        if (!data.getBoolean(JennyAddon.TAG_PASSIVE)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Player attackerPlayer) {
            // A domada nunca fica hostil com o proprio dono.
            if (data.getBoolean(JennyAddon.TAG_TAMED) && isOwner(data, attackerPlayer)) {
                return;
            }
            provoked.put(victim.getId(), PROVOKE_TICKS);
        }
    }

    /**
     * Bloqueia miras indesejadas: a domada nunca mira o dono, e Golems de ferro nunca miram a domada.
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget == null) {
            return;
        }

        if (isJenny(mob)) {
            CompoundTag data = mob.getPersistentData();
            if (data.getBoolean(JennyAddon.TAG_TAMED)
                    && newTarget instanceof Player targetPlayer
                    && isOwner(data, targetPlayer)) {
                event.setCanceled(true); // dono e intocavel
                return;
            }
        }

        if (mob instanceof IronGolem
                && isJenny(newTarget)
                && newTarget.getPersistentData().getBoolean(JennyAddon.TAG_TAMED)) {
            event.setCanceled(true); // Golem nao ataca Jenny domada
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        Entity dead = event.getEntity();
        if (dead.level().isClientSide() || !isJenny(dead)) {
            return;
        }
        CompoundTag data = dead.getPersistentData();
        if (!data.getBoolean(JennyAddon.TAG_TAMED) || !data.hasUUID(JennyAddon.TAG_OWNER)) {
            return;
        }
        Player owner = dead.level().getPlayerByUUID(data.getUUID(JennyAddon.TAG_OWNER));
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("Jenny has died!"));
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        Entity ent = event.getEntity();
        if (ent.level().isClientSide() || !(ent instanceof PathfinderMob mob) || !isJenny(ent)) {
            return;
        }
        if (!ent.getPersistentData().getBoolean(JennyAddon.TAG_PASSIVE)) {
            return;
        }
        Integer left = provoked.get(ent.getId());
        if (left != null) {
            left = left - 1;
            if (left <= 0) {
                provoked.remove(ent.getId());
                mob.setTarget(null);
            } else {
                provoked.put(ent.getId(), left);
            }
        }
    }

    // ---- utilitarios ----

    private static boolean isOwner(CompoundTag data, Player player) {
        return data.hasUUID(JennyAddon.TAG_OWNER)
            && data.getUUID(JennyAddon.TAG_OWNER).equals(player.getUUID());
    }

    private static void spawnParticles(Entity e, ParticleOptions particle) {
        if (!(e.level() instanceof ServerLevel sl)) {
            return;
        }
        sl.sendParticles(particle,
                e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(),
                7,
                e.getBbWidth() * 0.5D, e.getBbHeight() * 0.5D, e.getBbWidth() * 0.5D,
                0.05D);
    }

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
