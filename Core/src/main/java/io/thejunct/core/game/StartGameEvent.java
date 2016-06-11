/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.core.game;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by david on 6/02.
 */
public class StartGameEvent extends Event {
    private Minigame minigame;

    public StartGameEvent(Minigame minigame) {
        this.minigame = minigame;
        minigame.setRunning(true);
    }

    public Minigame getMinigame() {
        return minigame;
    }

    @Override
    public HandlerList getHandlers() {
        return null;
    }
}
