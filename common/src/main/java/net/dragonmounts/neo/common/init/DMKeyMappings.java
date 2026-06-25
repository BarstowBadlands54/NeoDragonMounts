package net.dragonmounts.neo.common.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.dragonmounts.neo.common.client.ClientDragonEntity;
import net.dragonmounts.neo.config.ClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;

import java.util.function.Consumer;

public class DMKeyMappings {
    public static final String KEY_CATEGORY = "key.categories.neodragonmounts";
    public static final ToggleKeyMapping DESCEND = new ToggleKeyMapping(
            "key.neodragonmounts.descend",
            InputConstants.KEY_Z,
            KEY_CATEGORY,
            ClientConfig.INSTANCE.toggleDescending::get
    );
    public static final ToggleKeyMapping BREATHE = new ToggleKeyMapping(
            "key.neodragonmounts.breathe",
            InputConstants.KEY_R,
            KEY_CATEGORY,
            ClientConfig.INSTANCE.toggleBreathing::get
    );

    public static final KeyMapping PROJECTILE = new KeyMapping(
            "key.neodragonmounts.projectile",
            InputConstants.KEY_G,
            KEY_CATEGORY
    );

    public static void register(Consumer<KeyMapping> registry) {
        registry.accept(DESCEND);
        registry.accept(BREATHE);
        registry.accept(PROJECTILE);
    }

}
