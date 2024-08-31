/*
 * Copyright 2018 Jeffrey C. Hoyt. Released under the Aapache 2.0 license
 */

package com.asharpminer.wps;

import java.util.logging.Logger;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

/* Sets the location of a mailbox.  This adds the location the player is currently standing on to the list of 
 * locations monitored by this plugin.  This assumes a permissions plugin exists to manage who has access
 */
public class MailboxCommandExecutor implements CommandExecutor {
    private WoolfPostalService plugin;
    private Logger logger = null;

    public MailboxCommandExecutor(WoolfPostalService plugin) {
        this.plugin = plugin;           // Store the plugin in situations where you need it.
        logger = plugin.getLogger();
        PluginCommand c = this.plugin.getCommand("wpsbox");  //don't forget to update plugin.yml
        c.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        //verify a player is sending this
        if (sender instanceof Player) {
            Player player = (Player)sender;
            String nickname = args.length == 0 ? "nn" : args[0];
            Block block = player.getLocation().getBlock();
            plugin.setMailbox(nickname, block);
            // if(null == plugin.getMailbox(block))
            // {
            //     logger.info("New mailbox location is nicknamed " + nickname);
            //     plugin.setMailbox(nickname, player.getLocation());
            //     return true; 
            // }
            return true;

        } else {
            sender.sendMessage("You must be a player!");
            return false;
        }
    }
}
