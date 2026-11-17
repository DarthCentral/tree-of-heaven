package com.example.worldroot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import java.util.List;

/** Growth engine: a pulse every 40 ticks, accelerated near water. */
public class WorldrootHeartBlock extends Block {
    public WorldrootHeartBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 40);
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // One vertical segment per 40 ticks = 30 blocks/minute.
        growTrunk(level, pos);
        // A 10-block/minute lateral spread is approximated by one branch pulse every 120 ticks.
        if (level.getGameTime() % 120 == 0) growCrown(level, pos, random);
        // Send roots downward; stop naturally at the world's bottom/bedrock.
        growRoot(level, pos);
        // Nearby water shortens the next pulse; otherwise retain the advertised base rate.
        int delay = hasWater(level, pos, 8) ? 20 : 40;
        level.scheduleTick(pos, this, delay);
    }

    private void growTrunk(ServerLevel level, BlockPos heart) {
        BlockPos top = heart.above();
        for (int i = 0; i < 128 && !level.isEmptyBlock(top); i++) top = top.above();
        if (level.isEmptyBlock(top) && level.getHeight() > top.getY()) level.setBlock(top, WorldrootMod.WOOD.get().defaultBlockState(), 3);
    }

    private void growCrown(ServerLevel level, BlockPos heart, RandomSource random) {
        int height = Math.max(4, (int)Math.min(256, level.getGameTime() / 40));
        BlockPos center = heart.above(height);
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos branch = center.relative(dir, 1 + random.nextInt(3));
        if (level.isEmptyBlock(branch)) level.setBlock(branch, WorldrootMod.WOOD.get().defaultBlockState(), 3);
        BlockPos leafward = branch.relative(dir);
        if (level.isEmptyBlock(leafward)) level.setBlock(leafward, WorldrootMod.WOOD.get().defaultBlockState(), 3);
    }

    private void growRoot(ServerLevel level, BlockPos heart) {
        BlockPos cursor = heart.below();
        for (int i = 0; i < 256 && cursor.getY() > level.getMinBuildHeight(); i++, cursor = cursor.below()) {
            if (level.isEmptyBlock(cursor)) { level.setBlock(cursor, WorldrootMod.ROOT.get().defaultBlockState(), 3); return; }
            if (level.getBlockState(cursor).is(WorldrootMod.ROOT.get())) continue;
            return;
        }
    }

    private boolean hasWater(Level level, BlockPos pos, int radius) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x=-radius; x<=radius; x++) for (int y=-radius; y<=radius; y++) for (int z=-radius; z<=radius; z++) {
            p.set(pos.getX()+x, pos.getY()+y, pos.getZ()+z);
            if (level.getFluidState(p).isSource()) return true;
        }
        return false;
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        return List.of(new ItemStack(WorldrootMod.FRAGMENT.get()));
    }
}
