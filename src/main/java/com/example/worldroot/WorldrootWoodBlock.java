package com.example.worldroot;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import java.util.List;

public class WorldrootWoodBlock extends Block {
    public WorldrootWoodBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(WorldrootMod.FRAGMENT.get()));
    }
}
