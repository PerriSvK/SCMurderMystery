package sk.perri.murdermystery.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.Main;
import sk.perri.murdermystery.enums.GameState;

public class Murder implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if((sender instanceof Player && !sender.isOp()) || args.length == 0)
        {
            sender.sendMessage(new String[]{
                    ChatColor.GRAY+"-------------------------",
                    ChatColor.DARK_RED+""+ChatColor.BOLD+"Murder "+ChatColor.WHITE+""+ChatColor.BOLD+"Mystery"+
                            ChatColor.GRAY+" by "+ChatColor.DARK_GREEN+""+ChatColor.BOLD+"Perri",
                    ChatColor.GRAY+"-------------------------"});
            return true;
        }

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
            sender.sendMessage("/murder <start|forcestart|who|par>");

        if(args[0].equalsIgnoreCase("who"))
        {
            if(Main.get().getHra().getState() == GameState.Ingame)
            {
                sender.sendMessage(ChatColor.DARK_GREEN+"[MM] Vrah je: "+ChatColor.RED+""+ChatColor.BOLD+
                    (Main.get().getHra().getKiller() == null ? "NULL" : Main.get().getHra().getKiller().getPlayer().getDisplayName()));

                sender.sendMessage(ChatColor.DARK_GREEN+"[MM] Detektiv je: "+ChatColor.BLUE+""+ChatColor.BOLD+
                    (Main.get().getHra().getDetective() == null ? "NULL" : Main.get().getHra().getDetective().getPlayer().getDisplayName()));
            }
        }

        return true;
    }
}
