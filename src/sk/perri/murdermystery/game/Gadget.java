package sk.perri.murdermystery.game;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sk.perri.murdermystery.MainMurder;

import java.util.*;

public class Gadget
{
    private Location location;
    private int cost;
    private int type; // 0 - potion, 1 - break block, 2 - teleport
    private Location loc1;
    private Location loc2;
    public static Map<Location, Material> oldBlocks = new HashMap<>();

    public Gadget(int type, int cost, Location location, Location loc1, Location loc2)
    {
        this.location = location;
        this.cost = cost;
        this.type = type;
        this.loc1 = loc1;
        this.loc2 = loc2;
    }

    public int getCost() { return cost; }

    public Location getLocation()
    {
        return location;
    }

    public void onClick(Player player)
    {
        try
        {
            if(player.getInventory().getItem(8) != null && player.getInventory().getItem(8).getAmount() >= cost)
            {
                switch(type)
                {
                    case 0: givePotion(player); break;
                    case 2: teleportPlayer(player); break;
                    case 1: breakBlocks(player); break;
                }

                ItemStack is = player.getInventory().getItem(8);

                if(is.getAmount() == cost)
                    is.setType(Material.AIR);
                else
                    is.setAmount(is.getAmount()-cost);

                player.getInventory().setItem(8, is);
            }
            else
            {
                player.sendMessage("§cNemas dostatok zlata!");
            }
        }
        catch(Exception e)
        {
            Bukkit.getLogger().warning("Gadget - onClick Error: "+e.toString());
        }
    }

    private void givePotion(Player player) throws Exception
    {
        ItemStack pot = new ItemStack(Material.POTION, 1);
        ItemMeta im = pot.getItemMeta();
        im.setDisplayName("§d§lMagicky napoj");
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS);
        PotionMeta pm = (PotionMeta) im;

        List<PotionEffect> pe = new ArrayList<>();
        pe.add(new PotionEffect(PotionEffectType.SPEED, 400, 1, false));
        pe.add(new PotionEffect(PotionEffectType.SLOW, 400, 1, false));
        pe.add(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1, false));
        pe.add(new PotionEffect(PotionEffectType.INVISIBILITY, 400, 1, false));
        pe.add(new PotionEffect(PotionEffectType.CONFUSION, 220, 1, false));
        pe.add(new PotionEffect(PotionEffectType.JUMP, 500, 3, false));

        pm.addCustomEffect(pe.get(Random.class.newInstance().nextInt(pe.size())), true);

        pot.setItemMeta(im);

        player.getInventory().addItem(pot);
        player.sendMessage("§7Dostal si §d§lMagicky napoj");
    }

    private void teleportPlayer(Player player) throws Exception
    {
        if(loc1 == null)
            return;

        player.teleport(loc1);
        player.sendMessage("§7Bol si §3§lteleportovany");
    }

    private void breakBlocks(Player player) throws Exception
    {
        player.sendMessage("§7Aktivoval si §a§lpast");
        if(loc1 == null || loc2 == null)
        {
            Bukkit.getLogger().warning("Gadget not configured!");
            return;
        }

        World w = loc1.getWorld();

        Bukkit.getScheduler().runTaskLater(MainMurder.get(), () ->
        {
            for (int x = Math.min(loc1.getBlockX(), loc2.getBlockX()); x <= Math.max(loc1.getBlockX(), loc2.getBlockX()); x++)
            {
                for (int z = Math.min(loc1.getBlockZ(), loc2.getBlockZ()); z <= Math.max(loc1.getBlockZ(), loc2.getBlockZ()); z++)
                {
                    for (int y = Math.min(loc1.getBlockY(), loc2.getBlockY()); y <= Math.max(loc1.getBlockY(), loc2.getBlockY()); y++)
                    {
                        if (w.getBlockAt(x, y, z).getType() == Material.AIR)
                            continue;

                        Block b = w.getBlockAt(x, y, z);
                        oldBlocks.put(b.getLocation(), b.getType());
                        b.setType(Material.AIR);
                    }
                }
            }

            w.playSound(loc1, Sound.BLOCK_PISTON_EXTEND, 100, 1);
            w.playSound(loc2, Sound.BLOCK_PISTON_EXTEND, 100, 1);
        }, 2L);

        Bukkit.getScheduler().runTaskLater(MainMurder.get(), Gadget::reviveBlocks, 502L);
    }

    public static void reviveBlocks()
    {
        try
        {
            if (oldBlocks.size() < 2)
                return;

            oldBlocks.forEach((l, t) -> l.getWorld().getBlockAt(l).setType(t));
            ((Location) oldBlocks.keySet().toArray()[0]).getWorld().playSound((Location) oldBlocks.keySet().toArray()[0],
                    Sound.BLOCK_PISTON_CONTRACT, 100, 1);
            ((Location) oldBlocks.keySet().toArray()[0]).getWorld().playSound((Location) oldBlocks.keySet().toArray()[1],
                    Sound.BLOCK_PISTON_CONTRACT, 100, 1);
            oldBlocks.clear();
        }
        catch (Exception e)
        {
            Bukkit.getLogger().warning("Error revining blocks: "+e.toString());
        }
    }
}
