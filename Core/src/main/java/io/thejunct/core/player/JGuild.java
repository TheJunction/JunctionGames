/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.core.player;

import io.thejunct.core.Core;
import org.bukkit.Bukkit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by david on 4/28.
 */
public class JGuild {

    private static HashMap<UUID, JGuild> JGuilds = null;

    private UUID uuid;
    private Map<UUID, Role> members;
    private Map<Settings, String> settings;

    public JGuild(UUID uuid) {
        this.uuid = uuid;
        members = new HashMap<>();
        settings = new HashMap<>();
        Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance(), () -> {
            try {
                ResultSet result = Core.getInstance().getStatement().executeQuery("SELECT * FROM guilds WHERE uuid = '" + uuid + "';");
                result.next();

                members.put(UUID.fromString(result.getString("owners")), Role.OWNER);
                for (String officer : result.getString("officers").split("\n")) {
                    members.put(UUID.fromString(officer), Role.OFFICER);
                }
                for (String member : result.getString("members").split("\n")) {
                    members.put(UUID.fromString(member), Role.MEMBER);
                }
                
                for (Settings setting : Settings.values()) {
                    settings.put(setting, result.getString(setting.toString()));
                }

                JGuilds.put(uuid, this);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public JGuild(String name, UUID founder) {
        uuid = genUUID();
        
        for (Settings setting : Settings.values()) {
            settings.put(setting, setting.getDef());
        }
        settings.put(Settings.NAME, name);
        
        members.put(founder, Role.OWNER);
        
        JGuilds.put(uuid, this);
    }

    public static void setup() {
        JGuilds = new HashMap<>();

        try {
            ResultSet result = Core.getInstance().getStatement().executeQuery("SELECT * FROM guilds;");
            while (result.next()) {
                new JGuild(UUID.fromString(result.getString("uuid")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<UUID, JGuild> getJGuilds() {
        return JGuilds;
    }

    public static void setJGuilds(HashMap<UUID, JGuild> JGuilds) {
        JGuild.JGuilds = JGuilds;
    }

    public static JGuild get(UUID uuid) {
        return JGuilds.get(uuid);
    }

    private static UUID genUUID() {
        UUID uuid = UUID.randomUUID();
        try {
            ResultSet result = Core.getInstance().getStatement().executeQuery("SELECT * FROM guilds WHERE uuid = '" + uuid + "';");
            if (result.next()) {
                return genUUID();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return uuid;
    }

    public void save() {
        Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance(), () -> {
            try {
                String ownersS = "";
                String officersS = "";
                String membersS = "";
                for (UUID member : members.keySet()) {
                    switch (members.get(member)) {
                        case OWNER:
                            ownersS = ownersS.equals("") ? member.toString() : ownersS + "\n" + member.toString();
                            break;
                        case OFFICER:
                            officersS = officersS.equals("") ? member.toString() : officersS + "\n" + member.toString();
                            break;
                        case MEMBER:
                            membersS = membersS.equals("") ? member.toString() : membersS + "\n" + member.toString();
                            break;
                    }
                }
                Core.getInstance().getStatement().executeUpdate("INSERT INTO guilds (uuid, owners, officers, members, name, description, joining, synchronized, invite) VALUES ('" + uuid.toString() + "', '" + ownersS + "', '" + officersS + "', '" + membersS + "', '" + getSetting(Settings.NAME) + "', '" + getSetting(Settings.DESCRIPTION) + "', '" + getSetting(Settings.JOINING) + "', '" + getSetting(Settings.SYNCHRONIZED) + "', '" + getSetting(Settings.INVITE) + "');");
                JGuilds.remove(uuid);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getSetting(Settings key) {
        return settings.get(key);
    }

    public void setSetting(Settings key, String value) {
        settings.put(key, value);
    }

    public Set<UUID> getMemberList() {
        return members.keySet();
    }

    public Role getMemberRole(UUID uuid) {
        return members.get(uuid);
    }

    public void setMember(UUID member, Role role) {
        members.put(member, role);
    }

    public void removeMember(UUID member) {
        members.remove(member);
    }

    public enum Role {
        MEMBER,
        OFFICER,
        OWNER
    }

    public enum Settings {
        NAME("Name", "Guild name.", "N/A"),
        DESCRIPTION("Description", "Guild description.", "This is a default description."),
        JOINING("Joining", "Invite: Join by invite only. Open: Anyone can join.", "Invite", "Open"),
        SYNCHRONIZED("Synchronized", "Join games, and lobbies automatically as a group.", "True", "False"),
        INVITE("Invite", "Who can invite players to the guild (only if joining is invite-only).", "Officer", "Owner", "Member");

        private String name, desc, def;
        private List<String> values;

        Settings(String name, String desc, String def, String... values) {
            this.name = name;
            this.desc = desc;
            this.def = def;
            this.values = new ArrayList<>();
            this.values.add(def);
            Collections.addAll(this.values, values);
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

        public String getDef() {
            return def;
        }

        public List<String> getValues() {
            return values;
        }
    }
}
