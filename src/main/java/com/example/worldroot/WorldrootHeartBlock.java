package com.example.worldroot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

/** Repeating growth controller for a Worldroot. */
public class WorldrootHeartBlock extends Block {
    public WorldrootHeartBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 20);
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int height = findTrunkHeight(level, pos);
        growTrunk(level, pos);
        growCrown(level, pos, height, random);
        thickenTrunk(level, pos, height);
        growRoots(level, pos, height, random);
        // Keep the controller alive. Water makes the next pulse quicker.
        level.scheduleTick(pos, this, hasWater(level, pos, 10) ? 2 : 5);
    }

    private int findTrunkHeight(ServerLevel level, BlockPos heart) {
        int height = 0;
        BlockPos.MutableBlockPos cursor = heart.above().mutable();
        while (cursor.getY() < level.getMaxBuildHeight() - 1
                && level.getBlockState(cursor).is(WorldrootMod.WOOD.get())) {
            height++;
            cursor.move(Direction.UP);
        }
        return height;
    }

    private void growTrunk(ServerLevel level, BlockPos heart) {
        BlockPos top = heart.above();
        while (top.getY() < level.getMaxBuildHeight() - 1
                && level.getBlockState(top).is(WorldrootMod.WOOD.get())) top = top.above();
        if (top.getY() < level.getMaxBuildHeight() - 1 && (level.isEmptyBlock(top) || level.getBlockState(top).is(Blocks.OAK_LEAVES))) {
            level.setBlock(top, WorldrootMod.WOOD.get().defaultBlockState(), 3);
        }
    }

    /** Four guaranteed radial branch tiers; branch length increases as the trunk grows. */
    private void growCrown(ServerLevel level, BlockPos heart, int height, RandomSource random) {
        if (height < 3) return;
        int tierCount = Math.max(1, height / 6);
        int maxTiersThisPulse = Math.min(tierCount, 24);
        int firstTier = Math.max(0, tierCount - maxTiersThisPulse);
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int tier = firstTier; tier < tierCount; tier++) {
            int y = 3 + tier * 6;
            if (y > height) continue;
            BlockPos trunkAt = heart.above(y);
            if (!level.getBlockState(trunkAt).is(WorldrootMod.WOOD.get())) continue;
            int length = 5 + height / 5 + tier / 3;
            for (Direction dir : dirs) {
                BlockPos tip = trunkAt;
                for (int step = 1; step <= length; step++) {
                    BlockPos branch = trunkAt.relative(dir, step).above(step / 7);
                    if (!canPlaceWood(level, branch)) break;
                    level.setBlock(branch, WorldrootMod.WOOD.get().defaultBlockState(), 3);
                    tip = branch;
                    // Forks on both sides at regular intervals, not just one random fork.
                    if (step >= 4 && step % 5 == 0) {
                        growFork(level, branch, dir.getClockWise(), 3 + tier / 5, random);
                        growFork(level, branch, dir.getCounterClockWise(), 3 + tier / 5, random);
                    }
                }
                placeLeafCluster(level, tip, random, 3 + Math.min(3, height / 80));
            }
        }
        // Also leaf the upper crown, so new growth does not remain bare.
        BlockPos crown = heart.above(Math.max(1, height));
        if (level.getBlockState(crown).is(WorldrootMod.WOOD.get())) placeLeafCluster(level, crown, random, 4);
    }

    private void growFork(ServerLevel level, BlockPos start, Direction dir, int length, RandomSource random) {
        BlockPos tip = start;
        for (int i = 1; i <= Math.min(12, length); i++) {
            BlockPos next = start.relative(dir, i).above(i / 3);
            if (!canPlaceWood(level, next)) break;
            level.setBlock(next, WorldrootMod.WOOD.get().defaultBlockState(), 3);
            tip = next;
        }
        placeLeafCluster(level, tip, random, 2 + random.nextInt(2));
    }

    private boolean canPlaceWood(ServerLevel level, BlockPos pos) {
        if (pos.getY() >= level.getMaxBuildHeight() - 1) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.OAK_LEAVES) || state.is(WorldrootMod.WOOD.get());
    }

    private void thickenTrunk(ServerLevel level, BlockPos heart, int height) {
        // Add one new outer ring over time. Radius grows with tree height; capped
        // per-ring for performance, while higher rings continue to widen as height rises.
        int radius = 1 + height / 24;
        int ringY = 2 + ((height / 12) * 12);
        if (ringY > height) ringY = Math.max(2, height - 1);
        int r = Math.min(32, radius);
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dz * dz > r * r) continue;
            BlockPos p = heart.offset(dx, ringY, dz);
            if (level.isEmptyBlock(p) || level.getBlockState(p).is(Blocks.OAK_LEAVES))
                level.setBlock(p, WorldrootMod.WOOD.get().defaultBlockState(), 3);
        }
    }

    private void placeLeafCluster(ServerLevel level, BlockPos center, RandomSource random, int radius) {
        for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dy * dy + dz * dz > radius * radius + 2 || random.nextFloat() < 0.07f) continue;
            BlockPos leaf = center.offset(dx, dy, dz);
            if (level.isEmptyBlock(leaf)) level.setBlock(leaf,
                    Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 3);
        }
    }

    /**
     * The taproot advances toward the first bedrock layer. Side roots have no
     * fixed maximum length: each growth pulse adds the next block to every
     * active radial root at every established taproot depth. The game-time
     * based frontier means the root network keeps expanding even after the
     * trunk reaches the build-height ceiling.
     */
    private void growRoots(ServerLevel level, BlockPos heart, int height, RandomSource random) {
        int minY = level.getMinBuildHeight();
        BlockPos cursor = heart.below();
        int safety = 0;
        while (cursor.getY() > minY + 5 && level.getBlockState(cursor).is(WorldrootMod.ROOT.get()) && safety++ < 4096) {
            cursor = cursor.below();
        }
        // Find the upper surface of the actual bedrock floor (normally several
        // layers thick), and stop immediately above it rather than replacing it.
        int bedrockTop = minY;
        for (int y = minY; y <= Math.min(minY + 5, level.getMaxBuildHeight() - 1); y++) {
            if (level.getBlockState(new BlockPos(heart.getX(), y, heart.getZ())).is(Blocks.BEDROCK)) {
                bedrockTop = y;
            }
        }
        if (cursor.getY() > bedrockTop + 1 && cursor.getY() > minY
                && level.hasChunkAt(cursor) && canRootReplace(level, cursor)) {
            level.setBlock(cursor, WorldrootMod.ROOT.get().defaultBlockState(), 3);
        }

        // One new block per pulse at the advancing end of each root arm.
        // This is deliberately not capped by tree height or a configured radius.
        long age = Math.max(0L, level.getGameTime() / 5L);
        int frontier = (int) Math.min(Integer.MAX_VALUE - 8L, 4L + age);
        int rootTiers = Math.max(1, Math.min(512, (height + 5) / 6));
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int tier = 0; tier < rootTiers; tier++) {
            int y = heart.getY() - 2 - tier * 6;
            if (y <= bedrockTop + 1) break;
            BlockPos anchor = new BlockPos(heart.getX(), y, heart.getZ());
            if (!level.getBlockState(anchor).is(WorldrootMod.ROOT.get())) continue;
            int step = Math.max(1, frontier);
            for (Direction dir : dirs) {
                BlockPos next = anchor.relative(dir, step).below(step / 24);
                if (next.getY() <= bedrockTop + 1 || !level.hasChunkAt(next)) continue;
                if (canRootReplace(level, next) || level.getBlockState(next).is(WorldrootMod.ROOT.get())) {
                    if (!level.getBlockState(next).is(WorldrootMod.ROOT.get()))
                        level.setBlock(next, WorldrootMod.ROOT.get().defaultBlockState(), 3);
                    // Every twelfth block sends out a permanent lateral spur.
                    if (step % 12 == 0) {
                        Direction side = dir.getClockWise();
                        BlockPos spur = next.relative(side);
                        if (spur.getY() > bedrockTop + 1 && level.hasChunkAt(spur) && canRootReplace(level, spur))
                            level.setBlock(spur, WorldrootMod.ROOT.get().defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private boolean canRootReplace(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight()) return false;
        BlockState s = level.getBlockState(pos);
        if (s.is(WorldrootMod.ROOT.get()) || s.isAir()) return true;
        // Roots may tunnel through natural terrain and stone, but not bedrock,
        // fluids, ores, containers, or player-placed utility blocks.
        return s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.COARSE_DIRT)
                || s.is(Blocks.ROOTED_DIRT) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM)
                || s.is(Blocks.SAND) || s.is(Blocks.RED_SAND) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.CLAY) || s.is(Blocks.MUD) || s.is(Blocks.MOSS_BLOCK)
                || s.is(Blocks.STONE) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.ANDESITE)
                || s.is(Blocks.DIORITE) || s.is(Blocks.GRANITE) || s.is(Blocks.TUFF);
    }

    private boolean hasWater(Level level, BlockPos pos, int radius) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) for (int z = -radius; z <= radius; z++) {
            p.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
            if (level.getFluidState(p).isSource()) return true;
        }
        return false;
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(WorldrootMod.FRAGMENT.get()));
    }
}
