package sk.perri.murdermystery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import sk.perri.murdermystery.game.Gadget;

public class GameMap
{
    private String name = "";
    private String interName = "";
    private FileConfiguration conf;
    private File mapConfigF;
    private List<Location> spawn = new ArrayList<>();
    private List<Location> items = new ArrayList<>();
    private List<Gadget> gadgets = new ArrayList<>();
    private Location lobby;
    private boolean setupMode = true;
    private MainMurder plugin;

    public GameMap(String interName, MainMurder plugin)
    {
        this.interName = interName;
        this.plugin = plugin;


        mapConfigF = new File(plugin.getDataFolder().getPath() + "/" + interName + ".yml");

        if (!mapConfigF.exists())
        {
            try
            {
                if (mapConfigF.createNewFile())
                {
                    plugin.getLogger().info("Config file for map " + interName + " has been created!");
                }
                else
                {
                    plugin.getLogger().warning("Config file for map " + interName + " has been NOT successfully created!");
                }
            }
            catch (IOException e)
            {
                plugin.getLogger().warning("Error while creating config file for map " + interName + ": " + e.toString());
            }

        }
        else
        {
            plugin.getLogger().info("File exist!");
        }

        conf = new org.bukkit.configuration.file.YamlConfiguration();

        try
        {
            conf.load(mapConfigF);
            plugin.getLogger().info("Config file " + interName + ".yml loaded!");
        }
        catch (Exception e)
        {
            plugin.getLogger().warning("Error while loading config file " + interName + ".yml: " + e.toString());
        }

        if (conf.isSet("lobby.x"))
        {
            setupMode = false;
        }
        else
        {
            return;
        }


        lobby = new Location(plugin.getServer().getWorld(interName), conf.getDouble("lobby.x"), conf.getDouble("lobby.y"), conf.getDouble("lobby.z"), (float) conf.getDouble("lobby.yaw"), (float) conf.getDouble("lobby.pitch"));

        if (!conf.isSet("spawn"))
        {
            plugin.getLogger().warning("Neviem nacitat spawn");
            return;
        }

        for (int i = 1; i <= conf.getConfigurationSection("spawn").getKeys(false).size(); i++)
        {
            spawn.add(new Location(plugin.getServer().getWorld(interName), conf
                    .getDouble("spawn." + i + ".x"), conf.getDouble("spawn." + i + ".y"), conf
                    .getDouble("spawn." + i + ".z"), (float) conf.getDouble("spawn." + i + ".yaw"),
                    (float) conf.getDouble("spawn." + i + ".pitch")));
        }

        for (int i = 1; i <= conf.getConfigurationSection("itemlocation").getKeys(false).size(); i++)
        {
            items.add(new Location(plugin.getServer().getWorld(interName), conf
                    .getDouble("itemlocation." + i + ".x"), conf.getDouble("itemlocation." + i + ".y"), conf
                    .getDouble("itemlocation." + i + ".z"), (float) conf.getDouble("itemlocation." + i + ".yaw"),
                    (float) conf.getDouble("itemlocation." + i + ".pitch")));
        }

        if(conf.isSet("gadget"))
        {
            for(int i = 1; i <=  conf.getConfigurationSection("gadget").getKeys(false).size(); i++)
            {
                Location l = new Location(plugin.getServer().getWorld(interName), conf
                        .getDouble("gadget." + i + ".loc.x"), conf.getDouble("gadget." + i + ".loc.y"), conf
                        .getDouble("gadget." + i + ".loc.z"));

                Location l1 = null;

                if(conf.isSet("gadget."+i+".loc1"))
                {
                    l1 = new Location(plugin.getServer().getWorld(interName), conf
                            .getDouble("gadget." + i + ".loc1.x"), conf.getDouble("gadget." + i + ".loc1.y"), conf
                            .getDouble("gadget." + i + ".loc1.z"), (float) conf.getDouble("gadget." + i + ".loc1.yaw"),
                            (float) conf.getDouble("gadget." + i + ".loc1.pitch"));
                }

                Location l2 = null;

                if(conf.isSet("gadget."+i+".loc2"))
                {
                    l2 = new Location(plugin.getServer().getWorld(interName), conf
                            .getDouble("gadget." + i + ".loc2.x"), conf.getDouble("gadget." + i + ".loc2.y"), conf
                            .getDouble("gadget." + i + ".loc2.z"));
                }

                gadgets.add(new Gadget(conf.getInt("gadget."+i+".type"), conf.getInt("gadget."+i+".cost"),
                        l, l1, l2));
            }
        }

        name = conf.getString("name");

        plugin.getServer().getWorld(interName).setDifficulty(org.bukkit.Difficulty.PEACEFUL);
    }

