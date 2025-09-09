package net.dragonmounts.neo.config;

import com.google.common.collect.HashBiMap;
import com.mojang.brigadier.builder.ArgumentBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.dragonmounts.neo.DragonMounts;
import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.entity.dragon.HatchableDragonEggEntity;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.compat.platform.ServerNetworkHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.Collection;

import static net.dragonmounts.neo.config.EntryUtil.config;
import static net.dragonmounts.neo.config.EntryUtil.register;

public class ServerConfig extends ConfigHolder<CommandSourceStack> {
    public static final net.dragonmounts.neo.config.ServerConfig INSTANCE = new net.dragonmounts.neo.config.ServerConfig(DragonMountsShared.NAMESPACE, "server.dat");
    protected final HashBiMap<net.dragonmounts.neo.config.ConfigEntry<?>, Integer> entries;
    public final net.dragonmounts.neo.config.BooleanEntry debug;
    public final net.dragonmounts.neo.config.BooleanEntry isEggPushable;
    public final net.dragonmounts.neo.config.BooleanEntry isEggOverridden;
    public final net.dragonmounts.neo.config.BooleanEntry ignitingBreath;
    public final net.dragonmounts.neo.config.BooleanEntry destructiveBreath;
    public final net.dragonmounts.neo.config.BooleanEntry smeltingBreath;
    public final net.dragonmounts.neo.config.BooleanEntry quenchingBreath;
    public final BooleanEntry frostyBreath;
    public final net.dragonmounts.neo.config.DoubleEntry baseArmor;
    public final net.dragonmounts.neo.config.DoubleEntry baseArmorToughness;
    public final net.dragonmounts.neo.config.DoubleEntry baseBodySize;
    public final net.dragonmounts.neo.config.DoubleEntry baseDamage;
    public final net.dragonmounts.neo.config.DoubleEntry baseFlyingSpeed;
    public final net.dragonmounts.neo.config.DoubleEntry baseFollowRange;
    public final net.dragonmounts.neo.config.DoubleEntry baseHealth;
    public final net.dragonmounts.neo.config.DoubleEntry baseJumpStrength;
    public final net.dragonmounts.neo.config.DoubleEntry baseKnockback;
    public final net.dragonmounts.neo.config.DoubleEntry baseKnockbackResistance;
    public final net.dragonmounts.neo.config.DoubleEntry baseMovementSpeed;
    public final net.dragonmounts.neo.config.DoubleEntry baseStepHeight;
    public final net.dragonmounts.neo.config.DoubleEntry baseTemptRange;
    public final DoubleEntry baseWaterMovementEfficiency;
    private AttributeSupplier dragonAttributes;
    private AttributeSupplier dragonEggAttributes;

