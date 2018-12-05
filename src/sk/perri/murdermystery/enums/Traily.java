package sk.perri.murdermystery.enums;

import org.bukkit.ChatColor;
import org.bukkit.Particle;

public class Traily
{
  private static String[] nazvy = { ChatColor.RED + "" + ChatColor.BOLD + "Srdíčka", ChatColor.GREEN + "" + ChatColor.BOLD + "Kroužky", ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Hvězdičky", ChatColor.RED + "" + ChatColor.BOLD + "Plamínky", ChatColor.RED + "" + ChatColor.BOLD + "Láva", ChatColor.BLUE + "" + ChatColor.BOLD + "Kuličky", ChatColor.GREEN + "" + ChatColor.BOLD + "Noty", ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "Černá srdíčka", ChatColor.GREEN + "" + ChatColor.BOLD + "Slime", ChatColor.AQUA + "" + ChatColor.BOLD + "Snehové koule", ChatColor.DARK_RED + "" + ChatColor.BOLD + "Brambory", ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Zelené hvězdy", ChatColor.RED + "" + ChatColor.BOLD + "Redstone", ChatColor.BLUE + "" + ChatColor.BOLD + "Voda", ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Bublinky", ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prskalky", ChatColor.BLUE + "" + ChatColor.BOLD + "Vločky" };
  





  public Traily() {}
  




  public static String[] getNames() { return nazvy; }
  
  private static String[] perm = { "heart", "spell", "witch", "flame", "lava", "totem", "note", "dmg", "slime", "snowball", "angry", "happy", "redstone", "water", "crit", "rod", "explo" };
  




  public static String[] getPerm() { return perm; }
  
  private static Particle[] particles = { Particle.HEART, Particle.SPELL_MOB, Particle.SPELL_WITCH, Particle.FLAME, Particle.LAVA, Particle.TOTEM, Particle.NOTE, Particle.DAMAGE_INDICATOR, Particle.SLIME, Particle.SNOWBALL, Particle.VILLAGER_ANGRY, Particle.VILLAGER_HAPPY, Particle.REDSTONE, Particle.WATER_DROP, Particle.CRIT_MAGIC, Particle.END_ROD, Particle.EXPLOSION_NORMAL };
  




  public static Particle[] getParticles()
  {
    return particles;
  }
}
