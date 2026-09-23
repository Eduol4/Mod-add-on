package com.eduol4.jennyaddon;

import net.minecraftforge.fml.common.Mod;

/**
 * Add-on companheiro para o mod "Jenny Dweller".
 * Toda a logica de comportamento fica em {@link PersonalityHandler},
 * registrada automaticamente pelo @Mod.EventBusSubscriber.
 */
@Mod(JennyAddon.MODID)
public class JennyAddon {
    public static final String MODID = "jennyaddon";

    public JennyAddon() {
        // Nada a fazer aqui: os handlers de evento se registram sozinhos.
    }
}
