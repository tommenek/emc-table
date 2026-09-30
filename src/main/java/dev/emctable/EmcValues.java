package dev.emctable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Decides what everything is worth.
 *
 * Raw materials (things you dig up, grow or kill for) get a hand-written value below - nothing
 * can work those out for you. Everything else is derived from crafting recipes: an item is worth
 * what its ingredients are worth, divided by how many the recipe makes. Deriving runs in several
 * passes because a recipe's ingredients may themselves need deriving first, and each item keeps
 * the CHEAPEST value found, so a costly recipe can't inflate something you can make cheaply.
 */
public final class EmcValues {

    /** item id -> emc. Filled once per server start. */
    private static final Map<String, Long> VALUES = new HashMap<>();

    /** How many times to sweep the recipe list looking for newly derivable items. */
    private static final int PASSES = 12;

    private EmcValues() {
    }

    /** Hand-written values for things that cannot be crafted from anything cheaper. */
    private static Map<String, Long> baseValues() {
        Map<String, Long> base = new HashMap<>();
        // stone and dirt
        base.put("minecraft:cobblestone", 1L);
        base.put("minecraft:stone", 1L);
        base.put("minecraft:dirt", 1L);
        base.put("minecraft:gravel", 4L);
        base.put("minecraft:sand", 1L);
        base.put("minecraft:red_sand", 1L);
        base.put("minecraft:clay_ball", 16L);
        base.put("minecraft:flint", 4L);
        base.put("minecraft:obsidian", 64L);
        base.put("minecraft:netherrack", 1L);
        base.put("minecraft:end_stone", 1L);
        base.put("minecraft:soul_sand", 49L);
        base.put("minecraft:magma_block", 128L);
        // ores and metals
        base.put("minecraft:coal", 128L);
        base.put("minecraft:charcoal", 32L);
        base.put("minecraft:raw_iron", 256L);
        base.put("minecraft:iron_ingot", 256L);
        base.put("minecraft:raw_copper", 85L);
        base.put("minecraft:copper_ingot", 85L);
        base.put("minecraft:raw_gold", 2048L);
        base.put("minecraft:gold_ingot", 2048L);
        base.put("minecraft:diamond", 8192L);
        base.put("minecraft:emerald", 16384L);
        base.put("minecraft:lapis_lazuli", 864L);
        base.put("minecraft:redstone", 64L);
        base.put("minecraft:quartz", 256L);
        base.put("minecraft:amethyst_shard", 512L);
        base.put("minecraft:ancient_debris", 12288L);
        base.put("minecraft:netherite_scrap", 12288L);
        base.put("minecraft:glowstone_dust", 384L);
        base.put("minecraft:ender_pearl", 1024L);
        base.put("minecraft:blaze_rod", 1536L);
        base.put("minecraft:nether_star", 139264L);
        base.put("minecraft:dragon_egg", 262144L);
        base.put("minecraft:echo_shard", 4096L);
        base.put("minecraft:heart_of_the_sea", 32768L);
        base.put("minecraft:nautilus_shell", 2048L);
        // wood and plants
        base.put("minecraft:oak_log", 32L);
        base.put("minecraft:birch_log", 32L);
        base.put("minecraft:spruce_log", 32L);
        base.put("minecraft:jungle_log", 32L);
        base.put("minecraft:acacia_log", 32L);
        base.put("minecraft:dark_oak_log", 32L);
        base.put("minecraft:mangrove_log", 32L);
        base.put("minecraft:cherry_log", 32L);
        base.put("minecraft:pale_oak_log", 32L);
        base.put("minecraft:crimson_stem", 32L);
        base.put("minecraft:warped_stem", 32L);
        base.put("minecraft:bamboo", 8L);
        base.put("minecraft:wheat", 24L);
        base.put("minecraft:wheat_seeds", 16L);
        base.put("minecraft:potato", 24L);
        base.put("minecraft:carrot", 24L);
        base.put("minecraft:beetroot", 24L);
        base.put("minecraft:sugar_cane", 32L);
        base.put("minecraft:cactus", 32L);
        base.put("minecraft:apple", 128L);
        base.put("minecraft:sweet_berries", 32L);
        base.put("minecraft:glow_berries", 32L);
        base.put("minecraft:cocoa_beans", 32L);
        base.put("minecraft:melon_slice", 16L);
        base.put("minecraft:pumpkin", 144L);
        base.put("minecraft:nether_wart", 24L);
        base.put("minecraft:chorus_fruit", 192L);
        base.put("minecraft:kelp", 16L);
        base.put("minecraft:seagrass", 16L);
        base.put("minecraft:vine", 16L);
        base.put("minecraft:brown_mushroom", 32L);
        base.put("minecraft:red_mushroom", 32L);
        base.put("minecraft:sea_pickle", 64L);
        base.put("minecraft:lily_pad", 16L);
        base.put("minecraft:snowball", 1L);
        base.put("minecraft:ice", 4L);
        // mob drops
        base.put("minecraft:rotten_flesh", 32L);
        base.put("minecraft:bone", 48L);
        base.put("minecraft:string", 12L);
        base.put("minecraft:spider_eye", 128L);
        base.put("minecraft:gunpowder", 192L);
        base.put("minecraft:slime_ball", 32L);
        base.put("minecraft:leather", 64L);
        base.put("minecraft:feather", 48L);
        base.put("minecraft:egg", 32L);
        base.put("minecraft:porkchop", 64L);
        base.put("minecraft:beef", 64L);
        base.put("minecraft:chicken", 64L);
        base.put("minecraft:mutton", 64L);
        base.put("minecraft:rabbit", 64L);
        base.put("minecraft:rabbit_hide", 16L);
        base.put("minecraft:rabbit_foot", 128L);
        base.put("minecraft:cod", 64L);
        base.put("minecraft:salmon", 64L);
        base.put("minecraft:tropical_fish", 64L);
        base.put("minecraft:pufferfish", 64L);
        base.put("minecraft:ink_sac", 16L);
        base.put("minecraft:glow_ink_sac", 32L);
        base.put("minecraft:ghast_tear", 4096L);
        base.put("minecraft:magma_cream", 64L);
        base.put("minecraft:phantom_membrane", 192L);
        base.put("minecraft:shulker_shell", 2048L);
        base.put("minecraft:prismarine_shard", 256L);
        base.put("minecraft:prismarine_crystals", 512L);
        base.put("minecraft:scute", 1024L);
        base.put("minecraft:honeycomb", 96L);
        base.put("minecraft:honey_bottle", 192L);
        base.put("minecraft:wither_skeleton_skull", 24576L);
        base.put("minecraft:white_wool", 48L);
        // misc naturals
        base.put("minecraft:water_bucket", 768L);
        base.put("minecraft:lava_bucket", 832L);
        base.put("minecraft:milk_bucket", 768L);
        return base;
    }

