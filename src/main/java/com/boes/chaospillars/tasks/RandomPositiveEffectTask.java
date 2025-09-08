package com.boes.chaospillars.tasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class RandomPositiveEffectTask extends BukkitRunnable {
    private final Set<UUID> activePlayers;
    private final Random random = new Random();

    public RandomPositiveEffectTask(Set<UUID> activePlayers) {
        this.activePlayers = activePlayers;
    }

    @Override
    public void run() {
        List<PotionEffectType> effects = List.of(
                PotionEffectType.SPEED,
                PotionEffectType.HASTE,
                PotionEffectType.STRENGTH,
                PotionEffectType.REGENERATION,
                PotionEffectType.RESISTANCE,
                PotionEffectType.FIRE_RESISTANCE,
                PotionEffectType.HEALTH_BOOST,
                PotionEffectType.ABSORPTION,
                PotionEffectType.JUMP_BOOST,
                PotionEffectType.NIGHT_VISION,
                PotionEffectType.SATURATION,
                PotionEffectType.CONDUIT_POWER,
                PotionEffectType.INVISIBILITY,
                PotionEffectType.LUCK,
                PotionEffectType.WATER_BREATHING,
                PotionEffectType.SLOW_FALLING
        );

        for (UUID uuid : activePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !player.isDead()) {
                PotionEffectType chosen = effects.get(random.nextInt(effects.size()));
                player.addPotionEffect(new PotionEffect(chosen, 20 * 20, 0, false, true));
                player.sendMessage(ChatColor.AQUA + "✨ You received " + chosen.getName() + "!");
            }
        }
    }
}
