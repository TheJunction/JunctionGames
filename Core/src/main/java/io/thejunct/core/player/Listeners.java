/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.core.player;

import io.thejunct.core.Core;
import io.thejunct.core.game.StartGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Created by david on 4/25.
 */
public class Listeners implements Listener {

    private BukkitRunnable start = new BukkitRunnable() {
        @Override
        public void run() {
            Bukkit.getServer().setWhitelist(true);
            Bukkit.getPluginManager().callEvent(new StartGameEvent(Core.getInstance().getMinigame()));

        }
    };
    private boolean isStarting = false;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        int wait = 60 * 20;

        if (Bukkit.getOnlinePlayers().size() >= Core.getInstance().getMinigame().getMax()) {
            wait = 5 * 20;
        }

        if (Bukkit.getOnlinePlayers().size() >= Core.getInstance().getMinigame().getMin()) {
            start.runTaskLater(Core.getInstance(), wait);
            isStarting = true;
        }

        new JPlayer(e.getPlayer());

        e.getPlayer().teleport(new Location(Bukkit.getWorld("world"), 0, 0, 0, 0, 0));

        /*
        new Title(e.getPlayer(), ChatColor.GREEN + "Welcome, " + e.getPlayer().getName() + ",", ChatColor.GREEN + "to...", 5, 20, 5);
        new Title(e.getPlayer(), ChatColor.GOLD + ChatColor.BOLD.toString() + "The Junction!", null, 5, 20, 5);
        */ //todo Move to Hub plugin
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        if (isStarting && Bukkit.getOnlinePlayers().size() < Core.getInstance().getMinigame().getMin()) {
            start.cancel();
        }

        JPlayer.get(e.getPlayer().getUniqueId()).save();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        JPlayer jP = JPlayer.get(p.getUniqueId());

        ChatColor suffix = ChatColor.GRAY;
        if (jP.getGroups().contains("staff")) {
            suffix = ChatColor.BLUE;
        } else if (jP.getGroups().contains("donor")) {
            suffix = ChatColor.WHITE;
        }

        e.setFormat("%1$s" + ChatColor.GRAY + "> " + suffix + "%2$s");
    }
}
