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

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class FPCommands implements CommandExecutor {

    private final FishingPlus plugin;
    private final FPListener execute;

    FPCommands(final FishingPlus plugin) {
        this.plugin = plugin;
        execute = new FPListener(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "fp":
                if (args.length != 1) {
                    return false;
                }
                switch (args[0]) {
                    case "reload":
                        plugin.reload();
                        sender.sendMessage(ChatColor.GOLD + "FishingPlus Reloaded");
                        break;
                    case "treasure":
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.GOLD + "This command can only be used by players!");
                            return true;
                        }
                        execute.spawnChest((Player) sender);
                        sender.sendMessage(ChatColor.GOLD + "Spawned a treasure chest.");
                        break;
                    case "test":
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.GOLD + "This command can only be used by players!");
                            return true;
                        }
                        final ItemStack testRod = new ItemStack(Material.FISHING_ROD);
                        final ItemMeta testRodMeta = testRod.getItemMeta();
                        testRodMeta.setDisplayName(ChatColor.GOLD + "Aeyther's Testing Rod");
                        testRod.setItemMeta(testRodMeta);
                        testRod.addEnchantment(Enchantment.LURE, 3);
                        testRod.addEnchantment(Enchantment.LUCK, 3);
                        testRod.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
                        ((Player) sender).getWorld().dropItemNaturally(((Player) sender).getLocation(), testRod);
                        sender.sendMessage(ChatColor.GOLD + "Testing Purposes Only!");
                        break;
                    default:
                        return false;
                }
                break;
            default:
                return false;
        }
        return true;
    }
}
