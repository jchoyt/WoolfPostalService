/*
 * Copyright 2018 Jeffrey C. Hoyt. Released under the Aapache 2.0 license
 */
package com.asharpminer.wps;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;


public class PlacementListener implements Listener {

    private WoolfPostalService plugin = null;
    private Logger logger = null;

    public PlacementListener(WoolfPostalService plugin) {
        this.plugin = plugin;  // Store the plugin in situations where you need it.
        logger = plugin.getLogger();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        logger.config("Placement listener registered");
    }

    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // looks for a package to be placed in one of the designated locations
        if(event.isCancelled()) return;

        // make sure it's a shulker
        Block box = event.getBlock();
        logger.warning(box.getBlockData().getMaterial().name() + " placed");
        if(!box.getBlockData().getMaterial().name().contains(Material.SHULKER_BOX.name())) return;

        Location pickup = box.getLocation();
        Player customer = event.getPlayer();
        
        plugin.asyncBroadcast(customer.getName() + " placed a package for pickup at " + pickup.getX() + " " 
            + pickup.getY() + " " +pickup.getZ() + " in " + pickup.getWorld().getName(), 5L);
        
        
        // if(Math.random(  ) > 0.02 || event.getBedEnterResult​().equals(PlayerBedEnterEvent.BedEnterResult.valueOf("OK"))) return;
        // String who = event.getPlayer().getName();
        // logger.fine( who + " has slept");
        // plugin.asyncBroadcast(ChatColor.DARK_AQUA + "Goodnight dear " + who + "! May you dream wonderful dreams.", 20L);
    }
}
