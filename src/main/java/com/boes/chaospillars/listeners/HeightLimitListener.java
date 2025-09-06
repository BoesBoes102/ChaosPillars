package com.boes.chaospillars.listeners;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public record HeightLimitListener(World gameWorld) implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (!block.getWorld().equals(gameWorld)) return;

        if (block.getY() >= 2) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "HEIGHT LIMIT!");
        }
    }
}
