package com.boes.chaospillars.listeners;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public record WorldListener(ChaosPillars plugin) implements Listener {

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();

        if (!player.getWorld().equals(plugin.getGameWorld())) return;
        if (plugin.getGameState() != GameState.RUNNING) return;

        World.Environment env = event.getTo().getWorld().getEnvironment();
        if (env == World.Environment.NETHER || env == World.Environment.THE_END) {
            event.setCancelled(true);
            player.sendMessage("Nether and End are disabled in the game world!");
        }
    }
}
