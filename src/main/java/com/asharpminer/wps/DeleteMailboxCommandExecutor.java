
/*
 * Copyright 2024 Jeffrey C. Hoyt. Released under the Aapache 2.0 license
 */
package com.asharpminer.wps;


 import java.util.logging.Logger;
 
 import org.bukkit.block.Block;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandExecutor;
 import org.bukkit.command.CommandSender;
 import org.bukkit.command.PluginCommand;
 import org.bukkit.entity.Player;
 
 /* Stops monitoring a location.  This removes the location the player is currently standing on from the list of 
  * locations monitored by this plugin.  This assumes a permissions plugin exists to manage who has access
  */
  public class DeleteMailboxCommandExecutor implements CommandExecutor {
    
     private WoolfPostalService plugin;
     private Logger logger = null;
 
     public DeleteMailboxCommandExecutor(WoolfPostalService plugin) {
         this.plugin = plugin;           // Store the plugin in situations where you need it.
         logger = plugin.getLogger();
         PluginCommand c = this.plugin.getCommand("deletebox");  //don't forget to update plugin.yml
         c.setExecutor(this);
     }
 
     @Override
     public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
         //verify a player is sending this
         if (sender instanceof Player) {
             Player player = (Player)sender;
             Block block = player.getLocation().getBlock();
             if(plugin.deleteMailbox(block)) {
                player.sendMessage("This location is no longer being monitored");
             }
             else 
                player.sendMessage("This isn't an WPS location. No change made.");
             return true;
 
         } else {
             sender.sendMessage("You must be a player!");
             return false;
         }
     }
 }
 