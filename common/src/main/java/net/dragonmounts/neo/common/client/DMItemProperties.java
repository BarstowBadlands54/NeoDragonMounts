package net.dragonmounts.neo.common.client;

import net.dragonmounts.neo.common.item.DragonScaleBowItem;
import net.dragonmounts.neo.common.item.DragonScaleShieldItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Item model predicates for the mod's shields and bows.
 * <p>
 * Vanilla registers {@code blocking} against {@code Items.SHIELD} alone, and {@code pull} /
 * {@code pulling} against {@code Items.BOW} and {@code Items.CROSSBOW} alone — the backing map in
 * {@code ItemProperties} is keyed per-item, not per-class. A modded {@code ShieldItem} therefore
 * has no {@code blocking} property at all, so the override in its model resolves to 0 forever and
 * the blocking model is never selected. Same story for the bow pulling frames.
 * <p>
 * {@code ItemProperties.register} is private in vanilla, so rather than force an access
 * transformer into the common module this class only describes <em>what</em> to register and
 * lets each loader supply the registrar it already has access to.
 *
 * <pre>
 * NeoForge, in FMLClientSetupEvent:   event.enqueueWork(() -&gt; DMItemProperties.registerAll(ItemProperties::register));
 * Fabric, in ClientModInitializer:    DMItemProperties.registerAll(ModelPredicateProviderRegistry::register);
 * </pre>
 */
public final class DMItemProperties {
    public static final ResourceLocation BLOCKING = ResourceLocation.withDefaultNamespace("blocking");
    public static final ResourceLocation PULLING = ResourceLocation.withDefaultNamespace("pulling");
    public static final ResourceLocation PULL = ResourceLocation.withDefaultNamespace("pull");

    /// 1 while the holder is actively using this exact stack. Mirrors vanilla's shield predicate.
    public static final ClampedItemPropertyFunction BLOCKING_FN =
            (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F;

    public static final ClampedItemPropertyFunction PULLING_FN =
            (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F;

    /// How far the draw has progressed, 0..1. The 20.0 divisor is vanilla's bow draw time.
    public static final ClampedItemPropertyFunction PULL_FN =
            (stack, level, entity, seed) -> {
                if (entity == null || entity.getUseItem() != stack) return 0.0F;
                return (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
            };

    /**
     * The property this item should supply for {@code id}, or null if it supplies none.
     * <p>
     * Read straight off the item type rather than a registration map, so it works without
     * anything having to be registered at startup — see {@code ItemPropertiesMixin}.
     */
    public static @Nullable ClampedItemPropertyFunction lookup(Item item, ResourceLocation id) {
        if (item instanceof DragonScaleShieldItem) {
            if (BLOCKING.equals(id)) return BLOCKING_FN;
        } else if (item instanceof DragonScaleBowItem) {
            if (PULLING.equals(id)) return PULLING_FN;
            if (PULL.equals(id)) return PULL_FN;
        }
        return null;
    }

    /// Receives one (item, property, function) triple at a time.
    @FunctionalInterface
    public interface Registrar {
        void register(Item item, ResourceLocation id, ClampedItemPropertyFunction function);
    }

    /**
     * Walks the item registry rather than a hand-written list, so any shield or bow added later
     * is covered without touching this class.
     */
    public static void registerAll(Registrar registrar) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof DragonScaleShieldItem) {
                registrar.register(item, BLOCKING, BLOCKING_FN);
            } else if (item instanceof DragonScaleBowItem) {
                registrar.register(item, PULLING, PULLING_FN);
                registrar.register(item, PULL, PULL_FN);
            }
        }
    }

    private DMItemProperties() {}
}
