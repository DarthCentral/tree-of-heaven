package com.example.worldroot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class WorldrootFragmentItem extends Item {
    public WorldrootFragmentItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) return InteractionResult.FAIL;
        Level level = context.getLevel();
        BlockPos place = context.getClickedPos().relative(context.getClickedFace());
        if (!level.isEmptyBlock(place) || level.isClientSide) return InteractionResult.SUCCESS;
        level.setBlock(place, WorldrootMod.HEART.get().defaultBlockState(), 3);
        if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
