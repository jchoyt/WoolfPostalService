/*
 * Copyright 2024 Jeffrey C. Hoyt. Released under the Aapache 2.0 license
 */

package com.asharpminer.wps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class WoolfPostalService extends JavaPlugin {

    private Logger log = null;
    private Map<Block, String> mailboxes = new HashMap<Block, String>();
    private File configFile = new File(getDataFolder(), "mailboxes.yml");
    private final String delim = ":";

    public WoolfPostalService() {
        log = getLogger();
    }

    @Override
    public void onEnable() {
        // read mailboxes in
        readMailboxes();
        // load up listeners and command executors
        new PlacementListener(this); // listens for package placements
        new MailboxCommandExecutor(this); 
    }

    @Override
    public void onDisable() {
        // save config
        saveMailboxes();
    }

    public void setMailbox(String nickname, Block block) {
        mailboxes.put(block, nickname);
        log.warning("Setting new mailbox at " + block.getLocation().toString() + " named " + nickname);
        // set and save config
    }

    public boolean isMailbox(Block block) {
        if(mailboxes.containsKey(block)) return true;
        return false;
    }

    public void deleteMailbox( Block block ) {
        // remove mailbox and save config
        mailboxes.remove(block);
    }

    private void readMailboxes() {
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
            List<String> boxes = (List<String>)config.get("mailboxes");
            for(String box : boxes) {
                String[] bits = box.split(delim);
                Block block;
                World world = Bukkit.getServer().getWorld(bits[0]);
                
                Location loc = new Location(world, 
                    Double.parseDouble(bits[1]),
                    Double.parseDouble(bits[2]),
                    Double.parseDouble(bits[3]));

                mailboxes.put(loc.getBlock(), bits[4]);
            }
        }
        catch (FileNotFoundException e)
        {
            saveMailboxes();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void saveMailboxes() {
        FileConfiguration config = new YamlConfiguration();
        List<String> ser = new ArrayList<String>();
        // convert mailboxes to text
        for(Map.Entry<Block, String> mailbox : mailboxes.entrySet()) {
            Location loc = mailbox.getKey().getLocation(); 
            StringBuffer buf = new StringBuffer(loc.getWorld().getName());
            buf.append(delim);
            buf.append(loc.getBlockX());
            buf.append(delim);
            buf.append(loc.getBlockY());
            buf.append(delim);
            buf.append(loc.getBlockZ());
            buf.append(delim);
            buf.append(mailbox.getValue());
            ser.add(buf.toString());
        }
        config.set("mailboxes", ser);
        try {
            config.save(configFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
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