    protected ServerConfig(String mod, String file) {
        super(mod, file);
        var registry = HashBiMap.<net.dragonmounts.neo.config.ConfigEntry<?>, Integer>create();
        register(registry, this.debug =
                config("debug", false)
        );
        register(registry, this.isEggPushable =
                config("isEggPushable", false)
        );
        register(registry, this.isEggOverridden =
                config("isEggOverridden", true)
        );
        register(registry, this.ignitingBreath =
                config("ignitingBreath", true)
        );
        register(registry, this.destructiveBreath =
                config("destructiveBreath", true)
        );
        register(registry, this.smeltingBreath =
                config("smeltingBreath", false)
        );
        register(registry, this.quenchingBreath =
                config("quenchingBreath", true)
        );
        register(registry, this.frostyBreath =
                config("frostyBreath", false)
        );
        register(registry, this.baseArmor =
                config("baseArmor", 8.0, 0.0, 30.0, this::invalidateAttributes)
        );
        register(registry, this.baseArmorToughness =
                config("baseArmorToughness", 20.0, 0.0, 20.0, this::invalidateAttributes)
        );
        register(registry, this.baseBodySize =
                config("baseBodySize", 1.0, 0.0625, 16.0, this::invalidateAttributes)
        );
        register(registry, this.baseDamage =
                config("baseDamage", 12.0, 0.0, 2048.0, this::invalidateAttributes)
        );
        register(registry, this.baseFlyingSpeed =
                config("baseFlyingSpeed", 0.25, 0.0, 1024.0, this::invalidateAttributes)
        );
        register(registry, this.baseFollowRange =
                config("baseFollowRange", 64.0, 0.0, 2048.0, this::invalidateAttributes)
        );
        register(registry, this.baseHealth =
                config("baseHealth", 90.0, 1.0, 1024.0, this::invalidateAttributes)
        );
        register(registry, this.baseJumpStrength =
                config("baseJumpStrength", 1.0, 0.0, 32.0, this::invalidateAttributes)
        );
        register(registry, this.baseKnockback =
                config("baseKnockback", 0.0, 0.0, 5.0, this::invalidateAttributes)
        );
        register(registry, this.baseKnockbackResistance =
                config("baseKnockbackResistance", 1.0, 0.0, 1.0, this::invalidateAttributes)
        );
        register(registry, this.baseMovementSpeed =
                config("baseMovementSpeed", 0.3, 0.0, 1024.0, this::invalidateAttributes)
        );
        register(registry, this.baseStepHeight =
                config("baseStepHeight", 1.25, 0.0, 10, this::invalidateAttributes)
        );
        register(registry, this.baseTemptRange =
                config("baseTemptRange", 16.0, 0.0, 2048.0, this::invalidateAttributes)
        );
        register(registry, this.baseWaterMovementEfficiency =
                config("baseWaterMovementEfficiency", 0.25, 0.0, 1.0, this::invalidateAttributes)
        );
        this.entries = registry;
        this.load();
    }

    public net.dragonmounts.neo.config.ConfigEntry<?> getEntry(int id) {
        return this.entries.inverse().get(id);
    }

    @Override
    public Collection<net.dragonmounts.neo.config.ConfigEntry<?>> getEntries() {
        return this.entries.keySet();
    }

    public void broadcast(net.dragonmounts.neo.config.ConfigEntry<?> entry) {
        var server = DragonMounts.getRunningServer();
        if (server == null) return;
        Integer id = this.entries.get(entry);
        if (id == null) return;
        ServerNetworkHandler.sendToAll(server, entry.wrap(id));
    }

    @Override
    protected <T> ArgumentBuilder<CommandSourceStack, ?> buildCommand(ConfigEntry<T> entry) {
        return Commands.literal(entry.key).executes(context -> {
            context.getSource().sendSuccess(() -> Component.translatable("commands.neodragonmounts.config.query", entry.getDisplayName(), entry.getAsString()), true);
            return 1;
        }).then(Commands.argument("value", entry.getArgument()).executes(context -> {
            if (entry.set(entry.parse(context, "value"))) {
                this.save();
                this.broadcast(entry);
            }
            context.getSource().sendSuccess(() -> Component.translatable("commands.neodragonmounts.config.modify", entry.getDisplayName(), entry.getAsString()), true);
            return 1;
        }));
    }

    public AttributeSupplier getDragonAttributes() {
        var attrs = this.dragonAttributes;
        if (attrs == null) {
            this.dragonAttributes = attrs = TameableDragonEntity.createAttributes().build();
        }
        return attrs;
    }

    public AttributeSupplier getDragonEggAttributes() {
        var attrs = this.dragonEggAttributes;
        if (attrs == null) {
            this.dragonEggAttributes = attrs = HatchableDragonEggEntity.createAttributes().build();
        }
        return attrs;
    }

    public void sync(ServerPlayer player) {
        var entries = new ObjectArrayList<S2CSyncConfigPayload.Entry>();
        for (var entry : this.entries.entrySet()) {
            entries.add(new S2CSyncConfigPayload.Entry(entry.getValue(), entry.getKey().dump()));
        }
        ServerNetworkHandler.sendTo(player, new S2CSyncConfigPayload(entries));
    }

    public void invalidateAttributes(double ignored) {
        this.dragonAttributes = null;
        this.dragonEggAttributes = null;
    }

    public static void init() {}
}
