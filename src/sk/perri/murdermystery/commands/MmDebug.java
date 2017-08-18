package sk.perri.murdermystery.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import sk.perri.murdermystery.Main;

import java.util.Arrays;

public class MmDebug implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(!sender.isOp())
        {
            sender.sendMessage(ChatColor.RED+"Nie pre tvoje oci!");
            return true;
        }

        if(args.length < 1)
        {
            sender.sendMessage(ChatColor.YELLOW+"/mmd <players|player>");
            return true;
        }

        if(args[0].equalsIgnoreCase("players"))
        {
            sender.sendMessage(Arrays.toString(Main.get().getHra().getLudia().values().toArray()));
            return true;
        }

        if(args[0].equalsIgnoreCase("player"))
        {
            if(args.length == 2)
            {
                if (Main.get().getHra().getLudia().containsKey(args[1]))
                    sender.sendMessage(Main.get().getHra().getLudia().get(args[1]).toString());
                else
                    sender.sendMessage(ChatColor.RED + "Neviem najst hraca " + ChatColor.GOLD + args[1]);
            }
            else
            {
                sender.sendMessage(ChatColor.RED+"/mmd player <nick>");
            }
            return true;
        }

        return true;
    }
}
