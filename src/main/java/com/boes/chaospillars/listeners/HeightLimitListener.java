package com.boes.chaospillars.listeners;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public record HeightLimitListener(ChaosPillars plugin) implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.getBlock().getWorld().equals(plugin.getGameWorld())) return;

        if (plugin.getGameState() != GameState.RUNNING) return;
        int maxHeight = 2;
        if (event.getBlock().getY() >= maxHeight) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "HEIGHT LIMIT!");
        }
    }
}
