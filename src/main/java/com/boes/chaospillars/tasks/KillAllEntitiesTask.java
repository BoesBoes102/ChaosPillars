package com.boes.chaospillars.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class KillAllEntitiesTask {

    private final World gameWorld;

    public KillAllEntitiesTask(World gameWorld) {
        this.gameWorld = gameWorld;
    }

    public void run() {
        if (gameWorld == null || !gameWorld.equals(Bukkit.getWorld(gameWorld.getName()))) {
            return;
        }

        WorldBorder border = gameWorld.getWorldBorder();
        double borderSize = border.getSize();
        Location center = border.getCenter();

        for (Entity e : gameWorld.getEntities()) {
            if (e instanceof Player) continue;

            Location entityLoc = e.getLocation();
            double distanceFromCenter = entityLoc.distance(center);

            if (distanceFromCenter <= borderSize / 2) {
                e.remove();
            }
        }
    }
}
