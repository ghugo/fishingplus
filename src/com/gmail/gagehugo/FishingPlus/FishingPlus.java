// Licensed under the Apache License, Version 2.0 (the "License"); you may
// not use this file except in compliance with the License. You may obtain
// a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package com.gmail.gagehugo.FishingPlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class FishingPlus extends JavaPlugin {

    private static final String TOTALCATCHODDS = "TotalCatchOdds";
    private static final String FISH = "Fish";
    private static final String TREASURE = "Treasure";
    private static final String JUNK = "Junk";
    private static final String MOBS = "Mobs";
    private static final String TREASURECHEST = "TreasureChest";
    private static final String FISHINGENCHANTS = "FishingEnchants";
    private static final String BOWENCHANTS = "BowEnchants";
    private static final String BOOKENCHANTS = "BookEnchants";

    final List<String> totalCatchOdds = new ArrayList<>();
    final List<String> fishCatches = new ArrayList<>();
    final List<String> treasureCatches = new ArrayList<>();
    final List<String> junkCatches = new ArrayList<>();
    final List<String> treasureChestItems = new ArrayList<>();
    final List<String> mobCatches = new ArrayList<>();
    final List<String> fishingEnchants = new ArrayList<>();
    final List<String> bowEnchants = new ArrayList<>();
    final List<String> bookEnchants = new ArrayList<>();
    final Map<String, String> fishingMessages = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new FPListener(this), this);
        getCommand("fp").setExecutor(new FPCommands(this));
        if (getConfig().getBoolean("ClownfishBait")) {
            getServer().addRecipe(baitedRod());
        }
        loadValues();
    }

    @Override
    public void onDisable() {
        totalCatchOdds.clear();
        fishCatches.clear();
        treasureCatches.clear();
        junkCatches.clear();
        treasureChestItems.clear();
        fishingEnchants.clear();
        bowEnchants.clear();
        bookEnchants.clear();
    }

    void reload() {
        this.reloadConfig();
        this.onDisable();
        this.loadValues();
    }

    String getCatchType(final int chance) {
        return totalCatchOdds.get(chance);
    }

    String getFishingMessage(final String caught) {
        return fishingMessages.get(caught);
    }

    private ShapelessRecipe baitedRod() {
        final ItemStack baitedRod = new ItemStack(Material.FISHING_ROD);
        baitedRod.addEnchantment(Enchantment.LURE, 1);
        final ShapelessRecipe baitedRodRecipe = new ShapelessRecipe(baitedRod);
        baitedRodRecipe.addIngredient(Material.FISHING_ROD);
        baitedRodRecipe.addIngredient(Material.RAW_FISH, 2);
        return baitedRodRecipe;
    }

    private void checkConfig() {
        if (fishCatches.isEmpty()) {
            getLogger().log(Level.WARNING, "Error, invalid configuration values in section " + FISH);
            configError(FISH);
        }
        if (treasureCatches.isEmpty() || treasureChestItems.isEmpty()) {
            getLogger().log(Level.WARNING, "Error, invalid configuration values in section Treasure or Treasure Chest");
            configError(TREASURE);
        }
        if (junkCatches.isEmpty()) {
            getLogger().log(Level.WARNING, "Error, invalid configuration values in section " + JUNK);
            configError(JUNK);
        }
        if (mobCatches.isEmpty()) {
            getLogger().log(Level.WARNING, "Error, invalid configuration values in section " + MOBS);
            configError(MOBS);
        }
        if (fishingEnchants.isEmpty() || bowEnchants.isEmpty() || bookEnchants.isEmpty()) {
            getLogger().log(Level.WARNING, "Error, invalid configuration values in section Enchants");
            configError(TREASURE);
        }
    }

    private void configError(final String string) {
        getConfig().getConfigurationSection(TOTALCATCHODDS).set(string, 0);
        saveConfig();
    }

    private void loadValues() {
        //Load Fish Catches
        loadCatchList(fishCatches, FISH);

        //Load Treasure Catches
        loadCatchList(treasureCatches, TREASURE);

        //Load Junk Catches
        loadCatchList(junkCatches, JUNK);

        //Load Treasure Chest Catches
        loadCatchList(treasureChestItems, TREASURECHEST);

        //Load Mob Catches
        loadMobList(mobCatches, MOBS);

        //Load Fishing Enchants
        loadEnchantList(fishingEnchants, FISHINGENCHANTS);

        //Load Bow Enchants
        loadEnchantList(bowEnchants, BOWENCHANTS);

        //Load Book Enchants
        loadEnchantList(bookEnchants, BOOKENCHANTS);

        // Checks for invalid config values (all 0's and/or negative values)
        checkConfig();

        //Load Overall Odds
        for (String value : getConfig().getConfigurationSection(TOTALCATCHODDS).getKeys(false)) {
            final int chance = getConfig().getInt(TOTALCATCHODDS + "." + value);
            getLogger().log(Level.WARNING, "{0}{1}", new Object[]{String.valueOf(chance), value});
            if (chance > 0) {
                for (int i = 0; i < chance; i++) {
                    totalCatchOdds.add(value);
                }
            }
        }
    }

    private void loadCatchList(final List list, final String config) {
        final String configSection = config.concat(".");

        for (String value : getConfig().getConfigurationSection(config).getKeys(false)) {
            getLogger().log(Level.WARNING, value);
            final List<String> values = getConfig().getStringList(configSection + value);
            int chance;
            final String itemString = value;
            if (value.contains("/")) {
                final String[] splitTemp = value.split("/");
                value = splitTemp[0];
            }
            if (values.isEmpty()) {
                chance = getConfig().getInt(configSection + value);
            } else {
                chance = Integer.parseInt(values.get(0));
                fishingMessages.put(itemString, values.get(1));
            }
            if (chance > 0) {
                if (chance > 100) {
                    getLogger().log(Level.WARNING, "Found config value greater than 100, decreasing it to 100...");
                    chance = 100;
                    getConfig().getConfigurationSection(configSection).set(value, chance);
                    saveConfig();
                }
                try {
                    Material.getMaterial(value);
                } catch (NullPointerException e) {
                    getLogger().log(Level.WARNING, "Invalid item name in section {0}", config);
                    continue;
                }
                for (int i = 0; i < chance; i++) {
                    list.add(itemString);
                }
            }
        }
    }

    private void loadEnchantList(final List list, final String config) {
        final String configSection = config.concat(".");

        for (String value : getConfig().getConfigurationSection(config).getKeys(false)) {
            if (value != null) {
                int chance = getConfig().getInt(configSection + value);
                if (chance > 0) {
                    if (chance > 100) {
                        getLogger().log(Level.WARNING, "Found config value greater than 100, decreasing it to 100...");
                        chance = 100;
                        getConfig().getConfigurationSection(configSection).set(value, chance);
                        saveConfig();
                    }
                    if (value.equalsIgnoreCase("UNENCHANTED")) {
                        value = null;
                    }
                    try {
                        Enchantment.getByName(value);
                    } catch (NullPointerException e) {
                        getLogger().log(Level.WARNING, "Invalid item name in section ", config);
                        continue;
                    }
                    for (int i = chance; --i >= 0;) {
                        list.add(value);
                    }
                }
            }
        }
    }

    private void loadMobList(final List list, final String config) {
        final String configSection = config.concat(".");

        for (String value : getConfig().getConfigurationSection(config).getKeys(false)) {
            if (!value.isEmpty()) {
                final List<String> values = getConfig().getStringList(configSection + value);
                int chance;
                if (values.isEmpty()) {
                    chance = getConfig().getInt(configSection + value);
                } else {
                    chance = Integer.parseInt(values.get(0));
                    fishingMessages.put(value, values.get(1));
                }
                if (chance > 0) {
                    if (chance > 100) {
                        getLogger().log(Level.WARNING, "Found config value greater than 100, decreasing it to 100...");
                        chance = 100;
                        getConfig().getConfigurationSection(configSection).set(value, chance);
                        saveConfig();
                    }
                    try {
                        EntityType.valueOf(value);
                    } catch (NullPointerException e) {
                        getLogger().log(Level.WARNING, "Invalid mob name in section " + MOBS);
                        continue;
                    }
                    for (int i = chance; --i >= 0;) {
                        list.add(value);
                    }
                }
            }
        }
    }
}
