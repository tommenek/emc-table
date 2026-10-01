package dev.emctable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Tech Reborn support, done by reflection so the mod still runs without it.
 *
 * Tech Reborn's machine recipes (grinder, compressor, alloy smelter...) hide their ingredients
 * from the recipe book, so they are read through RebornRecipe instead. Their crafting-table
 * recipes need nothing special.
 */
final class TechRebornCompat {

    private static final Class<?> REBORN_RECIPE = find("reborncore.common.crafting.RebornRecipe");
    private static final Class<?> FLUID_RECIPE = find("reborncore.common.crafting.RebornFluidRecipe");

    /**
     * Recipe types that must not set values: random loot, scrap from anything, and fluid copying
     * would all make expensive things look cheap.
     */
    private static final Set<String> SKIPPED_TYPES = Set.of(
            "techreborn:scrapbox", "techreborn:recycler", "techreborn:fluid_replicator");

    private TechRebornCompat() {
    }

    private static Class<?> find(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    static boolean isRebornRecipe(Recipe<?> recipe) {
        return REBORN_RECIPE != null && REBORN_RECIPE.isInstance(recipe);
    }

    /** Reads a machine recipe, or null if it should not be used for values. */
    static EmcValues.Shape shape(Recipe<?> recipe) {
        // fluid inputs have no EMC, so the outputs would be undervalued
        if (FLUID_RECIPE != null && FLUID_RECIPE.isInstance(recipe)) {
            return null;
        }
        Identifier type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        if (type == null || SKIPPED_TYPES.contains(type.toString())) {
            return null;
        }
        try {
            List<?> outputs = (List<?>) REBORN_RECIPE.getMethod("outputs").invoke(recipe);
            // with several outputs there is no fair way to split the cost between them
            if (outputs.size() != 1) {
                return null;
            }
            ItemStackTemplate output = (ItemStackTemplate) outputs.getFirst();

            List<Ingredient> ingredients = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            for (Object sized : (List<?>) REBORN_RECIPE.getMethod("ingredients").invoke(recipe)) {
                Method count = sized.getClass().getMethod("count");
                Method ingredient = sized.getClass().getMethod("ingredient");
                ingredients.add((Ingredient) ingredient.invoke(sized));
                counts.add((Integer) count.invoke(sized));
            }
            return new EmcValues.Shape(ingredients, counts, output.item().value(), output.count());
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    /** Raw materials: what you mine or grow. Everything made from them is derived. */
    static Map<String, Long> baseValues() {
        Map<String, Long> base = new HashMap<>();
        // ores, for silk touch; the ones that drop themselves also get processed from here
        ore(base, "bauxite", 256L);
        ore(base, "cinnabar", 256L);
        ore(base, "galena", 512L);
        ore(base, "iridium", 16384L);
        ore(base, "lead", 512L);
        ore(base, "peridot", 2048L);
        ore(base, "pyrite", 256L);
        ore(base, "ruby", 2048L);
        ore(base, "sapphire", 2048L);
        ore(base, "sheldonite", 8192L);
        ore(base, "silver", 1024L);
        ore(base, "sodalite", 256L);
        ore(base, "sphalerite", 256L);
        ore(base, "tin", 256L);
        ore(base, "tungsten", 4096L);
        ore(base, "uranium", 4096L);
        // what the metal ores drop
        base.put("techreborn:raw_tin", 256L);
        base.put("techreborn:raw_lead", 512L);
        base.put("techreborn:raw_silver", 1024L);
        base.put("techreborn:raw_tungsten", 4096L);
        base.put("techreborn:raw_iridium", 16384L);
        base.put("techreborn:raw_uranium", 4096L);
        // what the gem ores drop
        base.put("techreborn:ruby_gem", 2048L);
        base.put("techreborn:sapphire_gem", 2048L);
        base.put("techreborn:peridot_gem", 2048L);
        base.put("techreborn:red_garnet_gem", 1024L);
        base.put("techreborn:yellow_garnet_gem", 1024L);
        // rubber trees
        base.put("techreborn:rubber_log", 32L);
        base.put("techreborn:rubber_sapling", 32L);
        base.put("techreborn:rubber_leaves", 1L);
        return base;
    }

    /** Values that recipes are not allowed to lower. */
    static Map<String, Long> fixedValues() {
        return Map.of(
                "techreborn:sap", 64L,
                "techreborn:rubber", 96L);
    }

    private static void ore(Map<String, Long> base, String name, long value) {
        base.put("techreborn:" + name + "_ore", value);
        base.put("techreborn:deepslate_" + name + "_ore", value);
    }
}
