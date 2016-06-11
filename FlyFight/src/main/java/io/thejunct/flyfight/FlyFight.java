/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.flyfight;

import io.thejunct.core.game.Minigame;
import io.thejunct.core.game.StartGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by david on 4/25.
 */
public class FlyFight extends JavaPlugin implements Listener {

    public Minigame flyFight;

    public Set<Location> spawnList;
    public Random rand = new Random();

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("Core") == null) {
            getLogger().severe("Core plugin not found. Disabling FlyFight...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        flyFight = new Minigame("FlyFight", 4, 16, false);

        Bukkit.getPluginManager().registerEvents(this, this);

        spawnList = new HashSet<>();

        setupWorld();
    }

    @EventHandler
    public void onStart(StartGameEvent e) {
        Bukkit.getOnlinePlayers().forEach(this::respawn);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getEntity().spigot().respawn();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        respawn(e.getPlayer());
    }

    private void respawn(Player p) {

        int size = spawnList.size();
        Location teleport = spawnList.toArray(new Location[size])[rand.nextInt(size)];
        p.teleport(teleport);

        p.getInventory().clear();
        p.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        p.setGliding(true);
    }

    public void setupWorld() {
        flyFight.getWorld().getEntities().stream().filter(e -> e.getType() == EntityType.ARMOR_STAND).forEach(e -> {
            spawnList.add(e.getLocation());
            e.remove();
        });
    }

}
