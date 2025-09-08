package com.boes.chaospillars.tasks;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ItemTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final Set<UUID> activePlayers;
    private final int itemGiveIntervalTicks;
    private final Random random = new Random();
    private BukkitRunnable runnable;

    private final List<Material> validItems = Arrays.stream(Material.values())
            .filter(mat -> mat.isItem() && mat != Material.AIR && mat != Material.CAVE_AIR && mat != Material.VOID_AIR)
            .toList();

    public ItemTask(JavaPlugin plugin, Set<UUID> activePlayers, int itemGiveIntervalTicks) {
        this.plugin = plugin;
        this.activePlayers = activePlayers;
        this.itemGiveIntervalTicks = itemGiveIntervalTicks;
    }

    public void start() {
        stop();

        runnable = new BukkitRunnable() {
            int secondsLeft = Math.max(1, itemGiveIntervalTicks / 20);

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    for (UUID uuid : new HashSet<>(activePlayers)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player == null || !player.isOnline() || player.isDead()) continue;

                        Material mat = validItems.get(random.nextInt(validItems.size()));
                        player.getInventory().addItem(new ItemStack(mat));
                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.GREEN + "You received an item!")
                        );
                    }
                    secondsLeft = Math.max(1, itemGiveIntervalTicks / 20);
                } else {
                    for (UUID uuid : new HashSet<>(activePlayers)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player == null || !player.isOnline()) continue;

                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.YELLOW + "Next item in " + secondsLeft + "s")
                        );
                    }
                    secondsLeft--;
                }
            }
        };

        runnable.runTaskTimer(plugin, 20L, 20L);
    }

    public void stop() {
        if (runnable != null) {
            runnable.cancel();
            runnable = null;
        }
    }

    @Override
    public void run() {

    }
}
