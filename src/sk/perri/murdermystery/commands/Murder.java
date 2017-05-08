package sk.perri.murdermystery.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.Main;

public class Murder implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length == 0)
            return false;

        if(sender instanceof Player && !sender.isOp())
            return true;

        if(Main.get().getServer().getOnlinePlayers().size() < 2)
        {
            sender.sendMessage("Malo hracov, treba aspon 2!");
            return true;
        }

        if(args[0].equalsIgnoreCase("start"))
            Main.get().getHra().start(false);

        if(args[0].equalsIgnoreCase("forcestart"))
            Main.get().getHra().start(true);

        if(args[0].equalsIgnoreCase("help"))
            sender.sendMessage("/murder <start|forcestart>");

        return true;
    }
}
