package sk.perri.murdermystery;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InvBuilder
{
    private InvBuilder()
    {

    }

    static Inventory buildInv(Player player)
    {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE+"KOSMETIK MENU");
        inv.setItem(11, craftItem(Material.WOOD_SWORD, player));
        inv.setItem(12, craftItem(Material.STONE_SWORD, player));
        inv.setItem(13, craftItem(Material.IRON_SWORD, player));
        inv.setItem(14, craftItem(Material.GOLD_SWORD, player));
        inv.setItem(15, craftItem(Material.DIAMOND_SWORD, player));

        return inv;
    }

    static ItemStack craftItem(Material m, Player player)
    {
        String nazov = "";

        switch (m)
        {
            case WOOD_SWORD: nazov = "Dřevěnný meč"; break;
            case STONE_SWORD: nazov = "Kamenný meč"; break;
            case IRON_SWORD: nazov = "Železný meč"; break;
            case GOLD_SWORD: nazov = "Zlatý meč"; break;
            case DIAMOND_SWORD: nazov = "Diamantový meč"; break;
        }

        ChatColor chc = ChatColor.RED;
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.RED+"Tento meč nemáš odomčen!");

        if(m == Material.IRON_SWORD || player.hasPermission("murder.swords."+m.name().toLowerCase()))
        {
            chc = ChatColor.GREEN;
            lore.set(1, ChatColor.GREEN+"Klikni pro výběr!");
        }

        ItemStack is = new ItemStack(m, 1);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(chc+nazov);

        if(Main.get().getHra().findClovek(player).getSword() == m)
        {
            im.addEnchant(Enchantment.LUCK, 1, true);
            lore.set(1, ChatColor.GOLD+"Vybrán!");
        }

        im.setLore(lore);
        is.setItemMeta(im);

        return is;
    }
}
