package net.dragonmounts.neo.common.crafting;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.*;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.dragonmounts.neo.common.init.DMItems;
import net.dragonmounts.neo.common.init.DMRecipes;
import net.dragonmounts.neo.common.init.DragonArmorMaterials;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@MethodsReturnNonnullByDefault
public class DragonArmorUpgradeRecipe implements SmithingRecipe {
    public static List<ItemAttributeModifiers.Entry> merge(List<ItemAttributeModifiers.Entry> base, ItemLike item) {
        var component = item.asItem().components().get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (component == null) return base;
        var props = new Object2DoubleOpenHashMap<Holder<Attribute>>();
        for (var entry : component.modifiers()) {
            if (entry.slot() != EquipmentSlotGroup.BODY) continue;
            var modifier = entry.modifier();
            if (modifier.is(DMItems.DRAGON_ARMOR_MODIFIER_NAME) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                props.put(entry.attribute(), modifier.amount());
            }
        }
        var builder = ImmutableList.<ItemAttributeModifiers.Entry>builderWithExpectedSize(base.size() + props.size());
        for (var entry : base) {
            if (entry.slot() == EquipmentSlotGroup.BODY && props.containsKey(entry.attribute())) {
                var modifier = entry.modifier();
                if (modifier.is(DMItems.DRAGON_ARMOR_MODIFIER_NAME)) {
                    if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                        var attribute = entry.attribute();
                        double amount = props.getDouble(attribute);
                        props.removeDouble(attribute);
                        if (modifier.amount() < amount) {
                            builder.add(new ItemAttributeModifiers.Entry(
                                    attribute,
                                    new AttributeModifier(DMItems.DRAGON_ARMOR_MODIFIER_NAME, amount, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.BODY
                            ));
                            continue;
                        }
                    } else {
                        props.removeDouble(entry.attribute());
                    }
                }
            }
            builder.add(entry);
        }
        for (var entry : props.object2DoubleEntrySet()) {
            builder.add(new ItemAttributeModifiers.Entry(
                    entry.getKey(),
                    new AttributeModifier(DMItems.DRAGON_ARMOR_MODIFIER_NAME, entry.getDoubleValue(), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.BODY
            ));
        }
        return builder.build();
    }

    private final Ingredient template = Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    private final Ingredient base = Ingredient.of(DMItems.DIAMOND_DRAGON_ARMOR);
    private final Ingredient addition;

    public DragonArmorUpgradeRecipe(Ingredient addition) {
        this.addition = addition;
    }

    @Override
    public boolean matches(@NotNull SmithingRecipeInput input, @NotNull Level level) {
        return this.template.test(input.template()) && this.base.test(input.base()) && this.addition.test(input.addition());
    }

    @Override
    public ItemStack assemble(@NotNull SmithingRecipeInput input, @NotNull HolderLookup.Provider registries) {
        var stack = input.base();
        var component = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        boolean found = false;
        var builder = ImmutableList.<ItemAttributeModifiers.Entry>builder();
        for (var entry : component.modifiers()) {
            if (entry.slot() == EquipmentSlotGroup.BODY && entry.attribute().equals(Attributes.ARMOR)) {
                var modifier = entry.modifier();
                if (modifier.is(DMItems.DRAGON_ARMOR_MODIFIER_NAME) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    if (modifier.amount() < DragonArmorMaterials.NETHERITE.defense().getOrDefault(ArmorItem.Type.BODY, 0)) {
                        builder.add(new ItemAttributeModifiers.Entry(
                                Attributes.ARMOR,
                                new AttributeModifier(DMItems.DRAGON_ARMOR_MODIFIER_NAME, modifier.amount() + 1.0, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.BODY
                        ));
                        found = true;
                        continue;
                    } else {
                        var result = stack.transmuteCopy(DMItems.NETHERITE_DRAGON_ARMOR, 1);
                        result.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(merge(
                                component.modifiers(),
                                DMItems.NETHERITE_DRAGON_ARMOR
                        ), component.showInTooltip()));
                        return result;
                    }
                }
            }
            builder.add(entry);
        }
        var result = stack.copy();
        result.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(found ? builder.build() : merge(
                component.modifiers(),
                DMItems.DIAMOND_DRAGON_ARMOR
        ), component.showInTooltip()));
        return result;
    }

    @Override
    public ItemStack getResultItem(@NotNull HolderLookup.Provider registries) {
        return new ItemStack(DMItems.NETHERITE_DRAGON_ARMOR);
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
    public RecipeSerializer<DragonArmorUpgradeRecipe> getSerializer() {
        return DMRecipes.DRAGON_ARMOR_UPGRADE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean isIncomplete() {
        return Stream.of(this.template, this.base, this.addition).anyMatch(Ingredient::isEmpty);
    }

    public static class Serializer implements RecipeSerializer<DragonArmorUpgradeRecipe> {
        public static final MapCodec<DragonArmorUpgradeRecipe> CODEC = new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.empty();
            }

            @Override
            public <T> RecordBuilder<T> encode(DragonArmorUpgradeRecipe input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return prefix;
            }

            @Override
            public <T> DataResult<DragonArmorUpgradeRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
                return DataResult.success(new DragonArmorUpgradeRecipe(Ingredient.of(Items.NETHERITE_INGOT)));
            }
        };
        public static final StreamCodec<RegistryFriendlyByteBuf, DragonArmorUpgradeRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public void encode(@NotNull RegistryFriendlyByteBuf buffer, @NotNull DragonArmorUpgradeRecipe ignored) {}

            @Override
            public DragonArmorUpgradeRecipe decode(@NotNull RegistryFriendlyByteBuf buffer) {
                return new DragonArmorUpgradeRecipe(Ingredient.of(Items.NETHERITE_INGOT));
            }
        };

        @Override
        public MapCodec<DragonArmorUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DragonArmorUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}