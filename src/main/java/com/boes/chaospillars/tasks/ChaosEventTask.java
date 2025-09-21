package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class ChaosEventTask {

    private final ChaosPillars plugin;
    private final Random random = new Random();

    public ChaosEventTask(ChaosPillars plugin, World gameWorld) {
        if (plugin == null || plugin.getGameWorld() == null) {
            throw new IllegalArgumentException("Plugin and gameWorld cannot be null");
        }
        this.plugin = plugin;
    }

    private World getGameWorld() {
        return plugin.getGameWorld();
    }

    private List<Player> getActivePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : plugin.getActivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }

    public void triggerRandomEvent() {
        int index = random.nextInt(10);
        String eventName;

        switch (index) {
            case 0 -> { changeWeather(); eventName = "Weather Change"; }
            case 1 -> { randomTimeChange(); eventName = "Time Shift"; }
            case 2 -> { swapPositions(); eventName = "Swap Positions"; }
            case 3 -> { swapInventories(); eventName = "Swap Inventories"; }
            case 4 -> { applyGlow(); eventName = "No Hiding"; }
            case 5 -> { dropAnvilsAbovePlayers(); eventName = "Anvil Drop"; }
            case 6 -> { randomNegativeEffect(); eventName = "Negative Potion Effect"; }
            case 7 -> { giveThrowable(); eventName = "Throwable Items"; }
            case 8 -> { swapHealthFoodSaturation(); eventName = "Swap Health"; }
            case 9 -> { makeEveryoneJump(); eventName = "Simon Says Jump"; }
            default -> eventName = "Unknown Event";
        }

        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[Chaos] " + ChatColor.YELLOW + eventName + ChatColor.LIGHT_PURPLE + " event has occurred!");
    }

    public void changeWeather() {
        World world = getGameWorld();
        if (world == null) return;
        world.setStorm(!world.hasStorm());
    }

    public void randomTimeChange() {
        World world = getGameWorld();
        if (world == null) return;
        world.setTime(random.nextInt(24000));
    }

    public void swapPositions() {
        List<Player> players = getActivePlayers();
        if (players.size() < 2) return;

        List<Location> locations = new ArrayList<>();
        for (Player p : players) locations.add(p.getLocation());

        List<Player> shuffledPlayers = new ArrayList<>(players);
        do { Collections.shuffle(shuffledPlayers); } while (noDoubles(players, shuffledPlayers));

        for (int i = 0; i < players.size(); i++) {
            Location loc = locations.get(players.indexOf(shuffledPlayers.get(i)));
            players.get(i).teleport(loc);
        }
    }

    public void swapInventories() {
        List<Player> players = getActivePlayers();
        if (players.size() < 2) return;

        List<ItemStack[]> contents = new ArrayList<>();
        List<ItemStack[]> armor = new ArrayList<>();
        List<ItemStack> offhands = new ArrayList<>();

        for (Player player : players) {
            contents.add(player.getInventory().getContents().clone());
            armor.add(player.getInventory().getArmorContents().clone());
            offhands.add(player.getInventory().getItemInOffHand().clone());
        }

        List<Player> shuffledPlayers = new ArrayList<>(players);
        do { Collections.shuffle(shuffledPlayers); } while (noDoubles(players, shuffledPlayers));

        for (int i = 0; i < players.size(); i++) {
            int fromIndex = players.indexOf(shuffledPlayers.get(i));
            Player target = players.get(i);
            target.getInventory().setContents(contents.get(fromIndex));
            target.getInventory().setArmorContents(armor.get(fromIndex));
            target.getInventory().setItemInOffHand(offhands.get(fromIndex));
        }
    }

    public void applyGlow() {
        for (Player player : getActivePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 20, 0, false, false, true));
        }
    }

    public void dropAnvilsAbovePlayers() {
        for (Player player : getActivePlayers()) {
            Location loc = player.getLocation().add(0, 30, 0);
            player.getWorld().spawnFallingBlock(loc, Material.ANVIL.createBlockData());
        }
    }

    public void randomNegativeEffect() {
        int duration = 20 * 20;
        int amplifier = 1;

        PotionEffectType[] negativeEffectTypes = {
                PotionEffectType.BLINDNESS,
                PotionEffectType.NAUSEA,
                PotionEffectType.HUNGER,
                PotionEffectType.POISON,
                PotionEffectType.SLOWNESS,
                PotionEffectType.MINING_FATIGUE,
                PotionEffectType.WEAKNESS,
                PotionEffectType.LEVITATION
        };

        for (Player player : getActivePlayers()) {
            PotionEffectType effect = negativeEffectTypes[random.nextInt(negativeEffectTypes.length)];
            player.addPotionEffect(new PotionEffect(effect, duration, amplifier));
        }
    }

    public void giveThrowable() {
        Material[] items = {Material.EGG, Material.SNOWBALL};
        ItemStack throwable = new ItemStack(items[random.nextInt(items.length)], 1);
        for (Player player : getActivePlayers()) {
            player.getInventory().addItem(throwable);
        }
    }

    public void swapHealthFoodSaturation() {
        List<Player> players = getActivePlayers();
        if (players.size() < 2) return;

        List<Double> healthList = new ArrayList<>();
        List<Integer> foodList = new ArrayList<>();
        List<Float> saturationList = new ArrayList<>();

        for (Player player : players) {
            healthList.add(player.getHealth());
            foodList.add(player.getFoodLevel());
            saturationList.add(player.getSaturation());
        }

        List<Player> shuffledPlayers = new ArrayList<>(players);
        do { Collections.shuffle(shuffledPlayers); } while (noDoubles(players, shuffledPlayers));

        for (int i = 0; i < players.size(); i++) {
            int from = players.indexOf(shuffledPlayers.get(i));
            Player target = players.get(i);

            double newHealth = Math.min(healthList.get(from), target.getMaxHealth());
            target.setHealth(newHealth);
            target.setFoodLevel(foodList.get(from));
            target.setSaturation(saturationList.get(from));
        }
    }

    public void makeEveryoneJump() {
        for (Player player : getActivePlayers()) {
            player.setVelocity(player.getVelocity().add(new Vector(0, 1.2, 0)));
        }
    }

    private boolean noDoubles(List<Player> original, List<Player> shuffled) {
        for (int i = 0; i < original.size(); i++) {
            if (original.get(i).equals(shuffled.get(i))) return true;
        }
        return false;
    }
}
