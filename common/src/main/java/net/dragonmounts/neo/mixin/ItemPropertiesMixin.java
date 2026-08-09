package net.dragonmounts.neo.mixin;

import net.dragonmounts.neo.common.client.DMItemProperties;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the mod's shields and bows their model predicates.
 * <p>
 * {@code ItemProperties.PROPERTIES} is keyed by the exact {@code Item} instance with no
 * inheritance, and vanilla only ever registers {@code blocking} against {@code Items.SHIELD}
 * and {@code pull}/{@code pulling} against {@code Items.BOW} and {@code Items.CROSSBOW}. A
 * modded {@code ShieldItem} therefore resolves {@code blocking} to nothing, its override never
 * matches, and the base model renders whether or not the player is holding right-click.
 * <p>
 * The registration method is private in vanilla, so rather than an access transformer plus a
 * client-init call in each loader, this hooks the read side: {@code ItemOverrides.findOverride}
 * calls {@code getProperty} once per property per render, so answering there covers every
 * dragon shield and bow with no startup wiring and no ordering concerns.
 */
@SuppressWarnings("UnusedMixin")
@Mixin(ItemProperties.class)
public class ItemPropertiesMixin {
    @Inject(method = "getProperty", at = @At("HEAD"), cancellable = true)
    private static void dragonmounts$supplyModdedProperties(
            ItemStack stack,
            ResourceLocation id,
            CallbackInfoReturnable<ItemPropertyFunction> cir
    ) {
        var function = DMItemProperties.lookup(stack.getItem(), id);
        if (function != null) cir.setReturnValue(function);
    }
}
