/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.core.game;

import io.thejunct.core.player.JPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by david on 6/02.
 */
public class EndGameEvent extends Event {
    private Minigame minigame;

    public EndGameEvent(Minigame minigame) {
        this.minigame = minigame;
        for (JPlayer p : JPlayer.getJPlayers().values()) {
            p.sendServer("Lobby");
        }
        minigame.setRunning(false);
        minigame.reset();
    }

    @Override
    public HandlerList getHandlers() {
        return null;
    }
}
