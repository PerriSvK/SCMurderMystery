package sk.perri.murdermystery.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.Main;

public class Setup implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length == 0)
            return false;

        if(!(sender instanceof Player))
        {
            sender.sendMessage("This command can run only player!");
            return true;
        }

        if(!sender.isOp())
            return true;

        if(args[0].equalsIgnoreCase("lobby"))
        {
            Main.get().setLobbyLocation(((Player) sender).getLocation());
            sender.sendMessage(ChatColor.GREEN+"Lobby spawn set!");
        }

        if(args[0].equalsIgnoreCase("spawn"))
        {
            Main.get().setSpawnLocation(((Player) sender).getLocation());
            sender.sendMessage(ChatColor.GREEN+"Spawn added!");
        }

        if(args[0].equalsIgnoreCase("item"))
        {
            Main.get().setItemLocation(((Player) sender).getLocation());
            sender.sendMessage(ChatColor.GREEN+"Item spawn added!");
        }

        if(args[0].equalsIgnoreCase("save"))
        {
            Main.get().saveConfig();
            sender.sendMessage(ChatColor.GREEN+"Setup complete!");
        }

        if(args[0].equalsIgnoreCase("help"))
        {
            sender.sendMessage("HELP: /setup <lobby|spawn|item|save>");
        }

        return true;
    }
}
