package net.dragonmounts.neo.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;

/**
 * 1.21.1 STUB. The original (1.21.4) implementation used net.minecraft.client.data.models.* — the new
 * item-model-definition system (ItemModelGenerators.generateBow/generateShield/generateBooleanDispatch,
 * ItemModelUtils, ItemTintSource, Dye, UseDuration). None of that exists in 1.21.1, whose
 * net.minecraft.data.models.ItemModelGenerators only offers generateFlatItem(...) and friends.
 *
 * Port block/flat-item models here against the 1.21.1 generators; hand-author the complex ones
 * (flute/bow/shield/armor-trim/dragon head/dragon core) as static JSON under
 * resources/assets/neodragonmounts/models/. The full 1.21.4 source is preserved alongside as
 * DMModelProvider.java.ORIGINAL-1.21.4-reference.txt.
 *
 * Verify the FabricModelProvider import path against your Fabric API version (1.21.4 used the extra
 * ".client": net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider).
 */
public class DMModelProvider extends FabricModelProvider {
    public DMModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {
        // TODO 1.21.1: port dragon block-state/models from the reference file.
    }

    @Override
    public void generateItemModels(ItemModelGenerators generators) {
        // TODO 1.21.1: port dragon item models; hand-author flute/bow/shield/head/core as static JSON.
    }
}
