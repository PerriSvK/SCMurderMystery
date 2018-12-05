package sk.perri.murdermystery.commands;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import sk.perri.murdermystery.MainMurder;

import static java.lang.Double.*;

public class GadgetCommand implements CommandExecutor, Listener
{
    public int type = 0;
    public int open = 0;
    public int cost = 0;
    public Location location;
    public Location loc1;
    public Location loc2;
    public int prize;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] arg)
    {
        if(!sender.isOp())
        {
            sender.sendMessage("§cPrepáč, na toto nemáš práva :/");
            return true;
        }

        if(arg.length == 0)
        {
            sender.sendMessage("§e/gadget new <typ> <cena>");
            sender.sendMessage("§e/gadget save");
            sender.sendMessage("§e/gadget cancel");
            sender.sendMessage("§e/gadget types");
            return true;
        }

        if(arg.length == 3 && arg[0].equalsIgnoreCase("new"))
        {
            try
            {
                type = Integer.parseInt(arg[1]);

                if(type > 2)
                {
                    sender.sendMessage("§cZly typ gadgetu! Napis /gadget types");
                    return true;
                }

                cost = Integer.parseInt(arg[2]);
                open = 1;
                sender.sendMessage("§aKlikni na blok, ktory aktivuje gadget.");
            }
            catch (Exception e)
            {
                sender.sendMessage("Typ a cena musi byt cislo!");
            }
            return true;
        }

        if(arg[0].equalsIgnoreCase("save"))
        {
            if(type == 2 && location != null && loc1 != null && loc2 != null)
            {
                saveGadget();
                sender.sendMessage("§aGadget bol ulozeny! Nacita sa pri zapnuti serveru!");
            }
            else if(type == 1 && location != null && loc1 != null)
            {
                saveGadget();
                sender.sendMessage("§aGadget bol ulozeny! Nacita sa pri zapnuti serveru!");
            }
            else if(type == 0 && location != null)
            {
                saveGadget();
                sender.sendMessage("§aGadget bol ulozeny! Nacita sa pri zapnuti serveru!");
            }
            else
            {
                sender.sendMessage("§cNieco je zle nastavene! Typ: "+type+" loc: "+
                        (location == null ? "NULL" : "SET")+" loc1: "+(loc1 == null ? "NULL" : "SET")+
                        " loc2: "+(loc2 == null ? "NULL" : "SET"));
            }
            return true;
        }

        if(arg[0].equalsIgnoreCase("cancel"))
        {
            nullData();
            sender.sendMessage("§eData boli vynulovane!");
        }

        if(arg[0].equalsIgnoreCase("types"))
        {
            sender.sendMessage("§e0 - Potion");
            sender.sendMessage("§e1 - Teleport");
            sender.sendMessage("§e2 - Zmiznutie blokov");
        }

        return true;
    }

    private void saveGadget()
    {
        Configuration conf = MainMurder.get().getMap().getConf();

        int i = 1;
        if (conf.isSet("gadget.1.type"))
        {
            i = conf.getConfigurationSection("gadget").getKeys(false).size() + 1;
        }
        conf.set("gadget." + i + ".type", type);
        conf.set("gadget." + i + ".cost", cost);
        conf.set("gadget." + i + ".loc.world", location.getWorld().getName());
        conf.set("gadget." + i + ".loc.x", location.getX());
        conf.set("gadget." + i + ".loc.y", location.getY());
        conf.set("gadget." + i + ".loc.z", location.getZ());

        if(loc1 != null)
        {
            conf.set("gadget." + i + ".loc1.world", loc1.getWorld().getName());
            conf.set("gadget." + i + ".loc1.x", loc1.getX());
            conf.set("gadget." + i + ".loc1.y", loc1.getY());
            conf.set("gadget." + i + ".loc1.z", loc1.getZ());
            conf.set("gadget." + i + ".loc1.yaw", loc1.getYaw());
            conf.set("gadget." + i + ".loc1.pitch", loc1.getPitch());
        }

        if(loc2 != null)
        {
            conf.set("gadget." + i + ".loc2.world", loc2.getWorld().getName());
            conf.set("gadget." + i + ".loc2.x", loc2.getX());
            conf.set("gadget." + i + ".loc2.y", loc2.getY());
            conf.set("gadget." + i + ".loc2.z", loc2.getZ());
        }

        MainMurder.get().getMap().saveConfig();
    }

    public void nullData()
    {
        open = -1;
        type = 0;
        location = null;
        loc1 = null;
        loc2 = null;
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event)
    {
        if(event.getPlayer().isOp() && open != 0)
        {
            switch (open)
            {
                case 1: location = event.getClickedBlock().getLocation(); break;
                case 2: loc1 = event.getClickedBlock().getLocation(); break;
                case 3: loc2 = event.getClickedBlock().getLocation(); break;
            }

            if(type+1 == open)
            {
                event.getPlayer().sendMessage("§eLokacia nastavena, pouzi §6/gadget save");
                open = 0;
                return;
            }

            event.getPlayer().sendMessage("§eLokacia nastavena, klinkni na dalsi blok na nastavenie dalsej");
            open++;

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e)
    {
        if(e.getPlayer().isOp() && open != 0 && e.getPlayer().getGameMode() == GameMode.CREATIVE)
        {
            e.setCancelled(true);
        }
    }
}
