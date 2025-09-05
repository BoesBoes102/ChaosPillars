package com.boes.chaospillars.listeners;


import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.entity.Player;

public class WorldListener implements Listener {

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        if (event.getTo() != null) {
            World.Environment env = event.getTo().getWorld().getEnvironment();
            if (env == World.Environment.NETHER || env == World.Environment.THE_END) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                player.sendMessage("Nether and End are disabled!");
            }
        }
    }
}