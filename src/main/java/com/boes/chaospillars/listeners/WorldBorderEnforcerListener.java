package com.boes.chaospillars.listeners;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public record WorldBorderEnforcerListener(ChaosPillars plugin, int maxRadius) implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!player.getWorld().equals(plugin.getGameWorld())) return;
        if (plugin.getGameState() != GameState.RUNNING) return;

        Location loc = player.getLocation();
        double distance = Math.sqrt(loc.getX() * loc.getX() + loc.getZ() * loc.getZ());

        if (distance > maxRadius) {
            Location spawn = plugin.getGameWorld().getSpawnLocation().clone().add(0.5, 0, 0.5);
            player.teleport(spawn);
            player.sendMessage(ChatColor.RED + "You cannot leave the arena!");
        }
    }
}
