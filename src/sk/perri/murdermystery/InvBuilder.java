package sk.perri.murdermystery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import sk.perri.murdermystery.game.Clovek;
import sk.perri.murdermystery.game.Ludia;

class InvBuilder
{
  private InvBuilder() {}
  
  static Inventory buildKosmeticInv(Player player)
  {
    Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "KOSMETIK MENU");
    inv.setItem(11, craftItem(1, Material.WOOD_SWORD, player));
    inv.setItem(12, craftItem(2, Material.STONE_SWORD, player));
    inv.setItem(13, craftItem(3, Material.IRON_SWORD, player));
    inv.setItem(14, craftItem(4, Material.GOLD_SWORD, player));
    inv.setItem(15, craftItem(5, Material.DIAMOND_SWORD, player));
    

    String noUn = ChatColor.RED + "Tento trail nemáš odomčen!";
    String un = ChatColor.GREEN + "Klikni pro výběr!";
    String se = ChatColor.GOLD + "Vybrán!";
    
    ItemStack noTrailIs = new ItemStack(Material.WOOL, 1, (short)14);
    ItemMeta noTrailIm = noTrailIs.getItemMeta();
    noTrailIm.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Bez trailu");
    
    if (Ludia.getClovek(player).getTrail() == null)
    {
      noTrailIm.setLore(Arrays.asList("", se));
      noTrailIm.addEnchant(Enchantment.LUCK, 1, true);
    }
    else {
      noTrailIm.setLore(Arrays.asList("", un)); }
    noTrailIs.setItemMeta(noTrailIm);
    inv.setItem(36, noTrailIs);
    
    for (int i = 0; i < sk.perri.murdermystery.enums.Traily.getNames().length; i++)
    {
      ItemStack is = new ItemStack(Material.TIPPED_ARROW, 1);
      ItemMeta im = is.getItemMeta();
      im.setDisplayName(sk.perri.murdermystery.enums.Traily.getNames()[i]);
      
      if (Ludia.getClovek(player).getTrail() == sk.perri.murdermystery.enums.Traily.getParticles()[i])
      {
        im.setLore(Arrays.asList("", se));
        im.addEnchant(Enchantment.LUCK, 1, true);


      }
      else if (player.hasPermission("murder.trails." + sk.perri.murdermystery.enums.Traily.getPerm()[i]))
      {
        im.setLore(Arrays.asList("", un ));
        ((PotionMeta)im).setColor(Color.GREEN);
      }
      else
      {
        im.setLore(Arrays.asList("", noUn ));
        ((PotionMeta)im).setColor(Color.RED);
      }
      
      is.setItemMeta(im);
      inv.setItem(37 + i, is);
    }
    
