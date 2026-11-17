package com.example.worldroot;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(WorldrootMod.MODID)
public class WorldrootMod {
    public static final String MODID = "worldroot";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Block> HEART = BLOCKS.register("worldroot_heart", () -> new WorldrootHeartBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(8.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> WOOD = BLOCKS.register("worldroot_wood", () -> new WorldrootWoodBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(5.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> ROOT = BLOCKS.register("worldroot_root", () -> new WorldrootWoodBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(4.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final RegistryObject<Item> HEART_ITEM = ITEMS.register("worldroot_heart", () -> new BlockItem(HEART.get(), new Item.Properties()));
    public static final RegistryObject<Item> FRAGMENT = ITEMS.register("worldroot_fragment", () -> new WorldrootFragmentItem(new Item.Properties().stacksTo(64)));

    public WorldrootMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SEARCH || event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(HEART_ITEM);
        }
    }
}
