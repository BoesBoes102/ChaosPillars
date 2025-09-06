package com.boes.chaospillars.listeners;

import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;

public record DamageListener(Map<UUID, UUID> lastDamager, World gameWorld) implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player damaged)) return;


        if (!damaged.getWorld().equals(gameWorld)) return;

        Entity damager = event.getDamager();


        if (damager instanceof Player playerDamager) {
            lastDamager.put(damaged.getUniqueId(), playerDamager.getUniqueId());
            return;
        }


        if (damager instanceof Snowball || damager instanceof Egg) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player shooter) {
                lastDamager.put(damaged.getUniqueId(), shooter.getUniqueId());


                Vector direction = damaged.getLocation().toVector()
                        .subtract(shooter.getLocation().toVector())
                        .normalize();
                Vector knockback = direction.multiply(0.8).setY(0.5);

                damaged.setVelocity(knockback);
            }
        }
    }
}
