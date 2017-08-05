package sk.perri.murdermystery.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.Clovek;
import sk.perri.murdermystery.Main;
import sk.perri.murdermystery.enums.GameState;

public class Murder implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(!(sender instanceof Player) || args.length == 0)//(sender instanceof Player && !sender.isOp()) || args.length == 0)
        {
            sender.sendMessage(new String[]{
                    ChatColor.GRAY+"-------------------------",
                    ChatColor.DARK_RED+""+ChatColor.BOLD+"Murder "+ChatColor.WHITE+""+ChatColor.BOLD+"Mystery"+
                            ChatColor.GRAY+" by "+ChatColor.DARK_GREEN+""+ChatColor.BOLD+"Perri",
                    ChatColor.GRAY+"-------------------------"});
            return true;
        }

        if(args[0].equalsIgnoreCase("top"))
        {
            sender.sendMessage(ChatColor.RED+"-------------------------------");
            sender.sendMessage(ChatColor.RED+"Nejvíc karmy:");
            for(int i = 1; i < 4; i++)
            {
                sender.sendMessage(ChatColor.GRAY+"#"+i+": "+ChatColor.RED+Main.get().getTop().get(i-1));
            }
            sender.sendMessage(ChatColor.DARK_GRAY+"-------------------------------");
            sender.sendMessage(ChatColor.DARK_AQUA+"Nejvíc odohraných her:");
            for(int i = 1; i < 4; i++)
            {
                sender.sendMessage(ChatColor.GRAY+"#"+i+": "+ChatColor.DARK_AQUA+Main.get().getTop().get(i+2));
            }
            sender.sendMessage(ChatColor.DARK_AQUA+"-------------------------------");

            return true;
        }

        if(args[0].equalsIgnoreCase("me"))
        {
            Clovek c = Main.get().getHra().findClovek((Player) sender);
            if(c == null)
            {
                sender.sendMessage(ChatColor.RED+"Neviem ta najst!");
                return true;
            }
            sender.sendMessage(ChatColor.GRAY+"-------------------------------");
            sender.sendMessage(ChatColor.GRAY+"Vrah si nebyl "+ChatColor.RED+Math.round(Math.floor(c.getLkil()))+ChatColor.GRAY+" her. Máš "
                    +ChatColor.RED+Math.round(c.getPerk())+ChatColor.GRAY+"%");
            sender.sendMessage(ChatColor.GRAY+"Detektiv si nebyl "+ChatColor.DARK_AQUA+Math.round(Math.floor(c.getLdec()))+ChatColor.GRAY+
                    " her. Máš "+ChatColor.DARK_AQUA+Math.round(c.getPerd())+ChatColor.GRAY+"%");
            sender.sendMessage(ChatColor.GRAY+"Tvoje karma: "+ChatColor.GREEN+c.getScore());
            sender.sendMessage(ChatColor.GRAY+"Odohrané hry: "+ChatColor.YELLOW+c.getGames());
            sender.sendMessage(ChatColor.GRAY+"-------------------------------");


            return true;
        }

        if(args[0].equalsIgnoreCase("help"))
        {
            if(!sender.isOp())
                sender.sendMessage("/murder <me|top>");
            else
                sender.sendMessage("/murder <start|forcestart|who||me,top>");

            return true;
        }

        if(!sender.isOp())
        {
            sender.sendMessage(ChatColor.RED+"Bohužel nejsi OP, proto můžeš požit len tyto příkazy: me, top");
            return true;
        }

        if(args[0].equalsIgnoreCase("world"))
            sender.sendMessage(ChatColor.GOLD+"Si na svete: "+ChatColor.RED+((Player) sender).getLocation().getWorld().getName());

        if(Main.get().getServer().getOnlinePlayers().size() < 2)
        {
            sender.sendMessage("Malo hracov, treba aspon 2!");
            return true;
        }

        if(args[0].equalsIgnoreCase("start"))
            Main.get().getHra().start(false);

        if(args[0].equalsIgnoreCase("forcestart"))
            Main.get().getHra().start(true);

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
