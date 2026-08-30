package net.dragonmounts.neo.common.crafting;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.dragonmounts.neo.common.init.DMItems;
import net.dragonmounts.neo.common.init.DMRecipes;
import net.dragonmounts.neo.common.item.DragonScaleArmorItem;
import net.dragonmounts.neo.common.item.DragonScalesItem;
import net.dragonmounts.neo.common.tag.DMItemTags;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

/// Netherite armour + Dragon Scale Upgrade template + one dragon scale -> dragon scale armour of
/// that scale's type, keeping enchantments, name and (rescaled) damage.
///
/// The template stays untyped: the addition carries the type, so one template item covers every
/// dragon type and the End loot table needs a single entry.
///
/// One recipe covers every type x slot combination. The data-driven alternative would be
/// 4 slots x 18 types = 72 recipe files that all have to be regenerated whenever a type is added.
@MethodsReturnNonnullByDefault
public class DragonScaleUpgradeRecipe implements SmithingRecipe {
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;

    /// Built on first use, not at construction: recipes are deserialised during registry freeze on
    /// some loaders, and the item registry is not safe to walk until after that.
    private static @Nullable Map<DragonType, EnumMap<ArmorItem.Type, DragonScaleArmorItem>> lookup;

    public DragonScaleUpgradeRecipe(Ingredient template, Ingredient base, Ingredient addition) {
        this.template = template;
        this.base = base;
        this.addition = addition;
    }

    private static Map<DragonType, EnumMap<ArmorItem.Type, DragonScaleArmorItem>> lookup() {
        Map<DragonType, EnumMap<ArmorItem.Type, DragonScaleArmorItem>> map = lookup;
        if (map != null) return map;

        map = new Reference2ObjectOpenHashMap<>();
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof DragonScaleArmorItem armor) {
                map.computeIfAbsent(armor.type, ignored -> new EnumMap<>(ArmorItem.Type.class))
                        .putIfAbsent(armor.getType(), armor);
            }
        }
        // Each dragon type has exactly one armour family -- makeDragonScaleArmor for most,
        // makeDragonBoneArmor for skeleton and wither -- so there is no (type, slot) collision.
        // If a type ever gets both, putIfAbsent silently picks whichever registered first and
        // this needs to become an explicit family check.
        return lookup = map;
    }

    /// The scales carry the type. Reading the item's own field rather than the stack's
    /// DRAGON_TYPE component: the component is a default set at construction, so the two always
    /// agree, but the field cannot be stripped off a stack by a command or another mod.
    private static @Nullable DragonScaleArmorItem resultFor(ItemStack base, ItemStack addition) {
        if (!(base.getItem() instanceof ArmorItem armor)) return null;
        if (!(addition.getItem() instanceof DragonScalesItem scales)) return null;

        var slots = lookup().get(scales.type);
        return slots == null ? null : slots.get(armor.getType());
    }

    @Override
    public boolean matches(@NotNull SmithingRecipeInput input, @NotNull Level level) {
        return this.template.test(input.template())
                && this.base.test(input.base())
                && this.addition.test(input.addition())
                // Guards any type/slot pair with no armour item -- otherwise the table shows a
                // valid-looking preview that produces nothing on take.
                && resultFor(input.base(), input.addition()) != null;
    }

    @Override
    public ItemStack assemble(@NotNull SmithingRecipeInput input, @NotNull HolderLookup.Provider registries) {
        ItemStack base = input.base();
        DragonScaleArmorItem result = resultFor(base, input.addition());
        if (result == null) return ItemStack.EMPTY;

        // transmuteCopy carries the component patch across, so enchantments, custom name, dyes
        // and trim all survive the upgrade.
        ItemStack stack = base.transmuteCopy(result, 1);

        // Damage does NOT survive verbatim. Netherite and scale armour have different max
        // durability (a netherite chestplate is 592, a scale chestplate is 16 * 50 = 800), so
        // copying the raw damage value would hand the player a piece that is proportionally more
        // worn -- or already broken, if the raw value exceeds the new maximum. Carry the wear
        // fraction instead.
        int oldMax = base.getMaxDamage();
        int newMax = stack.getMaxDamage();
        if (base.isDamaged() && oldMax > 0 && newMax > 0) {
            int scaled = Math.round(base.getDamageValue() * (newMax / (float) oldMax));
            stack.setDamageValue(Math.min(scaled, newMax - 1));
        } else {
            stack.setDamageValue(0);
        }
        return stack;
    }

    @Override
    public ItemStack getResultItem(@NotNull HolderLookup.Provider registries) {
        // Recipe-book display only; the real result depends on the inputs.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return this.template.test(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return this.base.test(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return this.addition.test(stack);
    }

    @Override
    public RecipeSerializer<DragonScaleUpgradeRecipe> getSerializer() {
        return DMRecipes.DRAGON_SCALE_UPGRADE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean isIncomplete() {
        return Stream.of(this.template, this.base, this.addition).anyMatch(Ingredient::isEmpty);
    }

    private static DragonScaleUpgradeRecipe defaultInstance() {
        return new DragonScaleUpgradeRecipe(
                Ingredient.of(DMItems.DRAGON_SCALE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS),
                // 15 types, matching the 15 that have scale armour. Skeleton and wither bones are
                // NOT in this tag, so bone armour stays on its direct-craft recipes.
                Ingredient.of(DMItemTags.DRAGON_SCALES)
        );
    }

    /// Stateless, exactly like DragonArmorUpgradeRecipe.Serializer: the ingredients are fixed in
    /// code, so the JSON file carries nothing but its type.
    public static class Serializer implements RecipeSerializer<DragonScaleUpgradeRecipe> {
        public static final MapCodec<DragonScaleUpgradeRecipe> CODEC = new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.empty();
            }

            @Override
            public <T> RecordBuilder<T> encode(DragonScaleUpgradeRecipe input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return prefix;
            }

            @Override
            public <T> DataResult<DragonScaleUpgradeRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
                return DataResult.success(defaultInstance());
            }
        };

        public static final StreamCodec<RegistryFriendlyByteBuf, DragonScaleUpgradeRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public void encode(@NotNull RegistryFriendlyByteBuf buffer, @NotNull DragonScaleUpgradeRecipe ignored) {}

            @Override
            public DragonScaleUpgradeRecipe decode(@NotNull RegistryFriendlyByteBuf buffer) {
                return defaultInstance();
            }
        };

        @Override
        public MapCodec<DragonScaleUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DragonScaleUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
