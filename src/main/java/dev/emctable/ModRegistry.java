package dev.emctable;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModRegistry {

    public static final Block EMC_TABLE;
    public static final BlockItem EMC_TABLE_ITEM;
    public static final MenuType<EmcTableMenu> EMC_TABLE_MENU;
    public static final Item EMC_ORB;
    public static final Item TRANSMUTATION_TABLET;

    static {
        Identifier id = EmcTableMod.id("emc_table");
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        EMC_TABLE = Registry.register(BuiltInRegistries.BLOCK, blockKey,
                new EmcTableBlock(BlockBehaviour.Properties.of()
                        .strength(5.0F, 1200.0F)
                        .sound(SoundType.STONE)
                        .requiresCorrectToolForDrops()
                        .setId(blockKey)));

        EMC_TABLE_ITEM = Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(EMC_TABLE, new Item.Properties()
                        .setId(itemKey)
                        .useBlockDescriptionPrefix()));

        ResourceKey<Item> orbKey = ResourceKey.create(Registries.ITEM, EmcTableMod.id("emc_orb"));
        EMC_ORB = Registry.register(BuiltInRegistries.ITEM, orbKey,
                new Item(new Item.Properties()
                        .setId(orbKey)
                        .stacksTo(16)
                        .rarity(Rarity.EPIC)));

        ResourceKey<Item> tabletKey = ResourceKey.create(Registries.ITEM, EmcTableMod.id("transmutation_tablet"));
        TRANSMUTATION_TABLET = Registry.register(BuiltInRegistries.ITEM, tabletKey,
                new TransmutationTabletItem(new Item.Properties()
                        .setId(tabletKey)
                        .stacksTo(1)
                        .rarity(Rarity.EPIC)));

        EMC_TABLE_MENU = Registry.register(BuiltInRegistries.MENU, id,
                new MenuType<>(EmcTableMenu::new, FeatureFlags.VANILLA_SET));
    }

    public static void init() {
    }

    private ModRegistry() {
    }
}
