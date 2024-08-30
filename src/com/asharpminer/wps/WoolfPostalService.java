/*
 * Copyright 2024 Jeffrey C. Hoyt. Released under the Aapache 2.0 license
 */

package com.asharpminer.wps;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class WoolfPostalService extends JavaPlugin {

    private Logger log = null;

    public WoolfPostalService() {
        log = getLogger();
    }

    @Override
    public void onEnable() {
        // load up listeners and command executors
        new PlacementListener(this); // listens for package placements
    }

    @Override
    public void onDisable() {
        // TODO Insert logic to be performed when the plugin is disabled
    }

    /*
     * If a player has ever logged into this server, this returns the OfflinePlayer
     * representation of that player. Returns null if the player has never logged
     * to this server. To see if the player is currently online, use OfflinePlayer.getPlayer()
     * which will return a Player object if they are online or null if they are not.
     */
    public OfflinePlayer getPlayerByName(String name) {
        OfflinePlayer[]  offlinePlayers = this.getServer().getOfflinePlayers();
        OfflinePlayer targetPlayer = null;
        for(OfflinePlayer p: offlinePlayers) {
            if(name.equalsIgnoreCase(p.getName())) {
                targetPlayer = p;
                break;
            }
        }
        return targetPlayer;
    }

    public void runCommand(String command, long delay){
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }, delay);
    }

    /** Broadcast a message for an asynchronous chat **/
    public void asyncBroadcast(String message, long delay){
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage(message);
            }
        }, delay);
    }

}
