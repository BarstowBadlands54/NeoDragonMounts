package net.dragonmounts.neo.config;

import com.google.common.collect.HashBiMap;
import net.minecraft.nbt.Tag;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.function.DoubleConsumer;

public class EntryUtil {
    public static String formatName(ModConfigSpec.ConfigValue<?> entry) {
        return String.join(".", entry.getPath());
    }

    public static String translate(String key) {
        return "options.neodragonmounts." + key;
    }

    public static net.dragonmounts.neo.config.BooleanEntry config(
            ModConfigSpec.Builder builder,
            String key,
            boolean fallback,
            String desc
    ) {
        return config(builder, key, fallback, translate(key), desc);
    }

    public static net.dragonmounts.neo.config.BooleanEntry config(
            ModConfigSpec.Builder builder,
            String key,
            boolean fallback,
            String name,
            String desc
    ) {
        return new BooleanEntry(builder.translation(name).comment(desc).define(key, fallback));
    }

    public static net.dragonmounts.neo.config.DoubleEntry config(
            ModConfigSpec.Builder builder,
            String key,
            double fallback,
            double min,
            double max,
            String desc
    ) {
        return config(builder, key, fallback, min, max, desc, null);
    }

    public static net.dragonmounts.neo.config.DoubleEntry config(
            ModConfigSpec.Builder builder,
            String key,
            double fallback,
            double min,
            double max,
            String desc,
            DoubleConsumer onChanged
    ) {
        return new DoubleEntry(builder.translation(translate(key)).comment(desc).defineInRange(key, fallback, min, max), min, max, onChanged);
    }

    public static void register(HashBiMap<net.dragonmounts.neo.config.ConfigEntry<?>, Integer> registry, net.dragonmounts.neo.config.ConfigEntry<?> entry) {
        registry.put(entry, registry.size());
    }

    public static <T> void override(net.dragonmounts.neo.config.ConfigEntry<T> entry, Tag data) {
        entry.override(entry.load(data));
    }

    protected static void setSaved(ModConfigEvent event) {
        switch (event.getConfig().getType()) {
            case SERVER -> ServerConfig.INSTANCE.getEntries().forEach(net.dragonmounts.neo.config.ConfigEntry::setSaved);
            case CLIENT -> ClientConfig.INSTANCE.getEntries().forEach(ConfigEntry::setSaved);
        }
    }

    public static void onLoad(ModConfigEvent.Loading event) {
        setSaved(event);
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        setSaved(event);
    }
}
