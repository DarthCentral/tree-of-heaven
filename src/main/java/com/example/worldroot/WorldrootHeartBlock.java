package com.example.worldroot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

/** Growth engine: a pulse every 40 ticks, accelerated near water. */
public class WorldrootHeartBlock extends Block {
    public WorldrootHeartBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 40);
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Faster trunk extension: one segment every 2 ticks (up to 600 blocks/minute
        // in ideal conditions; actual growth is limited by build height and obstructions).
        growTrunk(level, pos);
        // Several lateral branches and leafy clusters are added on every growth pulse.
        growCrown(level, pos, random);
        growRoot(level, pos);
        int delay = hasWater(level, pos, 8) ? 1 : 2;
        level.scheduleTick(pos, this, delay);
    }

    private void growTrunk(ServerLevel level, BlockPos heart) {
        BlockPos top = heart.above();
        for (int i = 0; i < 512 && top.getY() < level.getMaxBuildHeight() - 1 && !level.isEmptyBlock(top); i++) top = top.above();
        if (level.isEmptyBlock(top) && top.getY() < level.getMaxBuildHeight()) {
            level.setBlock(top, WorldrootMod.WOOD.get().defaultBlockState(), 3);
        }
    }

    private void growCrown(ServerLevel level, BlockPos heart, RandomSource random) {
        // Find the highest connected trunk segment. Branches are added at several
        // heights, not just at the newest top block, so the crown becomes broad.
        BlockPos top = heart.above();
        int scanned = 0;
        while (scanned++ < 2048 && top.getY() < level.getMaxBuildHeight() - 1
                && level.getBlockState(top).is(WorldrootMod.WOOD.get())) {
            top = top.above();
        }
        int height = Math.max(1, top.getY() - heart.getY());
        int tiers = Math.min(8, Math.max(2, height / 12));
        int branchCount = 5 + random.nextInt(5);

        for (int b = 0; b < branchCount; b++) {
            // Choose a different level in the upper portion of the trunk.
            int tierOffset = random.nextInt(Math.max(1, Math.min(height, tiers * 5)));
            BlockPos origin = top.below(1 + tierOffset);
            if (!level.getBlockState(origin).is(WorldrootMod.WOOD.get())) continue;
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int length = 5 + random.nextInt(10) + Math.min(24, height / 10);
            BlockPos tip = origin;
            for (int step = 1; step <= length; step++) {
                BlockPos next = origin.relative(dir, step).above(Math.max(0, step / 5));
                if (!level.isEmptyBlock(next) && !level.getBlockState(next).is(Blocks.OAK_LEAVES)) break;
                level.setBlock(next, WorldrootMod.WOOD.get().defaultBlockState(), 3);
                tip = next;
                // Secondary forks make the branch network visibly subdivide.
                if (step >= 4 && step % 4 == 0) {
                    Direction fork = dir.getClockWise();
                    BlockPos forkPos = next;
                    for (int f = 1; f <= 3 + random.nextInt(4); f++) {
                        BlockPos forkNext = forkPos.relative(fork).above(f / 3);
                        if (!level.isEmptyBlock(forkNext)) break;
                        level.setBlock(forkNext, WorldrootMod.WOOD.get().defaultBlockState(), 3);
                        forkPos = forkNext;
                    }
                    placeLeafCluster(level, forkPos, random, 2 + random.nextInt(2));
                }
            }
            placeLeafCluster(level, tip, random, 3);
        }
    }

    private void placeLeafCluster(ServerLevel level, BlockPos center, RandomSource random, int radius) {
        // Dense, persistent leaf clusters so leaves do not decay when separated
        // from vanilla logs. A slightly irregular sphere looks less boxy.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distance = dx * dx + dy * dy + dz * dz;
                    if (distance > radius * radius + 2 || random.nextFloat() < 0.10f) continue;
                    BlockPos leaf = center.offset(dx, dy, dz);
                    if (level.isEmptyBlock(leaf)) {
                        level.setBlock(leaf, Blocks.OAK_LEAVES.defaultBlockState()
                                .setValue(LeavesBlock.PERSISTENT, true), 3);
                    }
                }
            }
        }
    }

    private void growRoot(ServerLevel level, BlockPos heart) {
        // Grow a central taproot and several radial woody roots. Roots can replace
        // common soft terrain (dirt, grass, sand, gravel, clay, mud), but stop at
        // bedrock and avoid fluids or valuable/constructed blocks.
        BlockPos below = heart.below();
        if (below.getY() > level.getMinBuildHeight() && canRootReplace(level, below)) {
            level.setBlock(below, WorldrootMod.ROOT.get().defaultBlockState(), 3);
        }
        int depth = 0;
        BlockPos cursor = below;
        while (depth < 512 && cursor.getY() > level.getMinBuildHeight()
                && level.getBlockState(cursor).is(WorldrootMod.ROOT.get())) {
            cursor = cursor.below();
            depth++;
        }
        if (depth > 0 && cursor.getY() > level.getMinBuildHeight() && canRootReplace(level, cursor)) {
            level.setBlock(cursor, WorldrootMod.ROOT.get().defaultBlockState(), 3);
        }

        // Side roots branch from progressively deeper points, spreading outward.
        Direction[] directions = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
        for (Direction direction : directions) {
            int levelDepth = 2 + level.random.nextInt(Math.max(2, Math.min(24, depth + 2)));
            BlockPos anchor = heart.below(levelDepth);
            if (!level.getBlockState(anchor).is(WorldrootMod.ROOT.get())) continue;
            BlockPos side = anchor.relative(direction);
            if (canRootReplace(level, side)) {
                level.setBlock(side, WorldrootMod.ROOT.get().defaultBlockState(), 3);
                BlockPos sideTip = side.relative(direction);
                if (canRootReplace(level, sideTip)) {
                    level.setBlock(sideTip, WorldrootMod.ROOT.get().defaultBlockState(), 3);
                }
            }
        }
    }

    private boolean canRootReplace(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight()) return false;
        BlockState state = level.getBlockState(pos);
        if (state.is(WorldrootMod.ROOT.get()) || state.isAir()) return true;
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY) || state.is(Blocks.MUD) || state.is(Blocks.MOSS_BLOCK);
    }

    private boolean hasWater(Level level, BlockPos pos, int radius) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x=-radius; x<=radius; x++) for (int y=-radius; y<=radius; y++) for (int z=-radius; z<=radius; z++) {
            p.set(pos.getX()+x, pos.getY()+y, pos.getZ()+z);
            if (level.getFluidState(p).isSource()) return true;
        }
        return false;
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(WorldrootMod.FRAGMENT.get()));
    }
}
