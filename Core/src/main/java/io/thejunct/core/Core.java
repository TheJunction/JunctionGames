/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.core;

import io.thejunct.core.game.Minigame;
import io.thejunct.core.player.JGuild;
import io.thejunct.core.player.JPlayer;
import io.thejunct.core.player.JScoreboard;
import io.thejunct.core.player.Listeners;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Statement;

/**
 * Created by david on 4/25.
 *
 * @author david
 */
public class Core extends JavaPlugin {

    public static String PREFIX = ChatColor.GRAY + "[" + ChatColor.BLUE + "The " + ChatColor.RED + "Junction" + ChatColor.GRAY + "] ";

    private static Core instance;
    private static Database database;

    private Minigame minigame;

    public static Core getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        database = new Database("thejunct.io", "TheJunction", "david", "DavidShen", 3306);
        Bukkit.getScheduler().cancelTasks(this);
        JPlayer.setup();
        JGuild.setup();
        new JScoreboard();
        Bukkit.getPluginManager().registerEvents(new Listeners(), this);
    }

    public Statement getStatement() {
        return database.getStatement();
    }

    public Minigame getMinigame() {
        return minigame;
    }
}
