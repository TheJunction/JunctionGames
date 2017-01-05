/*
 * Copyright (c) 2017 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.core.player;

import net.minecraft.server.v1_11_R1.IChatBaseComponent;
import net.minecraft.server.v1_11_R1.PacketPlayOutTitle;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by david on 6/02.
 */
public class Title {

    public Title(Player p, String titleMsg, String subMsg, int fadeIn, int dur, int fadeOut) {
        if (titleMsg != null) {
            IChatBaseComponent chatTitle = IChatBaseComponent.ChatSerializer.a(getJSON(titleMsg));
            PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, chatTitle);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(title);
        }

        if (subMsg != null) {
            IChatBaseComponent chatSub = IChatBaseComponent.ChatSerializer.a(getJSON(subMsg));
            PacketPlayOutTitle sub = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, chatSub);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(sub);
        }

        if (titleMsg != null || subMsg != null) {
            PacketPlayOutTitle length = new PacketPlayOutTitle(fadeIn, dur, fadeOut);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(length);
        }
    }

    /**
     * @param title Input title
     * @return String
     * @author werter318
     */
    private String getJSON(String title) {
        char colorChar = ChatColor.COLOR_CHAR;

        String template = "{text:\"TEXT\",color:COLOR,bold:BOLD,underlined:UNDERLINED,italic:ITALIC,strikethrough:STRIKETHROUGH,obfuscated:OBFUSCATED,extra:[EXTRA]}";
        String json = "";

        List<String> parts = new ArrayList<>();

        int first;
        int last = 0;

        while ((first = title.indexOf(colorChar, last)) != -1) {
            int offset = 2;
            while ((last = title.indexOf(colorChar, first + offset)) - 2 == first) {
                offset += 2;
            }

            if (last == -1) {
                parts.add(title.substring(first));
                break;
            } else {
                parts.add(title.substring(first, last));
            }
        }

        if (parts.isEmpty()) {
            parts.add(title);
        }

        Pattern colorFinder = Pattern.compile("(" + colorChar + "([a-f0-9]))");
        for (String part : parts) {
            json = (json.isEmpty() ? template : json.replace("EXTRA", template));

            Matcher matcher = colorFinder.matcher(part);
            ChatColor color = (matcher.find() ? ChatColor.getByChar(matcher.group().charAt(1)) : ChatColor.WHITE);

            json = json.replace("COLOR", color.name().toLowerCase());
            json = json.replace("BOLD", String.valueOf(part.contains(ChatColor.BOLD.toString())));
            json = json.replace("ITALIC", String.valueOf(part.contains(ChatColor.ITALIC.toString())));
            json = json.replace("UNDERLINED", String.valueOf(part.contains(ChatColor.UNDERLINE.toString())));
            json = json.replace("STRIKETHROUGH", String.valueOf(part.contains(ChatColor.STRIKETHROUGH.toString())));
            json = json.replace("OBFUSCATED", String.valueOf(part.contains(ChatColor.MAGIC.toString())));

            json = json.replace("TEXT", part.replaceAll("(" + colorChar + "([a-z0-9]))", ""));
        }

        json = json.replace(",extra:[EXTRA]", "");

        return json;
    }
}
