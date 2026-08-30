package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DescribedArmorEffect;
import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.client.renderer.armor.DragonScaleArmorRenderer;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;

public class DragonScaleArmorItem extends ArmorItem implements DragonTypified, GeoItem {
    public final DragonType type;
    public final DescribedArmorEffect effect;
    public final TranslatableContents name;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /// Defaults to the "dragon_scale_" family prefix.
    public DragonScaleArmorItem(DragonType type, DescribedArmorEffect effect, Type slot, Properties props) {
        this(type, effect, "dragon_scale_", slot, props);
    }

    public DragonScaleArmorItem(DragonType type, DescribedArmorEffect effect, String keyPrefix, Type slot, Properties props) {
        super(Holder.direct(type.material),
                slot,
                props.durability(slot.getDurability(DURABILITY_FACTOR))   // ← durability + implicit stacksTo(1)
                        .component(DMDataComponents.DRAGON_TYPE, type));
        this.type = type;
        this.effect = effect;
        this.name = new TranslatableContents(ITEM_TRANSLATION_KEY_PREFIX + keyPrefix + slot.getName() + ".name", null, new Object[]{MutableComponent.create(type.name)});
    }
    private static final int DURABILITY_FACTOR = 50; // netherite, tune it for balancing

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(this.effect);
    }

    @Override
    public Component getName(ItemStack stack) {
        return MutableComponent.create(this.name);
    }

    @Override
    public DragonType getDragonType() {
        return this.type;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Static armour. A controller here is what would drive a swaying cape or a fluttering
        // wing -- the bones are already parented correctly, they only need keyframes.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /// Only ever invoked on a physical client: AnimatableInstanceCache memoises the provider
    /// behind an isPhysicalClient() guard, so the HumanoidModel reference below never loads on
    /// a dedicated server.
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T entity, ItemStack stack,
                    @Nullable EquipmentSlot slot, @Nullable HumanoidModel<T> original) {
                // Types with no geo texture return null; GeckoLib keeps the vanilla model and
                // they carry on rendering from their _layer_1/_layer_2 sheets. That covers the
                // six types without art, and the bone armour sets (skeleton, wither).
                if (DragonScaleArmorItem.this.type.armorTexture == null) return null;
                // Lazy, not a field initialiser -- eager construction here is a known source of
                // incompatibility with mods that touch item classes during registration.
                if (this.renderer == null) this.renderer = new DragonScaleArmorRenderer();
                return this.renderer;
            }
        });
    }
}
