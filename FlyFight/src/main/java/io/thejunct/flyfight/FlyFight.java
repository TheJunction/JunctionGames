/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.flyfight;

import io.thejunct.core.game.Minigame;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by david on 4/25.
 */
public class FlyFight extends JavaPlugin {

    private static FlyFight instance;

    private Minigame flyFight;

    private Location bound1; //largest x, y, and z
    private Location bound2; //smallest x, y, and z

    private Player specialPlayer;

    public static FlyFight getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("Core") == null) {
            getLogger().severe("Core plugin not found. Disabling FlyFight...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        flyFight = new Minigame("FlyFight", 4, 16, false);

        Bukkit.getPluginManager().registerEvents(new Listeners(), this);

        setupWorld();
    }

    private void spawnRandom() {
        if (flyFight.isRunning()) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                ItemStack specialItem = Special.values()[getRandInt(0, Special.values().length)].getItem();

                Item droppedItem = flyFight.getWorld().dropItem(getRandLoc(flyFight.getWorld()).clone().add(0.5, 1, 0.5), specialItem);
                droppedItem.setVelocity(new Vector()); //todo: Make sure item spawns and doesn't move
            }, ThreadLocalRandom.current().nextLong(1, 10) * 20);
        }
    }

    boolean isSpecial(ItemStack item) {
        return (getSpecial(item) != null);
    }

    Special getSpecial(ItemStack item) {
        if (item != null && !isBoost(item) && item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null && Special.getSpecialFromItemName(item.getItemMeta().getDisplayName()) != null) {
            return Special.getSpecialFromItemName(item.getItemMeta().getDisplayName());
        } else {
            return null;
        }
    }

    boolean isBoost(ItemStack item) {
        return (item != null && item.getType().equals(Material.GOLD_NUGGET) && item.getAmount() == 1);
    }

    void respawn(Player p) {
        respawn(p, 0);
    }

    private void respawn(Player p, int iterator) {

        iterator++;

        int yMin = (bound1.getBlockY() - bound2.getBlockY()) * 3 / 4 + bound2.getBlockY();


        Location randLoc = getRandLoc(yMin, flyFight.getWorld());

        for (Entity e : flyFight.getWorld().getNearbyEntities(randLoc, 5, 5, 3)) {
            if (e instanceof Player) {
                if (iterator < 10) {
                    respawn(p, iterator);
                } else {
                    p.setGameMode(GameMode.SPECTATOR);
                    p.sendMessage(ChatColor.RED + "Uh oh, we couldn't find a suitable spawn point for you, please be patient as we try again (you can spectate for now).");

                    p.teleport(randLoc);
                    Bukkit.getScheduler().runTaskLater(this, () -> respawn(p), 5 * 20);
                }
                return;
            }
        }

        p.teleport(randLoc);

        p.getInventory().clear();
        p.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        p.setGliding(true);
    }

    private Location getRandLoc(World world) {
        return getRandLoc(bound2.getBlockY(), world);
    }

    private Location getRandLoc(int yMin, World world) {
        int xMax = bound1.getBlockX();
        int yMax = bound1.getBlockY();
        int zMax = bound1.getBlockZ();

        int xMin = bound2.getBlockX();
        int zMin = bound2.getBlockZ();

        return new Location(world, getRandInt(xMin, xMax), getRandInt(yMin, yMax), getRandInt(zMin, zMax));
    }

    private int getRandInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private void setupWorld() {
        for (Entity e : flyFight.getWorld().getEntities()) {
            if (e.getType() == EntityType.ARMOR_STAND) {
                switch (e.getCustomName()) { //todo: Check if correct, might be e.getName()
                    case "BOUND1":
                        bound1 = e.getLocation(); //todo: Check if location is armor stand head or foot (we want foot)
                        e.remove();
                        break;
                    case "BOUND2":
                        bound2 = e.getLocation();
                        e.remove();
                        break;
                }
            }
        }
    }

    public Player getSpecialPlayer() {
        return specialPlayer;
    }

    public void setSpecialPlayer(Player specialPlayer) {
        this.specialPlayer = specialPlayer;
    }

    public enum Special {
        BOOST("Boost", new ItemStack(Material.GOLD_NUGGET)),
        INVINCIBILITY("Invincibility", new ItemStack(Material.NETHER_STAR)),
        BOOST_REFILL("Boost Refill", new ItemStack(Material.EXP_BOTTLE));

        private String itemName;
        private ItemStack item;

        Special(String itemName, ItemStack item) {
            this.itemName = itemName;
            this.item = item;
        }

        public static Special getSpecialFromItemName(String itemName) {
            for (Special special : Special.values()) {
                if (special.getItemName().equals(itemName)) {
                    return special;
                }
            }
            return null;
        }

        public String getItemName() {
            return itemName;
        }

        public ItemStack getItem() {
            ItemStack newItem = item;
            ItemMeta meta = newItem.getItemMeta();
            meta.setDisplayName(itemName);
            newItem.setItemMeta(meta);

            return newItem;
        }

        public void runSpecial(Player p) {
            Bukkit.getScheduler().runTask(FlyFight.getInstance(), () -> {
                switch (this) {
                    case INVINCIBILITY:
                        p.setGlowing(true);
                        p.setInvulnerable(true);
                        Bukkit.getScheduler().runTaskLater(FlyFight.instance, () -> {
                            p.setGlowing(false);
                            p.setInvulnerable(false);
                        }, 10 * 20);
                        break;
                    case BOOST_REFILL:
                        p.setExp(99);
                        break;
                }
            });
        }
    }
}
