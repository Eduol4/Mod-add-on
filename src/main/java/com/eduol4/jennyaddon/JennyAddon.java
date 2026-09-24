package com.eduol4.jennyaddon;

import net.minecraftforge.fml.common.Mod;

/**
 * Add-on companheiro para o mod "Jenny Dweller".
 * A logica de comportamento fica em PersonalityHandler; as metas de IA em
 * PassiveFollowGoal e SitGoal.
 */
@Mod(JennyAddon.MODID)
public class JennyAddon {
    public static final String MODID = "jennyaddon";

    // Dados guardados na propria Jenny (persistem sozinhos junto da entidade).
    public static final String TAG_PASSIVE = "JennyAddonPassive"; // personalidade passiva (80%)
    public static final String TAG_TAMED   = "JennyAddonTamed";   // domesticada
    public static final String TAG_OWNER   = "JennyAddonOwner";   // UUID do dono
    public static final String TAG_SITTING = "JennyAddonSitting"; // sentada (fica parada no lugar)

    public JennyAddon() {
    }
}
