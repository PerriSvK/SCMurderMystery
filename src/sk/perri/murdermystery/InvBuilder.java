package sk.perri.murdermystery;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import sk.perri.murdermystery.enums.Traily;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InvBuilder
{
    private InvBuilder() { }

    static Inventory buildKosmeticInv(Player player)
    {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE+"KOSMETIK MENU");
        inv.setItem(11, craftItem(Material.WOOD_SWORD, player));
        inv.setItem(12, craftItem(Material.STONE_SWORD, player));
        inv.setItem(13, craftItem(Material.IRON_SWORD, player));
        inv.setItem(14, craftItem(Material.GOLD_SWORD, player));
        inv.setItem(15, craftItem(Material.DIAMOND_SWORD, player));

        // Traily
        String noUn = ChatColor.RED+"Tento trail nemáš odomčen!";
        String un = ChatColor.GREEN+"Klikni pro výběr!";
        String se = ChatColor.GOLD+"Vybrán!";

        ItemStack noTrailIs = new ItemStack(Material.WOOL, 1, (byte) 14);
        ItemMeta noTrailIm = noTrailIs.getItemMeta();
        noTrailIm.setDisplayName(ChatColor.RED+""+ChatColor.BOLD+"Bez trailu");

        if(Main.get().getHra().findClovek(player).getTrail() == null)
        {
            noTrailIm.setLore(Arrays.asList("", se));
            noTrailIm.addEnchant(Enchantment.LUCK, 1, true);
        }
        else
            noTrailIm.setLore(Arrays.asList("", un));
        noTrailIs.setItemMeta(noTrailIm);
        inv.setItem(36, noTrailIs);

        for(int i = 0; i < Traily.getNames().length; i++)
        {
            ItemStack is = new ItemStack(Material.TIPPED_ARROW, 1);
            ItemMeta im = is.getItemMeta();
            im.setDisplayName(Traily.getNames()[i]);

            if(Main.get().getHra().findClovek(player).getTrail() == Traily.getParticles()[i])
            {
                im.setLore(Arrays.asList("", se));
                im.addEnchant(Enchantment.LUCK, 1, true);
            }
            else
                im.setLore(Arrays.asList("",
                    player.hasPermission("murder.trails."+Traily.getPerm()[i]) ? un : noUn));

            ((PotionMeta) im).setColor(Color.BLACK);
            is.setItemMeta(im);
            inv.setItem(37+i, is);
        }

        return inv;
    }

    static ItemStack craftItem(Material m, Player player)
    {
        String nazov = "";

        switch (m)
        {
            case WOOD_SWORD: nazov = "Dřevěný meč"; break;
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

    public static Inventory buildCompassInv()
    {
        Inventory inv = Bukkit.createInventory(null, 18, ChatColor.AQUA+"HRÁČI");

        for(Clovek c : Main.get().getHra().getAlive())
        {
            ItemStack is = new ItemStack(Material.SKULL_ITEM, 1);
            is.setDurability((short) 3);
            SkullMeta sm = (SkullMeta) is.getItemMeta();
            sm.setOwner(c.getPlayer().getDisplayName());
            sm.setDisplayName(c.getPlayer().getDisplayName());
            is.setItemMeta(sm);
            inv.addItem(is);
        }

        return inv;
    }
}
