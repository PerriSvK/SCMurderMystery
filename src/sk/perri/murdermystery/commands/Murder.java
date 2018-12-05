package sk.perri.murdermystery.commands;

import java.util.Vector;

import me.mirek.devtools.api.DevTools;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.Game;
import sk.perri.murdermystery.MainMurder;
import sk.perri.murdermystery.enums.GameState;
import sk.perri.murdermystery.game.Clovek;
import sk.perri.murdermystery.game.Ludia;

public class Murder implements org.bukkit.command.CommandExecutor
{
    public Murder()
    {
    }

    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args)
    {
        if ((!(sender instanceof Player)) || (args.length == 0))
        {
            sender.sendMessage(new String[]{ChatColor.GRAY + "-------------------------", ChatColor.DARK_RED + "" + ChatColor.BOLD + "Murder " + ChatColor.WHITE + "" + ChatColor.BOLD + "Mystery" + ChatColor.GRAY + " by " + ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Perri", ChatColor.GRAY + "-------------------------"});


            return true;
        }

        if (args[0].equalsIgnoreCase("top"))
        {
            sender.sendMessage(ChatColor.RED + "-------------------------------");
            sender.sendMessage(ChatColor.RED + "Nejvíc karmy:");
            for (int i = 1; i < 4; i++)
            {
                sender.sendMessage(ChatColor.GRAY + "#" + i + ": " + ChatColor.RED + (String) MainMurder.get().getTop().get(i - 1));
            }
            sender.sendMessage(ChatColor.DARK_GRAY + "-------------------------------");
            sender.sendMessage(ChatColor.DARK_AQUA + "Nejvíc odohraných her:");
            for (int i = 1; i < 4; i++)
            {
                sender.sendMessage(ChatColor.GRAY + "#" + i + ": " + ChatColor.DARK_AQUA + (String) MainMurder.get().getTop().get(i + 2));
            }
            sender.sendMessage(ChatColor.DARK_AQUA + "-------------------------------");

            return true;
        }

        if (args[0].equalsIgnoreCase("me"))
        {
            Clovek c = Ludia.getClovek((Player) sender);
            if (c == null)
            {
                sender.sendMessage(ChatColor.RED + "Neviem ta najst!");
                return true;
            }
            sender.sendMessage(ChatColor.GRAY + "-------------------------------");
            sender.sendMessage(ChatColor.GRAY + "Vrah si nebyl " + ChatColor.RED + Math.round(Math.floor(c.getLkil())) + ChatColor.GRAY + " her. Máš " + ChatColor.RED +
                    Math.round(c.getPerk()) + ChatColor.GRAY + "%");
            sender.sendMessage(ChatColor.GRAY + "Detektiv si nebyl " + ChatColor.DARK_AQUA + Math.round(Math.floor(c.getLdec())) + ChatColor.GRAY + " her. Máš " + ChatColor.DARK_AQUA +
                    Math.round(c.getPerd()) + ChatColor.GRAY + "%");
            sender.sendMessage(ChatColor.GRAY + "Tvoje karma: " + ChatColor.GREEN + c.getScore());
            sender.sendMessage(ChatColor.GRAY + "Odohrané hry: " + ChatColor.YELLOW + c.getGames());
            sender.sendMessage(ChatColor.GRAY + "-------------------------------");


            return true;
        }

        if (args[0].equalsIgnoreCase("help"))
        {
            if (!sender.isOp())
            {
                sender.sendMessage("/murder <me|top>");
            }
            else
            {
                sender.sendMessage("/murder <start|forcestart|who||me,top>");
            }
            return true;
        }

        if (!sender.isOp())
        {
            sender.sendMessage(ChatColor.RED + "Bohužel nejsi OP, proto můžeš požit len tyto příkazy: me, top");
            return true;
        }

        if (args[0].equalsIgnoreCase("world"))
        {
            sender.sendMessage(ChatColor.GOLD + "Si na svete: " + ChatColor.RED + ((Player) sender).getLocation().getWorld().getName());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("kick"))
        {
            Player p = MainMurder.get().getServer().getPlayer(args[1]);
            if(p != null)
            {
                DevTools.getBungeeCord().sendToLobby(p);
                sender.sendMessage("§eHrac bol poslany na lobby!");
            }
            else
            {
                sender.sendMessage("§cNeviem najst hraca "+args[1]);
            }
        }

        if(args[0].equalsIgnoreCase("kickall"))
        {
            for(Player p : MainMurder.get().getServer().getOnlinePlayers())
            {
                if(p != ((Player) sender).getPlayer())
                {
                    DevTools.getBungeeCord().sendToLobby(p);
                }
            }

            sender.sendMessage("§eHraci boli poslany na lobby!");
        }

        if(args[0].equalsIgnoreCase("setup"))
        {
            MainMurder.get().getHra().setState(GameState.Setup);
            sender.sendMessage("§eNastavil si hru do §5§lSETUP §r§emodu!");
            return true;
        }

        if (MainMurder.get().getServer().getOnlinePlayers().size() < 2)
        {
            sender.sendMessage("Malo hracov, treba aspon 2!");
            return true;
        }

        if (args[0].equalsIgnoreCase("start"))
        {
            MainMurder.get().getHra().start(false);
        }
        if (args[0].equalsIgnoreCase("forcestart"))
        {
            MainMurder.get().getHra().start(true);
        }
        if (args[0].equalsIgnoreCase("who"))
        {
            if (MainMurder.get().getHra().getState() == sk.perri.murdermystery.enums.GameState.Ingame)
            {
                sender.sendMessage(ChatColor.DARK_GREEN + "[MM] Vrah je: " + ChatColor.RED + "" + ChatColor.BOLD + (
                        Ludia.getVrah() == null ? "NULL" : Ludia.getVrah().getPlayer().getDisplayName()));

                sender.sendMessage(ChatColor.DARK_GREEN + "[MM] Detektiv je: " + ChatColor.BLUE + "" + ChatColor.BOLD + (
                        Ludia.getDetektiv() == null ? "NULL" : Ludia.getDetektiv().getPlayer().getDisplayName()));
            }
        }

        return true;
    }
}
