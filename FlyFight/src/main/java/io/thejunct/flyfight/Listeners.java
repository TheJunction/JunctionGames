/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.flyfight;

import io.thejunct.core.game.StartGameEvent;
import io.thejunct.core.player.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Created by david on 7/16.
 */
public class Listeners implements Listener {

    @EventHandler
    public void onStart(StartGameEvent e) {
        Bukkit.getOnlinePlayers().forEach(FlyFight.getInstance()::respawn);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getEntity().spigot().respawn();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        FlyFight.getInstance().respawn(e.getPlayer());
    }

    @EventHandler
    public void onStartGame(StartGameEvent e) {
        for (Player player : FlyFight.getInstance().getServer().getOnlinePlayers()) {
            player.setExp(99);
        }
        Bukkit.getScheduler().scheduleSyncRepeatingTask(FlyFight.getInstance(), () -> FlyFight.getInstance().getServer().getOnlinePlayers().stream().filter(LivingEntity::isGliding).forEach(p -> p.setExp(p.getExp() - ((p.getInventory().getHeldItemSlot() + 1) * 0.1F))), 2, 2);
    }

    @EventHandler
    public void onPickupItem(PlayerPickupItemEvent e) {
        if (e.getPlayer().isGliding() && FlyFight.getInstance().isBoost(e.getItem().getItemStack())) {
            e.setCancelled(true);
            e.getItem().remove();
            e.getPlayer().setExp(e.getPlayer().getExp() + 0.1F);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (FlyFight.getInstance().isSpecial(e.getItem())) {
            FlyFight.Special special = FlyFight.getInstance().getSpecial(e.getItem());

            special.runSpecial(e.getPlayer());
            new Title(e.getPlayer(), ChatColor.GREEN + "You activated " + special.getItemName().toLowerCase() + ChatColor.GREEN + ".", null, 5, 5 * 20, 5); //todo: Check duration, if ticks or seconds
        }
    }
}
