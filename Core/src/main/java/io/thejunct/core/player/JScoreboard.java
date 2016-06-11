/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.core.player;

import io.thejunct.core.Core;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

/**
 * Created by david on 6/01.
 */
public class JScoreboard {

    public JScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective sidebar = board.registerNewObjective("sidebar", "dummy");
        Objective list = board.registerNewObjective("list", "dummy");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(Core.getInstance(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                JPlayer jP = JPlayer.get(p.getUniqueId());

                sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
                sidebar.setDisplayName(ChatColor.GOLD + ChatColor.BOLD.toString() + "  The Junction  ");

                int row = 0;
                Score score;

                String guild = "No Guild";
                if (jP.getGuild() != null) {
                    guild = jP.getGuild().getSetting(JGuild.Settings.NAME);
                    int online = 0;
                    for (Player member : Bukkit.getOnlinePlayers()) {
                        if (jP.getGuild().getMemberList().contains(member.getUniqueId())) {
                            online++;
                        }
                    }
                    guild += " (" + online + " Online)";
                }

                score = sidebar.getScore(guild);
                score.setScore(++row);
                score = sidebar.getScore(ChatColor.GREEN + "Guild");
                score.setScore(++row);
                score = sidebar.getScore("");
                score.setScore(++row);

                score = sidebar.getScore("");//todo online time
                score.setScore(++row);
                score = sidebar.getScore(ChatColor.BLUE + "Play Time");
                score.setScore(++row);
                score = sidebar.getScore(" ");
                score.setScore(++row);

                score = sidebar.getScore(jP.getTokens() + "");
                score.setScore(++row);
                score = sidebar.getScore(ChatColor.GOLD + "Tokens");
                score.setScore(++row);
                score = sidebar.getScore("  ");
                score.setScore(++row);

                String groups = null;
                for (String group : jP.getGroups()) {
                    if (groups != null) {
                        groups += ", ";
                    }
                    groups += group;
                }

                score = sidebar.getScore(groups == null ? "No Rank" : groups);
                score.setScore(++row);
                score = sidebar.getScore(ChatColor.GREEN + "Rank");
                score.setScore(++row);
                score = sidebar.getScore("   ");
                score.setScore(++row);

                score = sidebar.getScore(Bukkit.getServerName());
                score.setScore(++row);
                score = sidebar.getScore(ChatColor.YELLOW + "Server");
                score.setScore(++row);
                score = sidebar.getScore("    ");
                score.setScore(++row);

                score = sidebar.getScore(jP.getNick());
                score.setScore(++row);
                score = sidebar.getScore(ChatColor.DARK_BLUE + "Name");
                score.setScore(++row);
                score = sidebar.getScore("     ");
                score.setScore(++row);


                list.setDisplaySlot(DisplaySlot.PLAYER_LIST);
                score = list.getScore(p.getDisplayName());
                score.setScore(p.getLevel());


                p.setScoreboard(board);
            }
        }, 0, 100);
    }
}
