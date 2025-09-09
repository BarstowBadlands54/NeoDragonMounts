package net.dragonmounts.neo.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.dragonmounts.neo.client.DMConfigScreen;

public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DMConfigScreen::new;
    }
}
