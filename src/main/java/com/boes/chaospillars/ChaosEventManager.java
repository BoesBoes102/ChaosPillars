package com.boes.chaospillars;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class ChaosEventManager {

    private final JavaPlugin plugin;
    private final Random random = new Random();


    public ChaosEventManager(JavaPlugin plugin) {
        this.plugin = plugin;

    }

    public void triggerRandomEvent() {
        int index = random.nextInt(11);
        switch (index) {
            case 0 -> changeWeather();
            case 1 -> randomTimeChange();
            case 2 -> swapPositions();
            case 3 -> swapInventories();
            case 4 -> spawnFakeTNTOnPlayers();
            case 5 -> dropAnvilsAbovePlayers();
            case 6 -> randomNegativeEffect();
            case 7 -> giveThrowable();
            case 8 -> swapHealthFoodSaturation();
            case 9 -> makeEveryoneJump();
            case 10 -> forceSneakToggle();
        }
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[Chaos] A random event has occurred!");
    }

    public void changeWeather() {
        World world = Bukkit.getWorlds().getFirst();
        world.setStorm(!world.hasStorm());
    }

    public void randomTimeChange() {
        World world = Bukkit.getWorlds().getFirst();
        world.setTime(random.nextInt(24000));
    }

    public void swapPositions() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.size() < 2) return;

        List<Location> locations = players.stream()
                .map(Player::getLocation)
                .toList();

        Collections.shuffle(players);

        for (int i = 0; i < players.size(); i++) {
            Location loc = locations.get((i + 1) % players.size());
            players.get(i).teleport(loc);
        }
    }

    public void swapInventories() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.size() < 2) return;

        // Store complete inventory state
        List<ItemStack[]> contents = new ArrayList<>();
        List<ItemStack[]> armor = new ArrayList<>();
        List<ItemStack> offhands = new ArrayList<>();

        for (Player player : players) {
            contents.add(player.getInventory().getContents().clone());
            armor.add(player.getInventory().getArmorContents().clone());
            offhands.add(player.getInventory().getItemInOffHand().clone());
        }

        // Shuffle players
        Collections.shuffle(players);

        for (int i = 0; i < players.size(); i++) {
            int from = (i + 1) % players.size(); // next player's inventory

            players.get(i).getInventory().setContents(contents.get(from));
            players.get(i).getInventory().setArmorContents(armor.get(from));
            players.get(i).getInventory().setItemInOffHand(offhands.get(from));
        }
    }

    public void spawnFakeTNTOnPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation().clone().add(0, 2, 0); // 2 blocks above player's head
            World world = loc.getWorld();
            if (world == null) continue;

            // Spawn falling block TNT entity (fake TNT)
            FallingBlock tnt = world.spawnFallingBlock(loc, Material.TNT.createBlockData());
            tnt.setDropItem(false);        // don't drop item on landing
            tnt.setHurtEntities(false);    // doesn't hurt players
            tnt.setVelocity(new Vector(0, -0.1, 0)); // slight downward motion so it "falls"
        }
    }

    public void dropAnvilsAbovePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation().add(0, 30, 0);
            player.getWorld().spawnFallingBlock(loc, Material.ANVIL.createBlockData());
        }
    }


    public void randomNegativeEffect() {
        int duration = 20 * 3;  // 3 seconds in ticks
        int amplifier = 1;      // level 2 effect (amplifier starts at 0)

        PotionEffectType[] negativeEffectTypes = new PotionEffectType[] {
                PotionEffectType.BLINDNESS,
                PotionEffectType.NAUSEA,       // Nausea
                PotionEffectType.INSTANT_DAMAGE,
                PotionEffectType.HUNGER,
                PotionEffectType.POISON,
                PotionEffectType.SLOWNESS,
                PotionEffectType.MINING_FATIGUE,    // Mining Fatigue
                PotionEffectType.UNLUCK,
                PotionEffectType.WEAKNESS,
                PotionEffectType.WITHER,
                PotionEffectType.LEVITATION
        };

        Random rand = new Random();

        for (Player player : Bukkit.getOnlinePlayers()) {
            PotionEffectType randomEffect = negativeEffectTypes[rand.nextInt(negativeEffectTypes.length)];
            PotionEffect effect = new PotionEffect(randomEffect, duration, amplifier);
            player.addPotionEffect(effect);
        }
    }



    public void giveThrowable() {
        Material[] items = {Material.EGG, Material.SNOWBALL};
        ItemStack throwable = new ItemStack(items[random.nextInt(items.length)], 1);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().addItem(throwable);
        }
    }

    public void swapHealthFoodSaturation() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.size() < 2) return;

        List<Double> healthList = new ArrayList<>();
        List<Integer> foodList = new ArrayList<>();
        List<Float> saturationList = new ArrayList<>();

        for (Player player : players) {
            healthList.add(player.getHealth());
            foodList.add(player.getFoodLevel());
            saturationList.add(player.getSaturation());
        }

        Collections.shuffle(players);

        for (int i = 0; i < players.size(); i++) {
            int from = (i + 1) % players.size();

            Player target = players.get(i);

            double newHealth = Math.min(healthList.get(from), target.getMaxHealth());
            target.setHealth(newHealth);
            target.setFoodLevel(foodList.get(from));
            target.setSaturation(saturationList.get(from));
        }
    }

    public void makeEveryoneJump() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setVelocity(player.getVelocity().add(new Vector(0, 1.2, 0)));
        }
    }

    public void forceSneakToggle() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setSneaking(true);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setSneaking(false);
            }
        }, 20L);
    }
}
