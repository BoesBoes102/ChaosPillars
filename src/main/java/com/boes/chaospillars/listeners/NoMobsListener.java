package com.boes.chaospillars.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public record NoMobsListener(World gameWorld) implements Listener {

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {

        if (!event.getEntity().getWorld().equals(gameWorld)) return;

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            event.setCancelled(true);
        }
    }
}
