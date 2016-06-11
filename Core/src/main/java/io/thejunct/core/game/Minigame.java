/*
 * Copyright (c) 2016 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.core.game;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;

/**
 * Created by david on 4/25.
 */
public class Minigame {

    private final String name;
    private final int min, max;
    private final boolean resetWorld;
    private boolean running;

    public Minigame(String name, int min, int max, boolean resetWorld) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.resetWorld = resetWorld;
        running = false;
    }

    public void reset() {
        if (resetWorld) {
            File server = new File(".").getAbsoluteFile();
            File world = new File(server, name);
            world.delete();
            File template = new File(server.getParentFile().getParent() + "/Minigames/Worlds/" + name);
            try {
                FileUtils.copyDirectory(template, world);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getName() {
        return name;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean hasStarted) {
        this.running = hasStarted;
    }

    public World getWorld() {
        return Bukkit.getWorld(name);
    }
}
