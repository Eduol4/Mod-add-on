package com.eduol4.jennyaddon;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Mob;

import java.lang.reflect.Field;

/**
 * Ponte para acionar a animacao interna de "agachar" da Jenny.
 * A flag CROUCHING_ACCESSOR e public static no mod; usamos reflexao para aciona-la
 * sem precisar compilar o add-on contra o jar da Jenny.
 */
public final class JennyCompat {

    private static boolean resolved = false;
    private static Object crouchingAccessor; // EntityDataAccessor<Boolean>

    private JennyCompat() {}

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> cls = Class.forName("de.cadentem.jenny_dweller.entities.JennyDwellerEntity");
            Field f = cls.getField("CROUCHING_ACCESSOR");
            crouchingAccessor = f.get(null);
        } catch (Throwable t) {
            crouchingAccessor = null; // se falhar, a mecanica de sentar ainda funciona, so sem a pose
        }
    }

    @SuppressWarnings("unchecked")
    public static void setCrouching(Mob mob, boolean value) {
        resolve();
        if (crouchingAccessor == null) {
            return;
        }
        try {
            mob.getEntityData().set((EntityDataAccessor<Boolean>) crouchingAccessor, value);
            mob.refreshDimensions();
        } catch (Throwable ignored) {
        }
    }
}
