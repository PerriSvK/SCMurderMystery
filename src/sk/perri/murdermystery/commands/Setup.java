package sk.perri.murdermystery.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.MainMurder;

public class Setup implements org.bukkit.command.CommandExecutor
{
    public Setup()
    {
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        if (!(sender instanceof Player))
        {
            sender.sendMessage("This command can run only player!");
            return true;
        }

        if(args[0].equalsIgnoreCase("db"))
        {
            String[] ss = args;
            ss[0] = "";

            String sql = String.join(" ", ss);

            MainMurder.get().getDBE().runSql((Player) sender, sql);
            return true;
        }

        if(args[0].equalsIgnoreCase("dbs"))
        {
            String[] ss = args;
            ss[0] = "";

            String sql = String.join(" ", ss);

            MainMurder.get().getDBE().runSqls((Player) sender, sql);
            return true;
        }

        if (!sender.isOp())
        {
            return true;
        }
        if (args[0].equalsIgnoreCase("lobby"))
        {
            MainMurder.get().setLobbyLocation(((Player) sender).getLocation());
            sender.sendMessage(ChatColor.GREEN + "Lobby spawn set!");
        }

        if (args[0].equalsIgnoreCase("spawn"))
        {
            MainMurder.get().setSpawnLocation(((Player) sender).getLocation());
            sender.sendMessage(ChatColor.GREEN + "Spawn added!");
        }

        if (args[0].equalsIgnoreCase("item"))
        {
            MainMurder.get().setItemLocation(((Player) sender).getLocation());
            sender.sendMessage(ChatColor.GREEN + "Item spawn added!");
        }

        if(args[0].equalsIgnoreCase(""))

        if (args[0].equalsIgnoreCase("save"))
        {
            MainMurder.get().getMap().saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Setup complete!");
        }

        if (args[0].equalsIgnoreCase("help"))
        {
            sender.sendMessage("HELP: /setup <lobby|spawn|item|save>");
        }

        return true;
    }
}
