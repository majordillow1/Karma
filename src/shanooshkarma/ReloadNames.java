/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shanooshkarma;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static shanooshkarma.ShanooshKarma.getChat;

/**
 *
 * @author storm
 */
public class ReloadNames implements CommandExecutor {
     private ShanooshKarma plugin;
    public ReloadNames (ShanooshKarma plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
        if(cs.hasPermission("Karma.Commands")){
              for(Player player : plugin.getServer().getOnlinePlayers()){
                  getChat().setPlayerSuffix(player, ChatColor.translateAlternateColorCodes('&',plugin.getPlayerName(player)));
                  plugin.setAboveNames();
              }
              cs.sendMessage("Reloaded names");
        }
        
        return true;
    }

    
}
