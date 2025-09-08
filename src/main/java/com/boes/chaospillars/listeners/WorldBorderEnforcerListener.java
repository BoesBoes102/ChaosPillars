package com.boes.chaospillars.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.ChatColor;

public record WorldBorderEnforcerListener(World gameWorld, int maxRadius) implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!player.getWorld().equals(gameWorld)) return;

        Location loc = player.getLocation();
        double distance = Math.sqrt(loc.getX() * loc.getX() + loc.getZ() * loc.getZ());

        if (distance > maxRadius) {
            Location spawn = gameWorld.getSpawnLocation().clone().add(0.5, 0, 0.5);
            player.teleport(spawn);
            player.sendMessage(ChatColor.RED + "You cannot leave the arena!");
        }
    }
}
