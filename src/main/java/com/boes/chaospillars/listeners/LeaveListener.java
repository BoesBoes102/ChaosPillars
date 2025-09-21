package com.boes.chaospillars.listeners;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public record LeaveListener(ChaosPillars plugin) implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (!player.getWorld().equals(plugin.getGameWorld())) return;

        UUID uuid = player.getUniqueId();

        if (!plugin.getActivePlayers().contains(uuid)) return;

        plugin.getQuitters().add(uuid);

        player.setHealth(0.0);
    }
}
