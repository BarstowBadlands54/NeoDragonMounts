package net.dragonmounts.neo.client;


import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.client.ClientDragonEntity;
import net.dragonmounts.neo.common.client.gui.DragonCoreScreen;
import net.dragonmounts.neo.common.client.gui.DragonInventoryScreen;
import net.dragonmounts.neo.common.client.renderer.DMCoreShaders;
import net.dragonmounts.neo.common.client.renderer.block.DragonCoreRenderer;
import net.dragonmounts.neo.common.client.renderer.block.DragonHeadRenderer;
import net.dragonmounts.neo.common.client.renderer.dragon.DragonRenderer;
import net.dragonmounts.neo.common.client.renderer.egg.DragonEggRenderer;
import net.dragonmounts.neo.common.init.*;
import net.dragonmounts.neo.common.item.DragonHeadItem;
import net.dragonmounts.neo.common.item.DragonScaleBowItem;
import net.dragonmounts.neo.common.network.c2s.ControlDragonPayload;
import net.dragonmounts.neo.common.util.ArrayUtil;
import net.dragonmounts.neo.compat.platform.ClientNetworkHandler;
import net.dragonmounts.neo.compat.platform.DMScreenHandlers;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.dragonmounts.neo.config.ClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.io.IOException;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

@Mod(value = DragonMountsShared.NAMESPACE, dist = Dist.CLIENT)
public class DragonMountsClient {
    public static final ResourceLocation MODEL_RELOADER = makeId("model_reloader");

    public DragonMountsClient(IEventBus modbus, ModContainer container) {
        ClientConfig.INSTANCE.register(container);
        modbus.addListener(DragonMountsClient::onClientSetup);
        modbus.addListener(DragonMountsClient::registerBuiltinPacks);
        modbus.addListener(DragonMountsClient::registerKeyMappings);
        modbus.addListener(DragonMountsClient::registerClientExtensions);
        modbus.addListener(DragonMountsClient::registerParticles);
        modbus.addListener(DragonMountsClient::registerModels);
        modbus.addListener(DragonMountsClient::registerRenderers);
        modbus.addListener(DragonMountsClient::registerShaders);
        modbus.addListener(DragonMountsClient::registerScreens);
        container.registerExtensionPoint(IConfigScreenFactory.class, DMConfigScreen::new);
    }

    public static void registerBuiltinPacks(AddPackFindersEvent event) {
        var source = Component.translatable("pack.source.builtinMod", "Dragon Mounts 2");
        event.addPackFinders(
                makeId("resourcepacks/classic_amulet"),
                PackType.CLIENT_RESOURCES,
                Component.translatable("resourcePack.neodragonmounts.classic_amulet.name"),
                PackSource.create(desc -> Component.translatable(
                        "pack.nameAndSource",
                        desc,
                        source
                ).withStyle(ChatFormatting.GRAY), true),
                false,
                Pack.Position.TOP
        );
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        var play = NeoForge.EVENT_BUS;
        play.addListener(DragonMountsClient::onClientTick);
        play.addListener(DragonMountsClient::modifyPlayerFov);
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        DMKeyMappings.register(event::register);
        DMKeyMappings.DESCEND.setKeyConflictContext(KeyConflictContext.IN_GAME);
        DMKeyMappings.BREATHE.setKeyConflictContext(KeyConflictContext.IN_GAME);
    }

    //    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
//        event.addListener(MODEL_RELOADER, (ResourceManagerReloadListener) (manager) -> {
//            var models = Minecraft.getInstance().getEntityModels();
//            for (var variant : DragonVariant.REGISTRY) {
//                variant.appearance.onReload(models);
//            }
//        });
//        event.addDependency(VanillaClientListeners.MODELS, MODEL_RELOADER);
//    }
    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (ItemLike like : DMItemGroups.DRAGON_HEADS.items) {   // .items is the ObjectArrayList<ItemLike>
            Item item = like.asItem();
            if (!(item instanceof DragonHeadItem)) continue;
            event.registerItem(new IClientItemExtensions() {
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return GeoRenderProvider.of(item).getGeoItemRenderer();
                }
            }, item);
        }
    }

    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpecial(DMParticles.DRAGON_BREATH, BreathParticleProvider.INSTANCE);
    }

    public static void registerModels(EntityRenderersEvent.RegisterLayerDefinitions event) {
//        for (var model : BuiltinFactory.values()) {
//            event.registerLayerDefinition(model.location, model::makeModel);
//        }
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DMBlockEntities.DRAGON_CORE.get(), DragonCoreRenderer::new);
        event.registerBlockEntityRenderer(DMBlockEntities.DRAGON_HEAD.get(), DragonHeadRenderer::new);
        event.registerEntityRenderer(DMEntities.HATCHABLE_DRAGON_EGG.get(), DragonEggRenderer::new);
        event.registerEntityRenderer(DMEntities.TAMEABLE_DRAGON.get(), DragonRenderer::new);
    }

    static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath("neodragonmounts", "rendertype_entity_cutout_decal"),
                            DefaultVertexFormat.NEW_ENTITY),
                    DMCoreShaders::setEntityCutoutDecal
            );
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath("neodragonmounts", "rendertype_entity_translucent_emissive_decal"),
                            DefaultVertexFormat.NEW_ENTITY),
                    DMCoreShaders::setEntityTranslucentEmissiveDecal
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Dragon Mounts dissolve shaders", e);
        }
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(DMScreenHandlers.DRAGON_CORE, DragonCoreScreen::new);
        event.register(DMScreenHandlers.DRAGON_INVENTORY, DragonInventoryScreen::new);
    }

//    public static void registerSpecialRendererCodecs(EntityRenderersEvent.RegisterRenderers event) {
//        event.register(makeId("dragon_core"), DragonCoreRenderer.Unbaked.CODEC);
//        event.register(makeId("dragon_head"), DragonHeadRenderer.Unbaked.CODEC);
//    }
//
//    public static void registerSpecialRenderers(RegisterSpecialBlockModelRendererEvent event) {
//        event.register(DMBlocks.DRAGON_CORE.get(), new DragonCoreRenderer.Unbaked(0.0F, Direction.SOUTH));
//        for (var variant : DragonVariants.BUILTIN_VALUES) {
//            var head = variant.head;
//            var renderer = new DragonHeadRenderer.Unbaked(variant, 0.0F);
//            event.register(head.standing.get(), renderer);
//            event.register(head.wall.get(), renderer);
//        }
//    }

    public static void onClientTick(ClientTickEvent.Pre event) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null) return;
        if (player.getVehicle() instanceof ClientDragonEntity dragon) {
            if (player != dragon.getControllingPassenger()) return;
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

    public static void modifyPlayerFov(ComputeFovModifierEvent event) {
        var player = event.getPlayer();
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof DragonScaleBowItem) {
            event.setNewFovModifier(event.getFovModifier() * (
                    1.0F - Mth.square(Math.min((float) player.getTicksUsingItem() / 20.0F, 1.0F)) * 0.15F
            ));
        }
    }
}
