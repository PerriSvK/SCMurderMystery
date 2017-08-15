package sk.perri.murdermystery;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameMap
{
    private String name = "";
    private String interName = "";
    private FileConfiguration conf;
    private File mapConfigF;
    private List<Location> spawn = new ArrayList<>();
    private List<Location> items = new ArrayList<>();
    private Location lobby;
    private boolean setupMode = true;
    private Main plugin;

    public GameMap(String interName, Main plugin)
    {
        this.interName = interName;
        this.plugin = plugin;

        // Config file

        mapConfigF = new File(plugin.getDataFolder().getPath()+"/"+interName+".yml");

        if(!mapConfigF.exists())
        {
            try
            {
                if(mapConfigF.createNewFile())
                {
                    plugin.getLogger().info("Config file for map "+interName+" has been created!");
                }
                else
                {
                    plugin.getLogger().warning("Config file for map "+interName+" has been NOT successfully created!");
                }
            }
            catch (IOException e)
            {
                plugin.getLogger().warning("Error while creating config file for map "+interName+": "+e.toString());
            }
        }
        else
        {
            plugin.getLogger().info("File exist!");
        }

        conf = new YamlConfiguration();

        try
        {
            conf.load(mapConfigF);
            plugin.getLogger().info("Config file "+interName+".yml loaded!");
        }
        catch (Exception e)
        {
            plugin.getLogger().warning("Error while loading config file "+interName+".yml: "+e.toString());
        }

        if(conf.isSet("lobby.x"))
        {
            setupMode = false;
        }
        else
        {
            return;
        }

        // Load location

        lobby = new Location(plugin.getServer().getWorld(interName), conf.getDouble("lobby.x"),
                conf.getDouble("lobby.y"), conf.getDouble("lobby.z"),
                (float) conf.getDouble("lobby.yaw"), (float)conf.getDouble("lobby.pitch"));

        if(!conf.isSet("spawn"))
        {
            plugin.getLogger().warning("Neviem nacitat spawn");
            return;
        }

        for(int i = 1; i <= conf.getConfigurationSection("spawn").getKeys(false).size(); i++)
        {
            spawn.add(new Location( plugin.getServer().getWorld(interName),
                    conf.getDouble("spawn."+i+".x"), conf.getDouble("spawn."+i+".y"),
                    conf.getDouble("spawn."+i+".z"), (float) conf.getDouble("spawn."+i+".yaw"),
                    (float) conf.getDouble("spawn."+i+".pitch")));
        }

        for(int i = 1; i <= conf.getConfigurationSection("itemlocation").getKeys(false).size(); i++)
        {
            items.add(new Location(plugin.getServer().getWorld(interName),
                    conf.getDouble("itemlocation."+i+".x"), conf.getDouble("itemlocation."+i+".y"),
                    conf.getDouble("itemlocation."+i+".z"), (float) conf.getDouble("itemlocation."+i+".yaw"),
                    (float) conf.getDouble("itemlocation."+i+".pitch")));
        }

        name = conf.getString("name");
    }

    public boolean inSetup()
    {
        return setupMode;
    }

    public List<String> joinMessage()
    {
        List<String> s =  new ArrayList<>();
        
        if(conf.isSet("name"))
            s.add(ChatColor.GOLD+"Vitaj na mape: "+ChatColor.RED+name+ChatColor.GOLD+"!");
        
        if(conf.isSet("creators"))
            s.add(ChatColor.GOLD+"Mapu vytvoril "+ChatColor.RED+ChatColor.BOLD+
                String.join(", ", conf.getStringList("creators")));
        
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
        this.lobby = loc;

        conf.set("lobby.x", loc.getX());
        conf.set("lobby.y", loc.getY()+1);
        conf.set("lobby.z", loc.getZ());
        conf.set("lobby.yaw", loc.getYaw());
        conf.set("lobby.pitch", loc.getPitch());
    }

    public void addSpawn(Location loc)
    {
        int i = 1;
        if(conf.isSet("spawn.1.x"))
            i = conf.getConfigurationSection("spawn").getKeys(false).size() + 1;

        conf.set("spawn."+i+".world", loc.getWorld().getName());
        conf.set("spawn."+i+".x", loc.getX());
        conf.set("spawn."+i+".y", loc.getY()+1);
        conf.set("spawn."+i+".z", loc.getZ());
        conf.set("spawn."+i+".yaw", loc.getYaw());
        conf.set("spawn."+i+".pitch", loc.getPitch());
    }

    public void addItem(Location loc)
    {
        int i = 1;

        if(conf.isSet("itemlocation.1.x"))
            i = conf.getConfigurationSection("itemlocation").getKeys(false).size() + 1;

        conf.set("itemlocation."+i+".world", loc.getWorld().getName());
        conf.set("itemlocation."+i+".x", loc.getX());
        conf.set("itemlocation."+i+".y", loc.getY()+1);
        conf.set("itemlocation."+i+".z", loc.getZ());
        conf.set("itemlocation."+i+".yaw", loc.getYaw());
        conf.set("itemlocation."+i+".pitch", loc.getPitch());
    }

    public void saveConfig()
    {
        try
        {
            conf.save(mapConfigF);
        }
        catch (IOException e)
        {
            plugin.getLogger().warning("Unable save config file! "+e.toString());
        }
    }

    public String getName()
    {
        return name;
    }
}
