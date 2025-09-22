package com.boes.chaospillars.listeners;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public record PlayerRespawnListener(ChaosPillars plugin) implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (plugin.getGameState() != GameState.RUNNING) return;

        event.setRespawnLocation(plugin.getGameWorld().getSpawnLocation());

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            if (!player.getWorld().equals(plugin.getGameWorld())) {
                plugin.getLogger().warning("Player " + player.getName() +
                        " spawned in wrong world (" + player.getWorld().getName() + "), teleporting to game world.");
                player.teleport(plugin.getGameWorld().getSpawnLocation());
            }

            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);

            player.sendMessage("§7You have been eliminated and are now spectating the game.");
        }, 2L);
    }
}
