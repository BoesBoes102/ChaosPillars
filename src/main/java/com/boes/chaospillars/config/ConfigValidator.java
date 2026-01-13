package com.boes.chaospillars.config;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ConfigValidator {
    
    private final JavaPlugin plugin;
    private final List<String> errors = new ArrayList<>();
    
    public ConfigValidator(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean validateConfig() {
        errors.clear();
        
        validateGameSettings();
        validateBlockTypes();
        
        if (!errors.isEmpty()) {
            plugin.getLogger().severe("Configuration validation failed:");
            errors.forEach(error -> plugin.getLogger().severe("  - " + error));
            return false;
        }
        
        plugin.getLogger().info("Configuration validated successfully!");
        return true;
    }
    
    private void validateGameSettings() {
        int timerSeconds = plugin.getConfig().getInt("game.timer-seconds", -1);
        if (timerSeconds < 180) {
            errors.add("game.timer-seconds must be atleast 180 seconds or more! (found: " + timerSeconds + ")");
        }
        
        int eventCooldown = plugin.getConfig().getInt("game.event-cooldown-seconds", -1);
        if (eventCooldown < 5) {
            errors.add("game.event-cooldown-seconds should be at least 5 seconds (found: " + eventCooldown + ")");
        }
        
        int powerupCooldown = plugin.getConfig().getInt("game.powerup-cooldown-seconds", -1);
        if (powerupCooldown < 10) {
            errors.add("game.powerup-cooldown-seconds should be at least 10 seconds (found: " + powerupCooldown + ")");
        }
        
        int itemInterval = plugin.getConfig().getInt("game.item-give-interval-seconds", -1);
        if (itemInterval < 1) {
            errors.add("game.item-give-interval-seconds must be at least 1 second (found: " + itemInterval + ")");
        }
    }
    
    private void validateBlockTypes() {
        validateMaterialList("game.pillar-block-types", "pillar");
        validateMaterialList("game.floor-block-types", "floor");
    }
    
    private void validateMaterialList(String configPath, String blockType) {
        List<String> materials = plugin.getConfig().getStringList(configPath);
        
        if (materials.isEmpty()) {
            errors.add(configPath + " cannot be empty");
            return;
        }
        
        for (String materialName : materials) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                if (!material.isBlock() && !materialName.equalsIgnoreCase("AIR")) {
                    errors.add(configPath + " contains non-block material: " + materialName);
                }
            } catch (IllegalArgumentException e) {
                errors.add(configPath + " contains invalid material: " + materialName);
            }
        }
        
        plugin.getLogger().info("Validated " + materials.size() + " " + blockType + " block types");
    }

}
