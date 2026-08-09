package net.dragonmounts.neo.mixin;

import net.dragonmounts.neo.common.item.DragonScaleBowItem;
import net.dragonmounts.neo.common.item.DragonScaleShieldItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the mod's shields and bows their model predicates.
 * <p>
 * {@code ItemProperties.PROPERTIES} is keyed by the exact {@code Item} instance with no
 * inheritance, and vanilla only registers {@code blocking} against {@code Items.SHIELD} and
 * {@code pull}/{@code pulling} against {@code Items.BOW} and {@code Items.CROSSBOW}. A modded
 * {@code ShieldItem} therefore resolves {@code blocking} to nothing, its model override never
 * matches, and the base model renders whether or not the player is holding right-click.
 * <p>
 * The registration method is private in vanilla, so instead of forcing it open this hooks the
 * read side: {@code ItemOverrides.findOverride} calls {@code getProperty} once per property per
 * render, so answering there needs no startup wiring and has no ordering concerns.
 * <p>
 * Deliberately self-contained — it depends on nothing but the two item classes, so it is the
 * only file that has to exist for this to work.
 */
@SuppressWarnings("UnusedMixin")
@Mixin(ItemProperties.class)
public class ItemPropertiesMixin {
    private static final ResourceLocation DM$BLOCKING = ResourceLocation.withDefaultNamespace("blocking");
    private static final ResourceLocation DM$PULLING = ResourceLocation.withDefaultNamespace("pulling");
    private static final ResourceLocation DM$PULL = ResourceLocation.withDefaultNamespace("pull");

    /// 1 while the holder is actively using this exact stack. Mirrors vanilla's shield predicate.
    private static float dragonmounts$inUse(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F;
    }

    @Inject(method = "getProperty", at = @At("HEAD"), cancellable = true)
    private static void dragonmounts$supplyModdedProperties(
            ItemStack stack,
            ResourceLocation id,
            CallbackInfoReturnable<ItemPropertyFunction> cir
    ) {
        Item item = stack.getItem();
        if (item instanceof DragonScaleShieldItem) {
            if (DM$BLOCKING.equals(id)) {
                cir.setReturnValue((s, level, entity, seed) -> dragonmounts$inUse(s, entity));
            }
        } else if (item instanceof DragonScaleBowItem) {
            if (DM$PULLING.equals(id)) {
                cir.setReturnValue((s, level, entity, seed) -> dragonmounts$inUse(s, entity));
            } else if (DM$PULL.equals(id)) {
                cir.setReturnValue((s, level, entity, seed) -> {
                    if (entity == null || entity.getUseItem() != s) return 0.0F;
                    return (s.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
                });
            }
        }
    }
}
