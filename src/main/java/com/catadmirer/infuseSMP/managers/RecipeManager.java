package com.catadmirer.infuseSMP.managers;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ShapedRecipe;

public class RecipeManager {
    private final Infuse plugin;
    private final File recipesFile;
    private final FileConfiguration recipesConfig;

    public RecipeManager() {
        this.plugin = Infuse.getInstance();

        recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
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
        try {
            recipesConfig.load(recipesFile);
        } catch (Exception e) {
            Infuse.LOGGER.error("Could not reload recipes.yml", e);
        }

        // Removing all the infuse recipes
        for (InfuseEffect effect : InfuseEffect.getRegisteredEffects()) {
            Bukkit.removeRecipe(getRecipeKey(effect), true);
        }

        // Adding back the infuse recipes
        registerRecipes();
    }

    /** Registers the recipe for each effect. */
    public void registerRecipes() {
        for (InfuseEffect effect : InfuseEffect.getRegisteredEffects()) {
            if (effect.isAugmented()) continue;
            
            if (plugin.getMainConfig().allowInfiniteEffects()) {
                Bukkit.addRecipe(getRecipe(effect.getAugmentedVersion()));
                return;
            }

            effect = effect.getAugmentedVersion();
            int craftLimit = plugin.getMainConfig().getCraftLimit(effect);
            int crafted = plugin.getDataManager().getExistingCount(effect);

            // If augmented limit is reached, check regular limit.
            if (craftLimit == crafted) {
                effect = effect.getRegularVersion();

                craftLimit = plugin.getMainConfig().getCraftLimit(effect);
                crafted = plugin.getDataManager().getExistingCount(effect);

                // If regular limit is reached, don't register the recipe.
                if (craftLimit == crafted) continue;
            }
            
            Bukkit.addRecipe(getRecipe(effect));
        }
    }

    public boolean isRecipeEnabled(InfuseEffect mapping) {
        NamespacedKey key = getRecipeKey(mapping);
        return Bukkit.getRecipe(key) != null;
    }

    public ShapedRecipe getRecipe(InfuseEffect mapping) {
        String baseKey = mapping.key().value();
        NamespacedKey recipeKey = getRecipeKey(mapping);
        ShapedRecipe effectRecipe = new ShapedRecipe(recipeKey, mapping.createItem());

        if (mapping.isAugmented() && !recipesConfig.contains(baseKey)) {
            baseKey = mapping.getPlainKey();
        }

        effectRecipe.shape(recipesConfig.getStringList(baseKey + ".shape").toArray(String[]::new));

        ConfigurationSection ingredientsConfig = recipesConfig.getConfigurationSection(baseKey + ".ingredients");
        if (ingredientsConfig == null) return effectRecipe;

        for (String key : ingredientsConfig.getKeys(false)) {
            char ingredientLabel = key.charAt(0);

            String materialName = ingredientsConfig.getString(key);
            if (materialName == null) {
                Infuse.LOGGER.error("Failed to get a recipe for the '{}' effect.  An ingredient key has no value.", baseKey);
                continue;
            }

            NamespacedKey matKey = NamespacedKey.fromString(materialName.toLowerCase());
            if (matKey == null) {
                Infuse.LOGGER.error("Failed to get a recipe for the '{}' effect.  '{}' is an invalid material.", baseKey, materialName);
                continue;
            }

            Material ingredientMaterial = Registry.MATERIAL.get(matKey);
            if (ingredientMaterial == null) {
                Infuse.LOGGER.error("Failed to get a recipe for the '{}' effect.  The material '{}' could not be found.", baseKey, materialName);
                continue;
            }

            effectRecipe.setIngredient(ingredientLabel, ingredientMaterial);
        }

        return effectRecipe;
    }

    public NamespacedKey getRecipeKey(InfuseEffect effect) {
        return new NamespacedKey(effect.key().namespace(), effect.getPlainKey());
    }
}
