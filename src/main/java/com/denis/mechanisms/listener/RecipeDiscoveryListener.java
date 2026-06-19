package com.denis.mechanisms.listener;

import com.denis.mechanisms.crafting.MechanismRecipeService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class RecipeDiscoveryListener implements Listener {
    private final MechanismRecipeService recipeService;

    public RecipeDiscoveryListener(MechanismRecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        recipeService.discoverAll(event.getPlayer());
    }
}
