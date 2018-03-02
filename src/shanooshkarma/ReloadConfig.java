/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shanooshkarma;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 *
 * @author storm
 */
public class ReloadConfig implements CommandExecutor
{

    private ShanooshKarma plugin;
    public ReloadConfig (ShanooshKarma plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
        if(cs.hasPermission("Karma.Commands")){
              plugin.reloadConfig();
              cs.sendMessage("Reloaded config");
        }
        
        return true;
    }
    
}
