/*
 * Copyright 2018 Jeffrey C. Hoyt. Released under the Aapache 2.0 license
 */
package com.asharpminer.wps;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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
        if(!box.getBlockData().getMaterial().name().contains(Material.SHULKER_BOX.name())) return;

        // bail if the location is not a mailbox 
        if(!plugin.isMailbox(box)) return;

        Location pickup = box.getLocation();
        Player customer = event.getPlayer();
        
        // TODO - add Discord messages here
        String nickname = plugin.getNickname(box);
        String message;
        if(null == nickname) {
            message = customer.getName() + " placed a package for pickup at " + pickup.getBlockX() + " " 
            + pickup.getBlockY() + " " +pickup.getBlockZ() + " in " + pickup.getWorld().getName();
        } else {
            message = customer.getName() + " placed a package for pickup at " + nickname + " (" + pickup.getBlockX() + " " 
            + pickup.getBlockY() + " " +pickup.getBlockZ() + " in " + pickup.getWorld().getName() + ")";
        }

        plugin.asyncBroadcast(message, 5L);


        // notify customer 
        customer.sendMessage("WPS has been notified of your request and someone will be by shortly to pick up your package.");
          
    }
}
