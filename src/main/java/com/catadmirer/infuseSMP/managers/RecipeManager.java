package com.catadmirer.infuseSMP.managers;

import java.io.File;

import com.catadmirer.infuseSMP.effects.Ender;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import com.catadmirer.infuseSMP.Infuse;

public class RecipeManager {
    private final Infuse plugin;
    private final FileConfiguration recipesConfig;

    public RecipeManager(Infuse plugin) {
        this.plugin = plugin;

        File recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
        if (!recipesFile.exists()) {
            plugin.saveResource("recipes.yml", false);
        }

        recipesConfig = YamlConfiguration.loadConfiguration(recipesFile);
    }

    /**
     * Manager functionality for when the plugin is reloaded.
     * <p>
     * In this case, it unregisters all the recipes then adds them back.
     */
    public void reload() {
        // Removing all the infuse recipes
        for (InfuseEffect effect : InfuseEffect.getRegisteredEffects().values()) {
            Bukkit.removeRecipe(getRecipeKey(effect), true);
        }

        // Adding back the infuse recipes
        registerRecipes();
    }

    /** Registers the recipe for each effect. */
    public void registerRecipes() {
        for (InfuseEffect effect : InfuseEffect.getRegisteredEffects().values()) {
            if (isRecipeEnabled(effect)) continue;
            ShapedRecipe recipe = getRecipe(effect.getRegularVersion());

            Bukkit.addRecipe(recipe);
        }
    }

    public boolean isRecipeEnabled(InfuseEffect mapping) {
        return recipesConfig.getBoolean(mapping.getPlainKey() + ".enabled", false);
    }

    public ShapedRecipe getRecipe(InfuseEffect mapping) {
        String baseKey = mapping.getPlainKey();
        NamespacedKey recipeKey = new NamespacedKey(plugin, baseKey);
        ShapedRecipe effectRecipe = new ShapedRecipe(recipeKey, mapping.getRegularVersion().createItem());

        effectRecipe.shape(recipesConfig.getStringList(baseKey + ".shape").toArray(String[]::new));

        ConfigurationSection ingredientsConfig = recipesConfig.getConfigurationSection(baseKey + ".ingredients");
        if (ingredientsConfig == null) return effectRecipe;

        for (String key : ingredientsConfig.getKeys(false)) {
            char ingredientLabel = key.charAt(0);

            String materialName = ingredientsConfig.getString(key);
            if (materialName == null) {
                Infuse.LOGGER.error("The infuse effect '%s' has failed to register its recipe, A ingredient has not be defined properly.".formatted(baseKey));
            }

            Material ingredientMaterial = Material.valueOf(materialName.toUpperCase());
            effectRecipe.setIngredient(ingredientLabel, ingredientMaterial);
        }

        return effectRecipe;
    }

    public void updateEnderRecipe() {
        if (plugin.getDataManager().getExistingCount(new Ender(true)) > 0) {
            ShapedRecipe enderRecipe = getRecipe(new Ender(false));
            Bukkit.removeRecipe(enderRecipe.getKey(), true);

            String matName = recipesConfig.getString("ender.egg_replacement");
            Material eggReplacement = Material.valueOf(matName.toUpperCase());

            ConfigurationSection ingredientsConfig = recipesConfig.getConfigurationSection("ender.ingredients");
            for (String key : ingredientsConfig.getKeys(false)) {
                char ingredientLabel = key.charAt(0);
                if (!ingredientsConfig.getString(key).equals("DRAGON_EGG")) continue;

                enderRecipe.setIngredient(ingredientLabel, eggReplacement);
            }

            Bukkit.addRecipe(enderRecipe);
        }
    }

    public NamespacedKey getRecipeKey(InfuseEffect effect) {
        return new NamespacedKey(plugin, effect.getPlainKey());
    }

    /**
     * Gets the item to craft from an official Infuse recipe.
     * This makes it easier to determine whether an infuse recipe should craft an augmented or regular effect.
     *
     * @param recipe The infuse {@link Recipe} to determine the result for.
     *
     * @return The corresponding {@link ItemStack} for the recipe, or null if the craft limit has been reached or the recipe is not an infuse recipe.
     */
    public ItemStack getItemToCraft(Recipe recipe) {
        ItemStack item = recipe.getResult();

        // The returned EffectMapping should always be the regular form
        InfuseEffect effect = InfuseEffect.fromItem(item);
        if (effect == null) return null;
        if (effect.isAugmented()) return null;

        // Checking if the augmented limit has been reached
        InfuseEffect augEffect = effect.getAugmentedVersion();
        if (plugin.getMainConfig().getCraftLimit(augEffect) > plugin.getDataManager().getExistingCount(augEffect)) {
            return augEffect.createItem();
        }

        // Checking if the regular limit has been reached
        if (plugin.getMainConfig().getCraftLimit(effect) > plugin.getDataManager().getExistingCount(effect)) {
            return effect.createItem();
        }

        // Craft limits have been reached, return null
        return null;
    }
}