    /** Works out a value for every item, once, at server start. */
    public static void compute(MinecraftServer server) {
        VALUES.clear();
        VALUES.putAll(baseValues());

        List<RecipeHolder<?>> recipes = List.copyOf(server.getRecipeManager().getRecipes());
        for (int pass = 0; pass < PASSES; pass++) {
            boolean changed = false;
            for (RecipeHolder<?> holder : recipes) {
                if (derive(server, holder.value())) {
                    changed = true;
                }
            }
            if (!changed) {
                break; // nothing new became derivable, so further passes cannot help
            }
        }
        EmcTableMod.LOGGER.info("EMC Table: valued {} items", VALUES.size());
    }

    /** Returns true if this recipe let us give its result a new (or cheaper) value. */
    private static boolean derive(MinecraftServer server, Recipe<?> recipe) {
        ItemStack result;
        try {
            result = recipe.getResultItem(server.registryAccess());
        } catch (Exception e) {
            return false; // special/dynamic recipes have no fixed result
        }
        if (result == null || result.isEmpty() || result.getCount() <= 0) {
            return false;
        }

        long total = 0;
        List<Ingredient> ingredients;
        try {
            ingredients = recipe.placementInfo().ingredients();
        } catch (Exception e) {
            return false;
        }
        if (ingredients.isEmpty()) {
            return false;
        }
        for (Ingredient ingredient : ingredients) {
            long cheapest = Long.MAX_VALUE;
            for (var entry : ingredient.items()) {
                Long value = VALUES.get(entry.value().builtInRegistryHolder().key().location().toString());
                if (value != null && value < cheapest) {
                    cheapest = value;
                }
            }
            if (cheapest == Long.MAX_VALUE) {
                return false; // an ingredient has no value yet; try again next pass
            }
            total += cheapest;
        }

        long each = Math.max(1L, total / result.getCount());
        String id = key(result.getItem());
        Long existing = VALUES.get(id);
        if (existing == null || each < existing) {
            VALUES.put(id, each);
            return true;
        }
        return false;
    }

    public static String key(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? "" : id.toString();
    }

    /** 0 means "no value", which also means the table will not accept it. */
    public static long valueOf(Item item) {
        return VALUES.getOrDefault(key(item), 0L);
    }

    public static long valueOf(String id) {
        return VALUES.getOrDefault(id, 0L);
    }

    public static boolean hasValue(ItemStack stack) {
        return !stack.isEmpty() && valueOf(stack.getItem()) > 0;
    }
}
