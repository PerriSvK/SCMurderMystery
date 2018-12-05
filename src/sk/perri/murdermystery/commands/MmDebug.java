package sk.perri.murdermystery.commands;

import java.util.Arrays;
import me.mirek.devtools.api.DevTools;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.MainMurder;
import sk.perri.murdermystery.game.Clovek;
import sk.perri.murdermystery.game.Ludia;

public class MmDebug implements CommandExecutor {
    public MmDebug() {
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Nie pre tvoje oci!");
            return true;
        } else if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "/mmd <players|player|stats>");
            return true;
        } else {
            if (args[0].equalsIgnoreCase("testlvl")) {
                String s = sender.getName();
                MainMurder.get().getLogger().info("LVL TEST FOR: " + s);
                DevTools.getLevelManager().addExp(s, 1, true);
            }

            if (args[0].equalsIgnoreCase("players")) {
                sender.sendMessage(Arrays.toString(Ludia.getVsetci().values().toArray()));
                return true;
            } else if (args[0].equalsIgnoreCase("player")) {
                if (args.length == 2) {
                    Player[] con = new Player[]{null};
                    Ludia.getVsetci().keySet().forEach((k) -> {
                        if (k.getDisplayName().contains(args[1])) {
                            con[0] = k;
                        }

                    });
                    if (con[0] != null) {
                        sender.sendMessage(((Clovek)Ludia.getVsetci().get(con[0])).toString());
                    } else {
                        sender.sendMessage(ChatColor.RED + "Neviem najst hraca " + ChatColor.GOLD + args[1]);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "/mmd player <nick>");
                }

                return true;
            } else {
                return true;
            }
        }
    }
}
