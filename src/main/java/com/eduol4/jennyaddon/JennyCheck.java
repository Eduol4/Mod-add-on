package com.eduol4.jennyaddon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Utilitarios publicos para reconhecer a Jenny e seus estados, usados tanto pelos
 * handlers de evento quanto pelos Mixins.
 */
public final class JennyCheck {

    private static final ResourceLocation JENNY_ID = new ResourceLocation("jenny_dweller", "jenny_dweller");

    private JennyCheck() {}

    public static boolean isJenny(Entity e) {
        if (e == null) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return JENNY_ID.equals(key);
    }

    public static boolean isTamedJenny(Entity e) {
        return isJenny(e) && e.getPersistentData().getBoolean(JennyAddon.TAG_TAMED);
    }
}
