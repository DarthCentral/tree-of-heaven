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

/** Organic, deliberately slow growth controller for a Worldroot. */
public class WorldrootHeartBlock extends Block {
    private static final int MAX_TRUNK_SCAN = 2048;
    public WorldrootHeartBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 40);
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int height = findTrunkHeight(level, pos);
        growTrunk(level, pos);
        growCrown(level, pos, height);
        thickenTrunk(level, pos, height);
        growRoots(level, pos, height);
        level.scheduleTick(pos, this, 40);
    }

    private int findTrunkHeight(ServerLevel level, BlockPos heart) {
        int height = 0;
        BlockPos.MutableBlockPos cursor = heart.above().mutable();
        while (height < MAX_TRUNK_SCAN && cursor.getY() < level.getMaxBuildHeight() - 1
                && level.getBlockState(cursor).is(WorldrootMod.WOOD.get())) {
            height++;
            cursor.move(Direction.UP);
        }
        return height;
    }

    private void growTrunk(ServerLevel level, BlockPos heart) {
        BlockPos top = heart.above();
        int scanned = 0;
        while (scanned++ < MAX_TRUNK_SCAN && top.getY() < level.getMaxBuildHeight() - 1
                && level.getBlockState(top).is(WorldrootMod.WOOD.get())) top = top.above();
        if (top.getY() < level.getMaxBuildHeight() - 1
                && (level.isEmptyBlock(top) || level.getBlockState(top).is(Blocks.OAK_LEAVES)))
            level.setBlock(top, WorldrootMod.WOOD.get().defaultBlockState(), 3);
    }

    private void growCrown(ServerLevel level, BlockPos heart, int height) {
        if (height < 5) return;
        int tier = 0, branchY = 4;
        while (branchY < height - 1 && tier < 100) {
            RandomSource shape = RandomSource.create(shapeSeed(heart, 1000 + tier));
            BlockPos start = heart.above(branchY);
            if (level.getBlockState(start).is(WorldrootMod.WOOD.get())) {
                int limbs = shape.nextInt(4) == 0 ? 2 : 1;
                for (int limb = 0; limb < limbs; limb++) {
                    double angle = shape.nextDouble() * Math.PI * 2.0;
                    int targetLength = Math.min(15, 2 + (height - branchY) / 5);
                    targetLength = Math.min(targetLength, 5 + shape.nextInt(7));
                    growCurvedBranch(level, start, angle, targetLength,
                            shapeSeed(heart, 2000 + tier * 7L + limb));
                }
            }
            branchY += 9 + shape.nextInt(7);
            tier++;
        }
        BlockPos crown = heart.above(Math.max(1, height));
        if (level.getBlockState(crown).is(WorldrootMod.WOOD.get()) && height > 7) {
            RandomSource leaves = RandomSource.create(shapeSeed(heart, height / 4));
            placeLeafCluster(level, crown, leaves, 2 + leaves.nextInt(2));
        }
    }

    private void growCurvedBranch(ServerLevel level, BlockPos start, double angle, int length, long seed) {
        RandomSource shape = RandomSource.create(seed);
        double x = start.getX(), z = start.getZ();
        int y = start.getY();
        BlockPos tip = start;
        int lastX = start.getX(), lastZ = start.getZ(), lastY = start.getY();
        boolean forked = false;
        for (int step = 1; step <= length; step++) {
            if (step > 1 && step % (2 + shape.nextInt(3)) == 0)
                angle += (shape.nextDouble() - 0.5) * 0.9;
            x += Math.cos(angle);
            z += Math.sin(angle);
            if (step % (4 + shape.nextInt(3)) == 0 && shape.nextFloat() < 0.72f) y++;
            int bx = (int)Math.round(x), bz = (int)Math.round(z);
            if (bx == lastX && bz == lastZ && y == lastY) continue;
            BlockPos next = new BlockPos(bx, y, bz);
            if (!canPlaceWood(level, next)) break;
            level.setBlock(next, WorldrootMod.WOOD.get().defaultBlockState(), 3);
            tip = next;
            lastX = bx; lastZ = bz; lastY = y;
            if (!forked && step >= 4 && step >= length / 2 && shape.nextFloat() < 0.16f) {
                forked = true;
                double forkAngle = angle + (shape.nextBoolean() ? 1 : -1) * (0.65 + shape.nextDouble() * 0.55);
                growTwig(level, next, forkAngle, 2 + shape.nextInt(4), seed ^ 0x5DEECE66DL);
            }
        }
        if (!tip.equals(start)) {
            RandomSource leaves = RandomSource.create(seed ^ 0x9E3779B97F4A7C15L);
            placeLeafCluster(level, tip, leaves, 2 + leaves.nextInt(2));
        }
    }

    private void growTwig(ServerLevel level, BlockPos start, double angle, int length, long seed) {
        RandomSource shape = RandomSource.create(seed);
        double x = start.getX(), z = start.getZ();
        int y = start.getY();
        for (int i = 1; i <= length; i++) {
            angle += (shape.nextDouble() - 0.5) * 0.35;
            x += Math.cos(angle); z += Math.sin(angle);
            if (i % 3 == 0 && shape.nextBoolean()) y++;
            BlockPos next = new BlockPos((int)Math.round(x), y, (int)Math.round(z));
            if (!canPlaceWood(level, next)) break;
            level.setBlock(next, WorldrootMod.WOOD.get().defaultBlockState(), 3);
        }
    }

    private boolean canPlaceWood(ServerLevel level, BlockPos pos) {
        if (pos.getY() >= level.getMaxBuildHeight() - 1) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.OAK_LEAVES) || state.is(WorldrootMod.WOOD.get());
    }

    private void thickenTrunk(ServerLevel level, BlockPos heart, int height) {
        if (height < 12 || height % 16 != 0) return;
        int radius = Math.min(6, 1 + height / 70);
        int ringY = Math.max(2, height - 1);
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            double distance = Math.sqrt(dx * dx + dz * dz);
            long noise = mix64(shapeSeed(heart, dx * 31L + dz * 97L + height / 16));
            double unevenEdge = radius + (((noise >>> 11) & 1023) / 1023.0 - 0.5) * 1.2;
            if (distance > unevenEdge) continue;
            if (distance > radius - 0.4 && (noise & 3L) == 0) continue;
            BlockPos p = heart.offset(dx, ringY, dz);
            if (level.isEmptyBlock(p) || level.getBlockState(p).is(Blocks.OAK_LEAVES))
                level.setBlock(p, WorldrootMod.WOOD.get().defaultBlockState(), 3);
        }
    }

    private void placeLeafCluster(ServerLevel level, BlockPos center, RandomSource random, int radius) {
        for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dy * dy + dz * dz > radius * radius + 2 || random.nextFloat() < 0.16f) continue;
            BlockPos leaf = center.offset(dx, dy, dz);
            if (level.isEmptyBlock(leaf)) level.setBlock(leaf,
                    Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 3);
        }
    }

    private void growRoots(ServerLevel level, BlockPos heart, int height) {
        BlockPos cursor = heart.below();
        int depth = 0;
        while (depth < 1024 && cursor.getY() > level.getMinBuildHeight()
                && level.getBlockState(cursor).is(WorldrootMod.ROOT.get())) {
            cursor = cursor.below(); depth++;
        }
        if (cursor.getY() > level.getMinBuildHeight() && canRootReplace(level, cursor))
            level.setBlock(cursor, WorldrootMod.ROOT.get().defaultBlockState(), 3);

        int tier = 0, depthFromHeart = 3;
        while (depthFromHeart < depth && tier < 60) {
            RandomSource shape = RandomSource.create(shapeSeed(heart, 5000 + tier));
            BlockPos anchor = heart.below(depthFromHeart);
            if (level.getBlockState(anchor).is(WorldrootMod.ROOT.get())) {
                int rootsHere = shape.nextInt(3) == 0 ? 2 : 1;
                for (int r = 0; r < rootsHere; r++) {
                    double angle = shape.nextDouble() * Math.PI * 2.0;
                    int length = Math.min(22, 3 + height / 9 + shape.nextInt(5));
                    growCurvedRoot(level, anchor, angle, length, shapeSeed(heart, 6000 + tier * 5L + r));
                }
            }
            depthFromHeart += 7 + shape.nextInt(8);
            tier++;
        }
    }

    private void growCurvedRoot(ServerLevel level, BlockPos start, double angle, int length, long seed) {
        RandomSource shape = RandomSource.create(seed);
        double x = start.getX(), z = start.getZ();
        int y = start.getY();
        for (int step = 1; step <= length; step++) {
            if (step > 1 && step % (2 + shape.nextInt(3)) == 0)
                angle += (shape.nextDouble() - 0.5) * 1.15;
            x += Math.cos(angle); z += Math.sin(angle);
            if (step % (3 + shape.nextInt(3)) == 0) y--;
            BlockPos next = new BlockPos((int)Math.round(x), y, (int)Math.round(z));
            if (!canRootReplace(level, next) && !level.getBlockState(next).is(WorldrootMod.ROOT.get())) break;
            level.setBlock(next, WorldrootMod.ROOT.get().defaultBlockState(), 3);
            if (step % 7 == 0) {
                BlockPos spur = next.below();
                if (canRootReplace(level, spur))
                    level.setBlock(spur, WorldrootMod.ROOT.get().defaultBlockState(), 3);
            }
        }
    }

    private boolean canRootReplace(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight()) return false;
        BlockState s = level.getBlockState(pos);
        if (s.is(WorldrootMod.ROOT.get()) || s.isAir()) return true;
        return s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.COARSE_DIRT)
                || s.is(Blocks.ROOTED_DIRT) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM)
                || s.is(Blocks.SAND) || s.is(Blocks.RED_SAND) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.CLAY) || s.is(Blocks.MUD) || s.is(Blocks.MOSS_BLOCK)
                || s.is(Blocks.STONE) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.ANDESITE)
                || s.is(Blocks.DIORITE) || s.is(Blocks.GRANITE) || s.is(Blocks.TUFF);
    }

    private long shapeSeed(BlockPos pos, long salt) {
        return mix64(pos.asLong() ^ salt * 0x9E3779B97F4A7C15L);
    }

    private long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(WorldrootMod.FRAGMENT.get()));
    }
}
