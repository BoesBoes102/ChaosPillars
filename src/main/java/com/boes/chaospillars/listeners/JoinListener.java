package com.boes.chaospillars.listeners;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public record JoinListener(ChaosPillars plugin, World gameWorld) implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(gameWorld.getSpawnLocation());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Do /chaos start to start a game!");

            if (plugin.getGameState() == GameState.RUNNING) {
                plugin.updateGameScoreboard();
            } else {
                plugin.updateIdleScoreboard(player);
            }
        });
    }
}
