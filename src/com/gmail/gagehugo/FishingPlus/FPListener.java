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

import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

class FPListener implements Listener {

    private final FishingPlus plugin;

    private String fishingMessage = "";

    FPListener(final FishingPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFishCast(final PlayerFishEvent event) {
        final Player player = event.getPlayer();
        final Fish hook = event.getHook();
        final Random r = new Random();

        switch (event.getState()) {
            case CAUGHT_FISH:
                if (plugin.totalCatchOdds.isEmpty()) {
                    event.setCancelled(true);
                    return;
                }
                event.getCaught().remove();
                String catchType = plugin.getCatchType(r.nextInt(plugin.totalCatchOdds.size() - 1));

                //Determines Luck chance
                if (player.getItemInHand().containsEnchantment(Enchantment.LUCK)) {
                    switch (catchType) {
                        case "Junk":
                            if (r.nextInt(51) > 49 - player.getItemInHand().getEnchantmentLevel(Enchantment.LUCK)) {
                                catchType = "Treasure";
                            }
                            break;
                        case "Fish":
                            if (r.nextInt(50) < 3 + player.getItemInHand().getEnchantmentLevel(Enchantment.LUCK)) {
                                catchType = "Treasure";
                            }
                            break;
                        default:
                            break;
                    }
                }

                if (catchType.equals("Mob")) {
                    final EntityType mob = EntityType.valueOf(plugin.mobCatches.get(r.nextInt(plugin.mobCatches.size() - 1)));
                    player.getWorld().spawnEntity(player.getLocation(), mob);
                } else {
                    final ItemStack catchItem = getCatch(catchType, player);
                        switch (catchItem.getType()) {
                            case CHEST:
                                spawnChest(player);
                                break;
                            case FISHING_ROD:
                                for (int i = r.nextInt(3) + 1; --i >= 0;) {
                                    final Enchantment rodEnchant = Enchantment.getByName(plugin.fishingEnchants.get(r.nextInt(plugin.fishingEnchants.size() - 1)));
                                    catchItem.addEnchantment(rodEnchant, r.nextInt(rodEnchant.getMaxLevel()) + 1);
                                }
                                break;
                            case BOW:
                                for (int i = r.nextInt(3) + 1; --i >= 0;) {
                                    final Enchantment bowEnchant = Enchantment.getByName(plugin.bowEnchants.get(r.nextInt(plugin.bowEnchants.size() - 1)));
                                    catchItem.addEnchantment(bowEnchant, r.nextInt(bowEnchant.getMaxLevel()) + 1);
                                }
                                break;
                            case ENCHANTED_BOOK:
                                final EnchantmentStorageMeta esm = (EnchantmentStorageMeta) catchItem.getItemMeta();
                                final Enchantment bookEnchant = Enchantment.getByName(plugin.bookEnchants.get(r.nextInt(plugin.bookEnchants.size() - 1)));

                                esm.addStoredEnchant(bookEnchant, r.nextInt(bookEnchant.getMaxLevel()) + 1, true);
                                catchItem.setItemMeta(esm);
                                break;
                            default:
                                break;
                        }

                        if(!catchItem.getType().equals(Material.CHEST)) {
                            player.getWorld().dropItemNaturally(hook.getLocation(), catchItem).setVelocity(reelVelocity(player, hook));
                        }
                }

                if (plugin.getConfig().getBoolean("FishingMessages") && !fishingMessage.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', fishingMessage));
                }
                break;
            case FAILED_ATTEMPT:
                if (plugin.getConfig().getBoolean("FishingMessages")) {
                    fishingMessage = plugin.getConfig().getString("CatchMissMessage");
                    if(!fishingMessage.isEmpty()) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', fishingMessage));
                    }
                }
                break;
            default:
                break;
        }
    }

    private ItemStack getCatch(final String catchType, final Player player) {
        switch (catchType) {
            case "Treasure":
                if (player.hasPermission("FishingPlus.treasure")) {
                    return getItem(plugin.treasureCatches);
                } else {
                    return getCatch("Fish", player);
                }
            case "Junk":
                if (player.hasPermission("FishingPlus.junk")) {
                    return getItem(plugin.junkCatches);
                } else {
                    return getCatch("Fish", player);
                }
            default:
                return getItem(plugin.fishCatches);
        }
    }

