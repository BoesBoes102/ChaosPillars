package com.boes.chaospillars.tasks;

import com.boes.chaospillars.ChaosPillars;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class EventTask {

    private final ChaosPillars plugin;
    private final Random random = new Random();

    public EventTask(ChaosPillars plugin, World gameWorld) {
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
        int index = random.nextInt(9);
        String eventName;

        switch (index) {
            case 0 -> { sizeChange(); eventName = "Size Change"; }
            case 1 -> { randomItemDeleter(); eventName = "Item Purge"; }
            case 2 -> { swapPositions(); eventName = "Swap Positions"; }
            case 3 -> { swapInventories(); eventName = "Swap Inventories"; }
            case 4 -> { tntDrop(); eventName = "TNT Drop"; }
            case 5 -> { randomNegativeEffect(); eventName = "Bad Potion Effect"; }
            case 6 -> { giveThrowable(); eventName = "Snowball Fight"; }
            case 7 -> { swapHealthFoodSaturation(); eventName = "Swap Health"; }
            case 8 -> { thunderStrike(); eventName = "Thunder Jump"; }
            default -> eventName = "Unknown Event";
        }

        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[Chaos] " + ChatColor.YELLOW + eventName + ChatColor.LIGHT_PURPLE + " event has occurred!");
    }

    public void sizeChange() {
        World world = getGameWorld();
        if (world == null) return;

        for (Player player : getActivePlayers()) {
            double scale = 0.5 + random.nextDouble();
            var attribute = player.getAttribute(Attribute.SCALE);
            if (attribute != null) {
                attribute.setBaseValue(scale);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                var attribute = player.getAttribute(Attribute.SCALE);
                if (attribute != null) {
                    attribute.setBaseValue(1.0);
                }
            }
        }, 20 * 10);
    }

    public void randomItemDeleter() {
        for (Player player : getActivePlayers()) {
            ItemStack[] contents = player.getInventory().getContents();
            List<Integer> nonEmptyIndexes = new ArrayList<>();
            for (int i = 0; i < contents.length; i++) if (contents[i] != null) nonEmptyIndexes.add(i);

            int itemsToRemove = Math.max(1, nonEmptyIndexes.size() / 7);
            Collections.shuffle(nonEmptyIndexes);
            for (int i = 0; i < itemsToRemove; i++) contents[nonEmptyIndexes.get(i)] = null;

            player.getInventory().setContents(contents);
        }
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

    public void tntDrop() {
        for (Player player : getActivePlayers()) {
            Location loc = player.getLocation().add(0, 10, 0);
            org.bukkit.entity.TNTPrimed tnt = player.getWorld().spawn(loc, org.bukkit.entity.TNTPrimed.class);
            tnt.setFuseTicks(50);
            tnt.setMetadata("chaos_tnt", new FixedMetadataValue(plugin, true));
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setVelocity(new Vector(0, 1.3, 0));
                }
            }, 40);
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

    public void thunderStrike() {
        for (Player player : getActivePlayers()) {
            Location loc = player.getLocation();
            player.getWorld().strikeLightningEffect(loc);
            player.setVelocity(player.getVelocity().add(new Vector(0, 2, 0)));
            plugin.getThunderstruckPlayers().add(player.getUniqueId());
            loc.getBlock().setType(Material.FIRE);
        }
    }

    private boolean noDoubles(List<Player> original, List<Player> shuffled) {
        for (int i = 0; i < original.size(); i++) {
            if (original.get(i).equals(shuffled.get(i))) return true;
        }
        return false;
    }
}
