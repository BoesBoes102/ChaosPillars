package com.boes.chaospillars.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.World;

import java.util.Set;
import java.util.UUID;

public record LeaveListener(Set<UUID> activePlayers, Set<UUID> quitters, World gameWorld) implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (!player.getWorld().equals(gameWorld)) return;

        UUID uuid = player.getUniqueId();

        if (!activePlayers.contains(uuid)) return;

        quitters.add(uuid);

        player.setHealth(0.0);
    }
}