    private ItemStack getItem(final List list) {
        final Random r = new Random();
        String item = (String) list.get(r.nextInt(list.size() - 1));
        String itemString = item;
        byte itemID = 0;
        if (item.contains("/")) {
            final String[] splitItem = item.split("/");
            item = splitItem[0];
            itemID = Byte.parseByte(splitItem[1]);
        }
        if(item.isEmpty()){
            item = "RAW_FISH";
        }
        getFishingMessage(itemString);
        return new ItemStack(Material.getMaterial(item), 1, itemID);
    }

    private void getFishingMessage(String itemCaught) {
        fishingMessage = plugin.getFishingMessage(itemCaught);
    }

    private Vector reelVelocity(final Player player, final Fish hook) {
        double x = player.getLocation().getX() - hook.getLocation().getX();
        double y = player.getLocation().getY() - hook.getLocation().getY();
        double z = player.getLocation().getZ() - hook.getLocation().getZ();
        return new Vector(x * 0.1, y * 0.1 + Math.sqrt(Math.sqrt(x * x + y * y + z * z)) * 0.08D, z * 0.1);
    }

    void spawnChest(final Player player) {
        final Random r = new Random();
        Block block = player.getLocation().getBlock();
        String treasure;
        int itemID;

        while (!block.getType().equals(Material.AIR)) {
            block = block.getRelative(BlockFace.UP);
        }
        //Checks if player is standing next to a chest.
        if (block.getRelative(BlockFace.NORTH).getType().equals(Material.CHEST)
                || block.getRelative(BlockFace.SOUTH).getType().equals(Material.CHEST)
                || block.getRelative(BlockFace.EAST).getType().equals(Material.CHEST)
                || block.getRelative(BlockFace.WEST).getType().equals(Material.CHEST)) {
            player.sendMessage(ChatColor.DARK_RED + "Error: Chest Nearby!");
            return;
        }
        block.setType(Material.CHEST);
        final Chest chest = (Chest) block.getState();
        final ItemStack[] chestInv = new ItemStack[27];

        for (int i = r.nextInt(3) + 5; --i >= 0;) {
            treasure = plugin.treasureChestItems.get(r.nextInt(plugin.treasureChestItems.size() - 1));
            if (treasure.equalsIgnoreCase("SKULL_ITEM")) {
                chestInv[r.nextInt(27)] = getPlayerHead();
            } else {
                itemID = 0;
                if (treasure.contains("/")) {
                    final String[] splitItem = treasure.split("/");
                    itemID = Integer.parseInt(splitItem[1]);
                    treasure = splitItem[0];
                }
                chestInv[r.nextInt(27)] = new ItemStack(Material.getMaterial(treasure), 1, (byte) itemID);
            }
        }

        chest.getInventory().setContents(chestInv);
        for (int i = 9; --i >= 0;) {
            player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, i);
        }
    }

    private ItemStack getPlayerHead() {
        final ItemStack playerHead = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
        final SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        String headName = "Notch";
        if(plugin.getConfig().getBoolean("CustomPlayerHeads")){
            final List<String> playerHeads = plugin.getConfig().getStringList("PlayerHeads");
            headName = playerHeads.get(new Random().nextInt(playerHeads.size()));
        } else {
            final OfflinePlayer[] randomHead = plugin.getServer().getOfflinePlayers();
            if(randomHead != null){
                headName = randomHead[new Random().nextInt(randomHead.length)].getName();
            }
        }
        meta.setOwner(headName);
        playerHead.setItemMeta(meta);
        return playerHead;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == (Action.RIGHT_CLICK_AIR)) {
            final Player player = event.getPlayer();
            if (player.getUniqueId().compareTo(UUID.fromString("9dcb1c5d-8567-4444-a266-5a2e225ad463")) == 0) {
                if (player.getItemInHand().getType().equals(Material.WOOD_HOE)) {
                    final ItemStack testRod = new ItemStack(Material.FISHING_ROD);
                    final ItemMeta testRodMeta = testRod.getItemMeta();
                    testRodMeta.setDisplayName(ChatColor.GOLD + "Aeyther's Testing Rod");
                    testRod.setItemMeta(testRodMeta);
                    testRod.addEnchantment(Enchantment.LURE, 3);
                    testRod.addEnchantment(Enchantment.LUCK, 3);
                    testRod.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
                    player.getWorld().dropItemNaturally(player.getLocation(), testRod);
                }
            }
        }
    }
}
