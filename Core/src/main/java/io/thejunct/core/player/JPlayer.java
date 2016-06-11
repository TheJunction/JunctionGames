/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.core.player;

import io.thejunct.core.Core;
import io.thejunct.core.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by david on 4/25.
 */
public class JPlayer {

    private static final String GROUP_PREFIX = "group.";
    private static HashMap<UUID, JPlayer> JPlayers = null;
    private Player player;
    private UUID uuid;
    private JGuild guild;
    private Set<String> groups;
    private String name, nick, prefix;
    private int tokens;
    private int experience;

    public JPlayer(Player p) {
        player = p;
        uuid = p.getUniqueId();
        name = p.getName();
        groups = new HashSet<>();
        prefix = "";
        Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance(), () -> {
            try {
                for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
                    if (!pai.getPermission().startsWith(GROUP_PREFIX) || !pai.getValue())
                        continue;
                    String groupName = pai.getPermission().substring(GROUP_PREFIX.length());
                    groups.add(groupName);
                    ResultSet result = Core.getInstance().getStatement().executeQuery("SELECT * FROM settings WHERE id = '" + groupName + "_PREFIX" + "';");
                    boolean exists = result.next();
                    if (exists) {
                        prefix += ChatColor.translateAlternateColorCodes('&', result.getString("value")) + " ";
                    }
                }
                ResultSet result = Core.getInstance().getStatement().executeQuery("SELECT * FROM players WHERE uuid = '" + uuid + "';");
                boolean returning = result.next();
                guild = null;
                if (returning) {
                    if (!result.getString("guild").isEmpty()) {
                        guild = JGuild.get(UUID.fromString(result.getString("guild")));
                    }
                    nick = result.getString("nick");
                    tokens = result.getInt("tokens");
                    experience = result.getInt("experience");
                } else {
                    nick = "";
                    tokens = 0;
                    experience = 0;
                }
                p.setTotalExperience(experience);

                setNick(p, nick);

                JPlayers.put(uuid, this);
            } catch (SQLException e) {
                e.printStackTrace();
                p.sendMessage(Core.PREFIX + ChatColor.RED + "Uh oh, we were unable to retrieve your info! Sorry, but we have to send you back to Hub D:\nIf you have questions, please contact any of our staff. They'll be happy to help you!");
                this.sendServer("Lobby");
            }
        });
    }

    public static void setup() {
        JPlayers = new HashMap<>();
    }

    public static HashMap<UUID, JPlayer> getJPlayers() {
        return JPlayers;
    }

    public static void setJPlayers(HashMap<UUID, JPlayer> JPlayers) {
        JPlayer.JPlayers = JPlayers;
    }

    public static JPlayer get(UUID uuid) {
        return JPlayers.get(uuid);
    }

    public void save() {
        Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance(), () -> {
            try {
                Core.getInstance().getStatement().executeUpdate("INSERT INTO players (uuid, name, nick, tokens, experience) VALUES ('" + uuid.toString() + "', '" + name + "', '" + nick + "', " + tokens + ", " + experience + ");");
                JPlayers.remove(uuid);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public JGuild getGuild() {
        return guild; //Will be null if not in a group
    }

    public void setGuild(JGuild guild) {
        this.guild = guild;
        JPlayers.put(uuid, this);
    }

    public Set<String> getGroups() {
        return groups;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNick() {
        return nick; //Will be empty if no nick
    }

    public void setNick(Player p, String nick) {
        this.nick = nick;

        String dName = name;
        if (p.hasPermission(Permissions.NICK)) {
            dName = nick;
        }
        p.setDisplayName(prefix + dName);


        if (!p.hasPermission(Permissions.DEEP_NICK)) {
            dName = name;
        }
        p.setCustomName(dName);
        p.setPlayerListName(dName);

        JPlayers.put(uuid, this);
    }

    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
        JPlayers.put(uuid, this);
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
        player.setTotalExperience(experience);
        JPlayers.put(uuid, this);
    }

    public synchronized void sendServer(String server) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (Exception e) {
            e.printStackTrace();
        }
        player.sendPluginMessage(Core.getInstance(), "BungeeCord", b.toByteArray());
    }
}