    public boolean inSetup()
    {
        return setupMode;
    }

    public List<String> joinMessage()
    {
        List<String> s = new java.util.ArrayList();

        if (conf.isSet("name"))
        {
            s.add(ChatColor.GOLD + "Vitaj na mape: " + ChatColor.RED + name + ChatColor.GOLD + "!");
        }
        if (conf.isSet("creators"))
        {
            s.add(ChatColor.GOLD + "Mapu vytvoril " + ChatColor.RED + ChatColor.BOLD +
                    String.join(", ", conf.getStringList("creators")));
        }
        return s;
    }

    public Location getLobby()
    {
        return lobby;
    }

    public List<Location> getSpawn()
    {
        return spawn;
    }

    public List<Location> getItems()
    {
        return items;
    }

    public void setLobby(Location loc)
    {
        lobby = loc;

        conf.set("lobby.x", Double.valueOf(loc.getX()));
        conf.set("lobby.y", Double.valueOf(loc.getY() + 1.0D));
        conf.set("lobby.z", Double.valueOf(loc.getZ()));
        conf.set("lobby.yaw", Float.valueOf(loc.getYaw()));
        conf.set("lobby.pitch", Float.valueOf(loc.getPitch()));
    }

    public void addSpawn(Location loc)
    {
        int i = 1;
        if (conf.isSet("spawn.1.x"))
        {
            i = conf.getConfigurationSection("spawn").getKeys(false).size() + 1;
        }
        conf.set("spawn." + i + ".world", loc.getWorld().getName());
        conf.set("spawn." + i + ".x", Double.valueOf(loc.getX()));
        conf.set("spawn." + i + ".y", Double.valueOf(loc.getY() + 1.0D));
        conf.set("spawn." + i + ".z", Double.valueOf(loc.getZ()));
        conf.set("spawn." + i + ".yaw", Float.valueOf(loc.getYaw()));
        conf.set("spawn." + i + ".pitch", Float.valueOf(loc.getPitch()));
    }

    public void addItem(Location loc)
    {
        int i = 1;

        if (conf.isSet("itemlocation.1.x"))
        {
            i = conf.getConfigurationSection("itemlocation").getKeys(false).size() + 1;
        }
        conf.set("itemlocation." + i + ".world", loc.getWorld().getName());
        conf.set("itemlocation." + i + ".x", Double.valueOf(loc.getX()));
        conf.set("itemlocation." + i + ".y", Double.valueOf(loc.getY() + 1.0D));
        conf.set("itemlocation." + i + ".z", Double.valueOf(loc.getZ()));
        conf.set("itemlocation." + i + ".yaw", Float.valueOf(loc.getYaw()));
        conf.set("itemlocation." + i + ".pitch", Float.valueOf(loc.getPitch()));
    }

    public void saveConfig()
    {
        try
        {
            conf.save(mapConfigF);
        }
        catch (IOException e)
        {
            plugin.getLogger().warning("Unable save config file! " + e.toString());
        }
    }

    public void unloadWorld()
    {

    }

    public String getName()
    {
        return name;
    }

    public FileConfiguration getConf()
    {
        return conf;
    }

    public List<Gadget> getGadgets()
    {
        return gadgets;
    }
}