    return inv;
  }
  
  private static ItemStack craftItem(int sw, Material m, Player player)
  {
    String nazov = "";
    
    switch (sw)
    {
    case 1: nazov = "Dřevěný meč"; break;
    case 2:  nazov = "Kamenný meč"; break;
    case 3:  nazov = "Železný meč"; break;
    case 4:  nazov = "Zlatý meč"; break;
    case 5:  nazov = "Diamantový meč";
    }
    
    ChatColor chc = ChatColor.RED;
    List<String> lore = new ArrayList<>();
    lore.add("");
    lore.add(ChatColor.RED + "Tento meč nemáš odomčen!");
    
    if ((m == Material.IRON_SWORD) || (player.hasPermission("murder.swords." + m.name().toLowerCase())))
    {
      chc = ChatColor.GREEN;
      lore.set(1, ChatColor.GREEN + "Klikni pro výběr!");
    }
    
    ItemStack is = new ItemStack(m, 1);
    ItemMeta im = is.getItemMeta();
    im.setDisplayName(chc + nazov);
    
    if (Ludia.getClovek(player).getSword() == m)
    {
      im.addEnchant(Enchantment.LUCK, 1, true);
      lore.set(1, ChatColor.GOLD + "Vybrán!");
    }
    
    im.setLore(lore);
    is.setItemMeta(im);
    
    return is;
  }
  
  static Inventory buildCompassInv()
  {
    Inventory inv = Bukkit.createInventory(null, 18, ChatColor.AQUA + "HRÁČI");
    
    for (Clovek c : Ludia.getVsetci().values())
    {
      if (c.isAlive())
      {

        ItemStack is = new ItemStack(Material.SKULL_ITEM, 1);
        is.setDurability((short)3);
        SkullMeta sm = (SkullMeta)is.getItemMeta();
        sm.setOwner(c.getPlayer().getDisplayName());
        sm.setDisplayName(c.getPlayer().getDisplayName());
        is.setItemMeta(sm);
        inv.addItem(is);
      }
    }
    return inv;
  }
  
  static Inventory buildBetInv(Clovek cl)
  {
    Inventory inv = Bukkit.createInventory(null, 36, "§4§lTIP NA VRAHA");
    
    ItemStack a = new ItemStack(Material.PAPER, 1);
    ItemMeta im = a.getItemMeta();
    im.setDisplayName("§2§lPravidlá");
    im.setLore(Arrays.asList("§7Klikni na hlavu hráča o ktorom", "§7si myslíš, že bude vrah"));
    a.setItemMeta(im);
    inv.setItem(0, a);
    
    a = new ItemStack(Material.BARRIER, 1);
    im = a.getItemMeta();
    im.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Zrušiť");
    im.setLore(java.util.Collections.singletonList("§cZruší aktuálny tip!"));
    a.setItemMeta(im);
    inv.setItem(3, a);
    
    a = new ItemStack(Material.IRON_BLOCK, 1);
    im = a.getItemMeta();
    im.setDisplayName("§lIRON");
    im.setLore(Arrays.asList("§7Cena:", "§9§l250 Stylepoints", "", "§7Výhra:", "§9§l500 StylePoints", "§2§l50 Experiences" ));
    a.setItemMeta(im);
    inv.setItem(4, a);
    
    a = new ItemStack(Material.GOLD_BLOCK, 1);
    im = a.getItemMeta();
    im.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "GOLD");
    im.setLore(Arrays.asList("§7Cena:", "§9§l500 Stylepoints", "", "§7Výhra:", "§9§l1000 StylePoints", "§2§l100 Experiences"));
    a.setItemMeta(im);
    inv.setItem(5, a);
    
    a = new ItemStack(Material.DIAMOND_BLOCK, 1);
    im = a.getItemMeta();
    im.setDisplayName("§b§lDIAMOND");
    im.setLore(Arrays.asList("§7Cena:", "§9§l1000 Stylepoints", "§a§l1 StyleKredit", "", "§7Výhra:", "§9§l3000 StylePoints", "§2§l300 Experiences"));
    a.setItemMeta(im);
    inv.setItem(6, a);
    
    a = new ItemStack(Material.EMERALD_BLOCK, 1);
    im = a.getItemMeta();
    im.setDisplayName("§2§lEMERALD");
    im.setLore(Arrays.asList("§7Cena:", "§9§l2000 Stylepoints", "§a§l2 StyleKredity", "", "§7Výhra:", "§9§l8000 StylePoints", "§2§l800 Experiences"));
    
    a.setItemMeta(im);
    inv.setItem(7, a);
    

    ItemStack iss = inv.getItem(3 + cl.getBetPackage());
    ItemMeta ims = iss.getItemMeta();
    ims.addEnchant(Enchantment.LUCK, 1, true);
    ims.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    iss.setItemMeta(ims);
    
    int i = 18;
    for (Clovek c : Ludia.getVsetci().values())
    {
      ItemStack is = new ItemStack(Material.SKULL_ITEM, 1);
      is.setDurability((short)3);
      SkullMeta sm = (SkullMeta)is.getItemMeta();
      sm.setOwner(c.getPlayer().getDisplayName());
      sm.setDisplayName(c.getPlayer().getDisplayName());
      is.setItemMeta(sm);
      inv.setItem(i, is);
      i++;
    }
    
    return inv;
  }
}
