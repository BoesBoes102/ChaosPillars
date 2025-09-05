package com.boes.chaospillars.tasks;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class KillAllEntitiesTask {
    private final World gameWorld;

    public KillAllEntitiesTask(World gameWorld) {
        this.gameWorld = gameWorld;
    }

    public void run() {
        if (gameWorld == null) return;
        for (Entity e : gameWorld.getEntities()) {
            if (e instanceof Player) continue;
            e.remove();
        }
    }
}
