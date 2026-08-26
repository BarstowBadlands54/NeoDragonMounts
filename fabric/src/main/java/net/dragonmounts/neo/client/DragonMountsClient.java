package net.dragonmounts.neo.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.api.DescribedArmorEffect;
import net.dragonmounts.neo.common.client.ClientDragonEntity;
import net.dragonmounts.neo.common.client.DMItemColors;
import net.dragonmounts.neo.common.client.gui.DragonCoreScreen;
import net.dragonmounts.neo.common.client.gui.DragonInventoryScreen;
import net.dragonmounts.neo.common.client.renderer.DMCoreShaders;
import net.dragonmounts.neo.common.client.renderer.block.DragonCoreRenderer;
import net.dragonmounts.neo.common.client.renderer.block.DragonHeadRenderer;
import net.dragonmounts.neo.common.client.renderer.dragon.DragonRenderer;
import net.dragonmounts.neo.common.client.renderer.egg.DragonEggRenderer;
import net.dragonmounts.neo.common.client.renderer.projectile.DragonSnowballRenderer;
import net.dragonmounts.neo.common.client.renderer.projectile.DragonWaterConduitRenderer;
import net.dragonmounts.neo.common.init.*;
import net.dragonmounts.neo.common.network.c2s.ControlDragonPayload;
import net.dragonmounts.neo.common.util.ArrayUtil;
import net.dragonmounts.neo.compat.platform.ClientNetworkHandler;
import net.dragonmounts.neo.compat.platform.DMScreenHandlers;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.dragonmounts.neo.config.ClientConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
// 1.21.1: SpecialBlockRendererRegistry (Fabric 1.21.4 API) absent
// import net.fabricmc.fabric.api.client.rendering.v1.SpecialBlockRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
// 1.21.1: special-model system removed — core/head item render needs a BEWLR
// import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.renderer.entity.DragonFireballRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.CreativeModeTabs;
import org.jetbrains.annotations.Nullable;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

@Environment(EnvType.CLIENT)
public class DragonMountsClient implements
        ClientModInitializer,
        TooltipComponentCallback,
        ClientTickEvents.StartTick,
        SimpleSynchronousResourceReloadListener {
    public static final ResourceLocation MODEL_RELOADER = makeId("model_reloader");

    @Override
    public void onInitializeClient() {
        ClientConfig.init();
        ClientNetworkHandler.initClient();
        DMKeyMappings.register(KeyBindingHelper::registerKeyBinding);
        TooltipComponentCallback.EVENT.register(this);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries ->
                DMItemGroups.DRAGON_SPAWN_EGGS.accept(entries.getContext(), entries)
        );
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries ->
                DMItemGroups.DRAGON_EGGS.accept(entries.getContext(), entries)
        );
        MenuScreens.register(DMScreenHandlers.DRAGON_CORE, DragonCoreScreen::new);
        MenuScreens.register(DMScreenHandlers.DRAGON_INVENTORY, DragonInventoryScreen::new);

        ClientTickEvents.START_CLIENT_TICK.register(this);
        BlockEntityRenderers.register(DMBlockEntities.DRAGON_CORE.get(), DragonCoreRenderer::new);
        BlockEntityRenderers.register(DMBlockEntities.DRAGON_HEAD.get(), DragonHeadRenderer::new);
        EntityRendererRegistry.register(DMEntities.HATCHABLE_DRAGON_EGG.get(), DragonEggRenderer::new);
        EntityRendererRegistry.register(DMEntities.TAMEABLE_DRAGON.cast(), DragonRenderer::new);
        EntityRendererRegistry.register(DMEntities.DRAGON_END_CHARGE.get(), DragonFireballRenderer::new);
        EntityRendererRegistry.register(DMEntities.DRAGON_ICE_CHARGE.get(), DragonSnowballRenderer::new);
        EntityRendererRegistry.register(DMEntities.DRAGON_WATER_CHARGE.get(), DragonWaterConduitRenderer::new);
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(this);
        ParticleFactoryRegistry.getInstance().register(DMParticles.DRAGON_BREATH, BreathParticleProvider::new);
        ClientCommandRegistrationCallback.EVENT.register(DMClientCommand::register);
        ColorProviderRegistry.ITEM.register(DMItemColors::dyeableArmor, DMItems.LEATHER_DRAGON_ARMOR);
        FabricLoader.getInstance().getModContainer(DragonMountsShared.NAMESPACE).ifPresent(mod ->
                ResourceManagerHelper.registerBuiltinResourcePack(
                        makeId("classic_amulet"),
                        mod,
                        Component.translatable("resourcePack.neodragonmounts.classic_amulet.name"),
                        ResourcePackActivationType.NORMAL
                )
        );

        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(
                    makeId("rendertype_entity_cutout_decal"),
                    DefaultVertexFormat.NEW_ENTITY,
                    DMCoreShaders::setEntityCutoutDecal
            );
            context.register(
                    makeId("rendertype_entity_translucent_emissive_decal"),
                    DefaultVertexFormat.NEW_ENTITY,
                    DMCoreShaders::setEntityTranslucentEmissiveDecal
            );
        });
    }

    @Override
    public @Nullable ClientTooltipComponent getComponent(TooltipComponent data) {
        return data instanceof DescribedArmorEffect effect ? effect.getClientTooltip() : null;
    }

    @Override
    public void onStartTick(Minecraft client) {
        var player = client.player;
        if (player == null) return;
        if (player.getVehicle() instanceof ClientDragonEntity dragon) {
            if (player != dragon.getControllingPassenger()) return;

            dragon.clientTickProjectile(client);

            int flags = ArrayUtil.compressFlags(
                    DMKeyMappings.DESCEND.isDown(),
                    client.options.keySprint.isDown(),
                    DMKeyMappings.BREATHE.isDown()
            );
            if (flags == dragon.controlFlags) return;
            dragon.controlFlags = flags;
            ClientNetworkHandler.send(new ControlDragonPayload(dragon.getId(), flags));
        }
    }

    @Override
    public ResourceLocation getFabricId() {
        return MODEL_RELOADER;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        var models = Minecraft.getInstance().getEntityModels();
        for (var variant : DragonVariant.REGISTRY) {
            variant.appearance.onReload(models);
        }
    }
}
