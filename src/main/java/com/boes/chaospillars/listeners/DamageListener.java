package com.boes.chaospillars.listeners;

import com.boes.chaospillars.ChaosPillars;
import com.boes.chaospillars.enums.GameState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public record DamageListener(ChaosPillars plugin) implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player damaged)) return;

        if (!damaged.getWorld().equals(plugin.getGameWorld())) return;
        if (plugin.getGameState() != GameState.RUNNING) return;

        Entity damager = event.getDamager();

        if (damager instanceof Player playerDamager) {
            plugin.getLastDamager().put(damaged.getUniqueId(), playerDamager.getUniqueId());
            return;
        }

        if (damager instanceof Snowball || damager instanceof Egg) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player shooter) {
                plugin.getLastDamager().put(damaged.getUniqueId(), shooter.getUniqueId());

                Vector direction = damaged.getLocation().toVector()
                        .subtract(shooter.getLocation().toVector())
                        .normalize();
                Vector knockback = direction.multiply(0.8).setY(0.5);

                damaged.setVelocity(knockback);
            }
        }
    }
}
