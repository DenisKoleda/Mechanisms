package com.denis.mechanisms.crafting;

import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.block.MechanismItemService;
import com.denis.mechanisms.config.CraftingProfile;
import com.denis.mechanisms.config.MechanismsConfig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public final class MechanismRecipeService {
    private static final List<String> RECIPE_NAMES = List.of(
        "pipe",
        "pipe_fast",
        "pipe_express",
        "extractor",
        "router",
        "inserter",
        "overflow",
        "trash",
        "wrench"
    );

    private final JavaPlugin plugin;
    private final MechanismsConfig config;
    private final MechanismItemService itemService;

    public MechanismRecipeService(JavaPlugin plugin, MechanismsConfig config, MechanismItemService itemService) {
        this.plugin = plugin;
        this.config = config;
        this.itemService = itemService;
    }

    public void reload() {
        unregister();
        if (!config.recipesEnabled()) {
            return;
        }
        register(pipe());
        register(fastPipe());
        register(expressPipe());
        register(extractor());
        register(router());
        register(inserter());
        register(overflow());
        register(trash());
        register(wrench());
    }

    public void unregister() {
        for (String name : RECIPE_NAMES) {
            plugin.getServer().removeRecipe(key(name));
        }
    }

    public boolean allRegistered() {
        if (!config.recipesEnabled()) {
            return true;
        }
        return RECIPE_NAMES.stream().allMatch(name -> plugin.getServer().getRecipe(key(name)) != null);
    }

    public List<String> recipeNames() {
        return RECIPE_NAMES;
    }

    public int discoverAll(Player player) {
        if (!config.recipesEnabled()) {
            return 0;
        }
        for (String name : RECIPE_NAMES) {
            player.discoverRecipe(key(name));
        }
        return RECIPE_NAMES.size();
    }

    public RecipeInfo recipeInfo(MechanismBlockType type) {
        CraftingProfile profile = config.craftingProfile();
        return switch (type) {
            case PIPE -> new RecipeInfo(type, pipeAmount(), List.of("CCC", "G G", "CCC"), List.of(
                "C = " + materialName(profile == CraftingProfile.EXPENSIVE ? Material.COPPER_BLOCK : Material.COPPER_INGOT),
                "G = " + materialName(profile == CraftingProfile.EXPENSIVE ? tintedGlass() : Material.GLASS)
            ));
            case PIPE_FAST -> new RecipeInfo(type, pipeUpgradeAmount(), List.of("RPR", "PBP", "RPR"), List.of(
                "P = Труба I",
                "R = " + materialName(profile == CraftingProfile.EXPENSIVE ? Material.REDSTONE_BLOCK : Material.REDSTONE),
                "B = " + materialName(profile == CraftingProfile.EXPENSIVE ? Material.DIAMOND : Material.REDSTONE_BLOCK)
            ));
            case PIPE_EXPRESS -> new RecipeInfo(type, pipeUpgradeAmount(), List.of("EPE", "PDP", "EPE"), List.of(
                "P = Труба II",
                "E = " + materialName(profile == CraftingProfile.EXPENSIVE ? Material.ENDER_EYE : Material.ENDER_PEARL),
                "D = " + materialName(profile == CraftingProfile.EXPENSIVE ? Material.DIAMOND_BLOCK : Material.DIAMOND)
            ));
            case EXTRACTOR -> new RecipeInfo(type, blockAmount(), List.of("IRI", "RHR", "IRI"), List.of("H = воронка", "I = " + iron(), "R = " + redstone()));
            case ROUTER -> new RecipeInfo(type, blockAmount(), List.of("RCR", "CTC", "RCR"), List.of("T = мишень", "C = компаратор", "R = " + redstone()));
            case INSERTER -> new RecipeInfo(type, blockAmount(), List.of("IRI", "RDR", "IRI"), List.of("D = раздатчик", "I = " + iron(), "R = " + redstone()));
            case OVERFLOW -> new RecipeInfo(type, blockAmount(), List.of("IRI", "RBR", "IRI"), List.of("B = бочка", "I = " + iron(), "R = " + redstone()));
            case TRASH -> new RecipeInfo(type, blockAmount(), List.of("IMI", "RCR", "IMI"), List.of("C = котел", "M = магмовый блок", "I = " + iron(), "R = " + redstone()));
        };
    }

    public ToolRecipeInfo wrenchRecipeInfo() {
        return new ToolRecipeInfo("Ключ Mechanisms", 1, List.of(" II", " SI", "S  "), List.of("I = железный слиток", "S = палка"));
    }

    private void register(ShapedRecipe recipe) {
        plugin.getServer().addRecipe(recipe);
    }

    private ShapedRecipe pipe() {
        ShapedRecipe recipe = recipe("pipe", MechanismBlockType.PIPE, pipeAmount());
        recipe.shape("CCC", "G G", "CCC");
        recipe.setIngredient('C', config.craftingProfile() == CraftingProfile.EXPENSIVE ? Material.COPPER_BLOCK : Material.COPPER_INGOT);
        recipe.setIngredient('G', config.craftingProfile() == CraftingProfile.EXPENSIVE ? tintedGlass() : Material.GLASS);
        return recipe;
    }

    private ShapedRecipe fastPipe() {
        ShapedRecipe recipe = recipe("pipe_fast", MechanismBlockType.PIPE_FAST, pipeUpgradeAmount());
        recipe.shape("RPR", "PBP", "RPR");
        recipe.setIngredient('R', config.craftingProfile() == CraftingProfile.EXPENSIVE ? Material.REDSTONE_BLOCK : Material.REDSTONE);
        recipe.setIngredient('P', exact(MechanismBlockType.PIPE));
        recipe.setIngredient('B', config.craftingProfile() == CraftingProfile.EXPENSIVE ? Material.DIAMOND : Material.REDSTONE_BLOCK);
        return recipe;
    }

    private ShapedRecipe expressPipe() {
        ShapedRecipe recipe = recipe("pipe_express", MechanismBlockType.PIPE_EXPRESS, pipeUpgradeAmount());
        recipe.shape("EPE", "PDP", "EPE");
        recipe.setIngredient('E', config.craftingProfile() == CraftingProfile.EXPENSIVE ? Material.ENDER_EYE : Material.ENDER_PEARL);
        recipe.setIngredient('P', exact(MechanismBlockType.PIPE_FAST));
        recipe.setIngredient('D', config.craftingProfile() == CraftingProfile.EXPENSIVE ? Material.DIAMOND_BLOCK : Material.DIAMOND);
        return recipe;
    }

    private ShapedRecipe extractor() {
        ShapedRecipe recipe = recipe("extractor", MechanismBlockType.EXTRACTOR, blockAmount());
        recipe.shape("IRI", "RHR", "IRI");
        recipe.setIngredient('I', ironMaterial());
        recipe.setIngredient('R', redstoneMaterial());
        recipe.setIngredient('H', Material.HOPPER);
        return recipe;
    }

    private ShapedRecipe router() {
        ShapedRecipe recipe = recipe("router", MechanismBlockType.ROUTER, blockAmount());
        recipe.shape("RCR", "CTC", "RCR");
        recipe.setIngredient('R', redstoneMaterial());
        recipe.setIngredient('C', Material.COMPARATOR);
        recipe.setIngredient('T', Material.TARGET);
        return recipe;
    }

    private ShapedRecipe inserter() {
        ShapedRecipe recipe = recipe("inserter", MechanismBlockType.INSERTER, blockAmount());
        recipe.shape("IRI", "RDR", "IRI");
        recipe.setIngredient('I', ironMaterial());
        recipe.setIngredient('R', redstoneMaterial());
        recipe.setIngredient('D', Material.DROPPER);
        return recipe;
    }

    private ShapedRecipe overflow() {
        ShapedRecipe recipe = recipe("overflow", MechanismBlockType.OVERFLOW, blockAmount());
        recipe.shape("IRI", "RBR", "IRI");
        recipe.setIngredient('I', ironMaterial());
        recipe.setIngredient('R', redstoneMaterial());
        recipe.setIngredient('B', Material.BARREL);
        return recipe;
    }

    private ShapedRecipe trash() {
        ShapedRecipe recipe = recipe("trash", MechanismBlockType.TRASH, blockAmount());
        recipe.shape("IMI", "RCR", "IMI");
        recipe.setIngredient('I', ironMaterial());
        recipe.setIngredient('M', Material.MAGMA_BLOCK);
        recipe.setIngredient('R', redstoneMaterial());
        recipe.setIngredient('C', Material.CAULDRON);
        return recipe;
    }

    private ShapedRecipe wrench() {
        ShapedRecipe recipe = new ShapedRecipe(key("wrench"), itemService.createWrench());
        recipe.shape(" II", " SI", "S  ");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STICK);
        return recipe;
    }

    private ShapedRecipe recipe(String name, MechanismBlockType type, int amount) {
        return new ShapedRecipe(key(name), itemService.createMechanismItem(type, amount));
    }

    private RecipeChoice.ExactChoice exact(MechanismBlockType type) {
        ItemStack item = itemService.createMechanismItem(type, 1);
        return new RecipeChoice.ExactChoice(Arrays.asList(item));
    }

    private int pipeAmount() {
        return switch (config.craftingProfile()) {
            case EASY -> 12;
            case NORMAL -> 8;
            case EXPENSIVE -> 4;
        };
    }

    private int pipeUpgradeAmount() {
        return config.craftingProfile() == CraftingProfile.EXPENSIVE ? 4 : 8;
    }

    private int blockAmount() {
        return config.craftingProfile() == CraftingProfile.EASY ? 2 : 1;
    }

    private Material ironMaterial() {
        return config.craftingProfile() == CraftingProfile.EXPENSIVE ? Material.IRON_BLOCK : Material.IRON_INGOT;
    }

    private Material redstoneMaterial() {
        return config.craftingProfile() == CraftingProfile.EXPENSIVE ? Material.REDSTONE_BLOCK : Material.REDSTONE;
    }

    private String iron() {
        return materialName(ironMaterial());
    }

    private String redstone() {
        return materialName(redstoneMaterial());
    }

    private Material tintedGlass() {
        Material material = Material.matchMaterial("TINTED_GLASS");
        return material == null ? Material.GLASS : material;
    }

    private String materialName(Material material) {
        return material.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }

    public record RecipeInfo(MechanismBlockType type, int resultAmount, List<String> shape, List<String> legend) {
    }

    public record ToolRecipeInfo(String name, int resultAmount, List<String> shape, List<String> legend) {
    }
}
