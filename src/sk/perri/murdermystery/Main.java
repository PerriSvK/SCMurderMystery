package sk.perri.murdermystery;

import com.connorlinfoot.actionbarapi.ActionBarAPI;
import me.mirek.devtools.api.DevTools;
import me.mirek.devtools.api.utils.BungeeAPI;
import me.mirek.devtools.api.utils.TitleAPI;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import sk.perri.murdermystery.commands.Murder;
import sk.perri.murdermystery.commands.Setup;
import sk.perri.murdermystery.enums.DetectiveStatus;
import sk.perri.murdermystery.enums.GameState;
import sk.perri.murdermystery.enums.PlayerType;
import sk.perri.murdermystery.enums.Traily;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Main extends JavaPlugin implements Listener
{
    private static Main plugin;
    private Game hra;
    private Location lobbyLocation = null;
    private ArmorStand swordStand = null;
    private ArmorStand bowStand = null;
    private BukkitTask swordTask = null;
    private BukkitTask moveTask = null;
    private Map<Projectile, Particle> sipi = new HashMap<>();

    private static double SwordCooldown = 0.0;

    public static Main get()
    {
        return plugin;
    }

    @Override
    public void onEnable()
    {
        plugin = this;
        getServer().getPluginManager().registerEvents(this, this);
        removeItems();
        hra = new Game();
        createConfig();
        getLogger().info("Plugin enabled!");
        loadLocations();
        getCommand("setup").setExecutor(new Setup());
        getCommand("murder").setExecutor(new Murder());

        DevTools.registerChat();

        Bukkit.getPluginManager().registerEvents(new PingListener(),this);
        swordTimer();
    }

    private void swordTimer(){
        swordTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            StringBuilder toDisplay = new StringBuilder("§f§lVrhnutí §f - §a");
            if(SwordCooldown > 0){
                SwordCooldown-= 0.1;
                int green = (int) ((4.0 - SwordCooldown) / 0.1);
                int gray = (int) (SwordCooldown / 0.1);
                for(int x = 0;x < green;x++){
                    toDisplay.append(";");
                }
                toDisplay.append("§7");
                for(int x = 0;x < gray;x++){
                    toDisplay.append(";");
                }
                ActionBarAPI.sendActionBar(getHra().getKiller().getPlayer(), toDisplay.toString());
            }
        }, 2L, 2L);
    }

    @Override
    public void onDisable()
    {
        getLogger().info("Plugin disabled!");
    }

    void removeItems()
    {
        Bukkit.getScheduler().runTaskLater(this, () ->
        {
            for (World w : getServer().getWorlds())
            {
                for (Object objectEntity : w.getEntities())
                {
                    Entity localEntity = (Entity) objectEntity;

                    if (localEntity.getType() == EntityType.DROPPED_ITEM || localEntity instanceof Item)
                    {
                        localEntity.remove();
                        continue;
                    }

                    if (localEntity.getType() == EntityType.ARMOR_STAND && localEntity.getCustomName() != null)
                        localEntity.remove();

                    if (localEntity.getType() == EntityType.COW)
                        localEntity.remove();
                }
            }
        }, 60L);
    }

    // Listeners
    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        if(!getConfig().isSet("lobby.x"))
            return;

        // INV
        event.getPlayer().getInventory().clear();
        event.getPlayer().getInventory().setHeldItemSlot(0);

        // BACK TO LOBBY IS
        ItemStack btl = new ItemStack(Material.BED, 1);
        ItemMeta btlim = btl.getItemMeta();
        btlim.setDisplayName(ChatColor.RED+""+ChatColor.BOLD+"ZPĚT DO LOBBY");
        btl.setItemMeta(btlim);
        event.getPlayer().getInventory().setItem(8, btl);

        // DECORATION MENU IS
        ItemStack dm = new ItemStack(Material.BLAZE_POWDER, 1);
        ItemMeta dmim = dm.getItemMeta();
        dmim.setDisplayName(ChatColor.DARK_PURPLE+""+ChatColor.BOLD+"KOSMETIK MENU");
        dm.setItemMeta(dmim);
        event.getPlayer().getInventory().setItem(4, dm);

        // Hide players nickname in TAB
        //event.getPlayer().setPlayerListName(ChatColor.MAGIC+"------");

        hra.addPlayer(event.getPlayer());

        if((hra.getState() != GameState.Lobby) && (hra.getState() != GameState.Starting))
            event.setJoinMessage("");
        else
            event.setJoinMessage(Lang.PLAYER+ChatColor.YELLOW+" "+event.getPlayer().getDisplayName()+" "+Lang.CONNECTED);

        if(hra.getAlive().size() >= getConfig().getInt("maxplayers"))
        {
            hra.start(true);
            return;
        }

        if(hra.getAlive().size() > 1 && hra.getAlive().size() >= getConfig().getInt("minplayers") &&
                hra.getState() == GameState.Lobby)
        {
            getServer().broadcastMessage(Lang.ABLE_TO_START);
            hra.start(false);
        }
        TitleAPI.setTabTitle(event.getPlayer(),"§4§lMurder §f§lMystery\n§7Server: §8" + Bukkit.getServerName(),"§7mc.stylecraft.cz");

        // BETA INFO
        event.getPlayer().sendMessage(ChatColor.GOLD+"Ahoj, táto hra je ešte stále vo vývoji! Môže sa stať, že niečo"+
        " nebude "+ChatColor.RED+"fungovať"+ChatColor.GOLD+" tak, ako by malo. Ak nájdeš chybu napíš prosím Perrimu. Ďakujem.");
    }

    @EventHandler
    public void onSpawn(PlayerSpawnLocationEvent event)
    {
        if(lobbyLocation == null)
            return;

        if(hra.getState() == GameState.Lobby || hra.getState() == GameState.Starting)
            event.setSpawnLocation(lobbyLocation);
        else
            event.setSpawnLocation(hra.getSpectSpawnLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        if(hra.getState() != GameState.End)
            hra.removePlayer(event.getPlayer());

        event.setQuitMessage("");

        if(hra.getState() == GameState.Ingame)
            hra.winCheck();
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event)
    {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickupItem(PlayerPickupItemEvent event)
    {
        if(hra.getState() == GameState.End || hra.findClovek(event.getPlayer()).getType() == PlayerType.Spectator
                || hra.findClovek(event.getPlayer()).getType() == PlayerType.None)
        {
            event.setCancelled(true);
            return;
        }

        if(event.getItem().getItemStack().getType() == Material.GOLD_INGOT)
        {
            Clovek c = hra.findClovek(event.getPlayer());
            if(c == null)
                return;

            int pocet = event.getItem().getItemStack().getAmount();
            if(event.getPlayer().getInventory().getItem(8) != null)
                pocet += event.getPlayer().getInventory().getItem(8).getAmount();

            if(c.getType() == PlayerType.Innocent)
                c.addScore(ScoreTable.ITEM_PICK);

            if((c.getType() == PlayerType.Innocent || c.getType() == PlayerType.Killer) && pocet >= 10)
            {
                event.getPlayer().updateInventory();
                event.getPlayer().getInventory().remove(Material.GOLD_INGOT);
                if(pocet > 10)
                {
                    ItemStack is = new ItemStack(Material.GOLD_INGOT, pocet-10);
                    event.getPlayer().getInventory().setItem(8, is);
                }
                event.getPlayer().getInventory().setItem(2, new ItemStack(Material.BOW, 1));

                int poc = 0;

                if(event.getPlayer().getInventory().getItem(3) != null &&
                        event.getPlayer().getInventory().getItem(3).getType() == Material.ARROW)
                    poc = event.getPlayer().getInventory().getItem(3).getAmount();

                event.getPlayer().getInventory().setItem(3, new ItemStack(Material.ARROW, poc + 1));
            }
            else
            {
                event.getPlayer().getInventory().setItem(8, new ItemStack(Material.GOLD_INGOT, pocet == 0 ? 1 : pocet));
            }

            event.getItem().remove();
            event.setCancelled(true);
            return;
        }
        else if(event.getItem().getItemStack().getType() == Material.BOW)
        {
            if(hra.getDetectiveStatus() != DetectiveStatus.Killed)
            {
                event.getItem().remove();
                event.setCancelled(true);
                return;
            }

            if(hra.findClovek(event.getPlayer()).getType() == PlayerType.Innocent)
            {
                event.getPlayer().getInventory().setItem(1, new ItemStack(Material.BOW, 1));
                event.getPlayer().getInventory().setItem(2, new ItemStack(Material.ARROW, 1));
                event.getItem().remove();
                event.setCancelled(true);
                hra.setDetective(event.getPlayer());
                hra.findClovek(event.getPlayer()).addScore(ScoreTable.WEAP_PICK);
                return;
            }
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event)
    {
        try
        {
            if (event.getClickedInventory().getTitle().contains("KOSMETIK") && event.getCurrentItem().getType() != Material.AIR)
            {
                if(event.getSlot() >= 11 && event.getSlot() <= 15 &&
                        (event.getWhoClicked().hasPermission("murder.swords."+
                        event.getCurrentItem().getType().name().toLowerCase()) ||
                        event.getCurrentItem().getType() == Material.IRON_SWORD))
                    hra.findClovek((Player) event.getWhoClicked()).setSword(event.getCurrentItem().getType());
                else if(event.getSlot() >= 37 && event.getWhoClicked().hasPermission("murder.trails."+
                        Traily.getPerm()[event.getSlot()-37]))
                    hra.findClovek((Player) event.getWhoClicked()).setTrail(Traily.getParticles()[event.getSlot()-37]);
                else if(event.getSlot() == 36)
                    hra.findClovek((Player) event.getWhoClicked()).setTrail(null);

                event.getWhoClicked().closeInventory();
                getServer().getScheduler().runTaskLater(this, () ->
                        event.getWhoClicked().openInventory(InvBuilder.buildKosmeticInv((Player) event.getWhoClicked())),2L);
            }

            if(event.getClickedInventory().getTitle().contains("HRÁČI") && event.getCurrentItem().getType() != Material.AIR)
            {
                if(Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName()) != null &&
                        hra.getAlive().contains(hra.findClovek(Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName()))))
                {
                    event.getWhoClicked().teleport(Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName()).getLocation());
                    event.getWhoClicked().closeInventory();
                }
                else
                {
                    event.getWhoClicked().closeInventory();
                    getServer().getScheduler().runTaskLater(this, () ->
                            event.getWhoClicked().openInventory(InvBuilder.buildCompassInv()) ,2L);
                }
            }
        }
        catch (NullPointerException e)
        {
            getLogger().warning("Null pointer - kosmetik menu");
        }
        finally
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onArrowPickup(PlayerPickupArrowEvent event)
    {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDmg(EntityDamageEvent event)
    {
        if(event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.VOID)
        {
            hra.killPlayer(hra.findClovek((Player) event.getEntity()), true);
            event.getEntity().teleport(hra.getSpawn().get(0));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event)
    {
        hra.killPlayer(hra.findClovek(event.getEntity()), true);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event)
    {
        event.setCancelled(true);

        if(event.getDamager() instanceof Player && event.getEntity() instanceof Player && hra.getState() == GameState.Ingame &&
                isValidWeapon(event.getDamager()))
        {
            hra.killPlayer(getKiller(event.getDamager()), (Player) event.getEntity());
        }

        if(event.getDamager() instanceof Arrow && ((Arrow) event.getDamager()).getShooter() instanceof Player &&
                event.getEntity() instanceof Player && hra.getState() == GameState.Ingame && isValidWeapon(event.getDamager()))
        {
            if(hra.findClovek((Player) event.getEntity()).getType() == PlayerType.Spectator)
            {
                spawnNewArrow((Arrow) event.getDamager(), (Player) event.getEntity());
                return;
            }

            hra.killPlayer(getKiller(event.getDamager()), (Player) event.getEntity());
            event.getDamager().remove();
        }
    }

    @EventHandler
    public void onFall(EntityDamageEvent event)
    {
        if(event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL)
            event.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event)
    {
        if(!event.getPlayer().isOp())
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event)
    {
        if(!event.getPlayer().isOp())
            event.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event)
    {
        event.setCancelled(true);
    }

    @EventHandler
    public void onArmorStandEvent(PlayerArmorStandManipulateEvent event)
    {
        event.setCancelled(true);
    }

    @EventHandler
    public void onItemClick(PlayerInteractEvent event)
    {
        if(event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BED)
        {
            event.setCancelled(true);

            BungeeAPI.sendToLobby(event.getPlayer());
        }

        if(event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BLAZE_POWDER)
        {
            getServer().getScheduler().runTaskLater(this, () ->
                    event.getPlayer().openInventory(InvBuilder.buildKosmeticInv(event.getPlayer())), 2L);
        }

        if(event.getPlayer().getInventory().getItemInMainHand().getType() == Material.COMPASS &&
                (hra.findClovek(event.getPlayer()).getType() == PlayerType.Spectator ||
                hra.findClovek(event.getPlayer()).getType() == PlayerType.None))
        {
            event.getPlayer().openInventory(InvBuilder.buildCompassInv());
        }
    }

    @EventHandler
    public void onSword(final PlayerInteractEvent event)
    {
        if(hra.getState() != GameState.Ingame)
            return;

        if(event.getItem() == null || event.getItem().getType() != hra.getKiller().getSword())
            return;

        if(event.getAction().name().toLowerCase().contains("right") && SwordCooldown <= 0)
        {
            createSword(event.getPlayer());
            SwordCooldown = 4.0;

            //event.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.SHIELD, 1));
            //final int[] holdTime = {1};
            //ActionBarAPI.sendActionBar(event.getPlayer(), "§c§l||||");

            // Time to throw 2 sec = 40 ticks
            /*swordTask = getServer().getScheduler().runTaskTimer(this, () ->
            {
                if(event.getPlayer().isBlocking())
                {
                    String text1 = String.join("", Collections.nCopies(holdTime[0], "§a§l|"))+
                            String.join("", Collections.nCopies(4 - holdTime[0], "§c§l|"));
                    ActionBarAPI.sendActionBar(event.getPlayer(), text1);
                    if(holdTime[0] > 3)
                    {
                        createSword(event.getPlayer());
                        stopSwordCounter(true, event.getPlayer());
                    }

                    holdTime[0]++;
                }
                else
                {
                    stopSwordCounter(false, event.getPlayer());
                }
            }, 5L, 5L);*/
        }

        /*if (event.getAction().name().toLowerCase().contains("right"))
        {
            if (localPlayer.getInventory().getItemInMainHand().getType() == Material.AIR)
            {
                return;
            }
            if (localPlayer.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD)
            {
                event.setCancelled(true);
                if (hra.findClovek(localPlayer).getType() == PlayerType.Killer)
                {
                    final ArmorStand localArmorStand = event.getPlayer().getWorld().spawn(
                            event.getPlayer().getLocation(), ArmorStand.class);

                    localArmorStand.setArms(true);
                    localArmorStand.setBasePlate(false);
                    localArmorStand.setVisible(false);
                    localArmorStand.setGravity(false);
                    localArmorStand.setCustomName("sword");
                    localArmorStand.setCustomNameVisible(false);

                    localArmorStand.setRightArmPose(new EulerAngle(Math.toRadians(0.0D),
                            Math.toRadians(-localPlayer.getLocation().getPitch()), Math.toRadians(90.0D)));
                    localArmorStand.setItemInHand(event.getPlayer().getInventory().getItemInMainHand().clone());

                    Location localLocation = localPlayer.getLocation().clone();

                    final double d1 = localLocation.getX();
                    final double d2 = localLocation.getY();
                    final double d3 = localLocation.getZ();
                    double d4 = Math.toRadians(localLocation.getYaw() + 90.0F);
                    double d5 = Math.toRadians(localLocation.getPitch() + 90.0F);
                    double d6 = Math.sin(d5) * Math.cos(d4);
                    double d7 = Math.sin(d5) * Math.sin(d4);
                    double d8 = Math.cos(d5);

                    //this.Yaw.put(localPlayer, Float.valueOf(localPlayer.getEyeLocation().getYaw()));
                    //this.Pitch.put(localPlayer, Float.valueOf(localPlayer.getEyeLocation().getPitch()));

                    Material[] arrayOfMaterial1 = { Material.AIR, Material.WATER, Material.STATIONARY_WATER,
                            Material.WALL_BANNER, Material.WALL_SIGN, Material.CARPET, Material.CARROT_ITEM,
                            Material.CROPS, Material.DEAD_BUSH, Material.DIODE, Material.DIODE_BLOCK_OFF,
                            Material.DIODE_BLOCK_ON, Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_OFF,
                            Material.TORCH, Material.DOUBLE_PLANT, Material.LONG_GRASS };

                    ArrayList localArrayList = new ArrayList();
                    Material[] arrayOfMaterial2;
                    int i = (arrayOfMaterial2 = arrayOfMaterial1).length;
                    for (int j = 0; j < i; j++)
                    {
                        Material localMaterial = arrayOfMaterial2[j];
                        localArrayList.add(localMaterial);
                    }

                    final double[] a = {0};

                    getServer().getScheduler().runTaskTimer(this, () ->
                    {
                        Location newLocalLocation = new Location(localPlayer.getWorld(), d1 + a[0] * d6,
                                d2 + a[0] * d8, d3 + a[0] * d7);

                        newLocalLocation.setYaw((float) Math.toDegrees(d4)-90.0f);
                        newLocalLocation.setPitch((float) Math.toDegrees(d5)-90.0f);

                        Block localBlock = localPlayer.getEyeLocation().getBlock();
                        localArmorStand.teleport(newLocalLocation);


                        List<Entity> entities = new ArrayList<Entity>();
                        entities.addAll(localArmorStand.getNearbyEntities(2, 2, 2));
                        for (int j = 0; j < entities.size(); j++)
                        {
                            Entity localArena = entities.get(j);
                            if (((localArena instanceof LivingEntity)) &&
                                    (!(localArena instanceof ArmorStand)) &&
                                    ((localArena instanceof Player)))
                            {
                                hra.killPlayer(localPlayer, (Player)localArena);

                            }
                        }
                        a[0] += 0.2;
                    }, 2L, 2L);
                }
            }
        }*/
    }

    @EventHandler
    public void onBowShot(EntityShootBowEvent event)
    {
        if(event.getEntity() instanceof Player)
        {
            Clovek c = hra.findClovek((Player) event.getEntity());
            if(c == null)
                return;

            if(c.getType() == PlayerType.Detective)
            {
                ((Player) event.getEntity()).getInventory().remove(Material.BOW);
                ((Player) event.getEntity()).getInventory().setItem(1, new ItemStack(Material.BOW, 1));
                Bukkit.getScheduler().runTaskLater(this, () ->
                {
                    if (hra.getDetective() != null)
                        hra.getDetective().getPlayer().getInventory().setItem(2, new ItemStack(Material.ARROW, 1));
                }, 100);

                if(c.getTrail() != null)
                    sipi.put((Projectile) event.getProjectile(), c.getTrail());
            }
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event)
    {
        if(sipi.containsKey(event.getEntity()))
            sipi.remove(event.getEntity());
    }

    @EventHandler
    public void onPaintingEvent(HangingBreakEvent event)
    {
        event.setCancelled(true);
    }

    @EventHandler
    public void onSwapEvent(PlayerSwapHandItemsEvent event)
    {
        event.setCancelled(true);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event)
    {
        if(hra.findClovek(event.getPlayer()).getType() == PlayerType.Spectator)
        {
            hra.getAlive().forEach(c -> event.getRecipients().remove(c.getPlayer()));
            String s = event.getMessage();
            event.setMessage(ChatColor.GRAY+s);
        }
    }

    // funkcie
    private boolean isValidWeapon(Entity damager)
    {
        return damager instanceof Arrow && ((Arrow) damager).getShooter() instanceof Player ||
                (damager instanceof Player &&
                ((Player) damager).getInventory().getItemInMainHand().getType() == hra.getKiller().getSword());

    }

    private Player getKiller(Entity damager)
    {
        if(damager instanceof Arrow)
            return (Player) ((Arrow) damager).getShooter();
        else
            return (Player) damager;
    }

    private void createConfig()
    {
        try
        {
            if (!getDataFolder().exists() && getDataFolder().mkdirs())
                getLogger().warning("Neviem vytvorit config priecinok!");

            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists())
            {
                getLogger().info(Lang.CONFIG_CREATED);
                saveDefaultConfig();
            }
            else
                getLogger().info(Lang.CONFIG_LOADED);
        }
        catch (Exception e)
        {
            Main.get().getLogger().warning(Lang.CONFIG_LOAD_ERROR+" "+e.toString());
        }
    }

    private void loadLocations()
    {
        if(!getConfig().isSet("lobby.x"))
            return;

        lobbyLocation = new Location(getServer().getWorld(getConfig().getString("lobby.world")),
                getConfig().getDouble("lobby.x"), getConfig().getDouble("lobby.y"),
                getConfig().getDouble("lobby.z"), (float) getConfig().getDouble("lobby.yaw"),
                (float) getConfig().getDouble("lobby.pitch"));

        for(int i = 1; i <= getConfig().getConfigurationSection("spawn").getKeys(false).size(); i++)
        {
            hra.getSpawn().add(new Location(getServer().getWorld(getConfig().getString("spawn."+i+".world")),
                    getConfig().getDouble("spawn."+i+".x"), getConfig().getDouble("spawn."+i+".y"),
                    getConfig().getDouble("spawn."+i+".z"), (float) getConfig().getDouble("spawn."+i+".yaw"),
                    (float) getConfig().getDouble("spawn."+i+".pitch")));
        }

        for(int i = 1; i <= getConfig().getConfigurationSection("itemlocation").getKeys(false).size(); i++)
        {
            hra.getItemsLocation().add(new Location(getServer().getWorld(getConfig().getString("itemlocation."+i+".world")),
                    getConfig().getDouble("itemlocation."+i+".x"), getConfig().getDouble("itemlocation."+i+".y"),
                    getConfig().getDouble("itemlocation."+i+".z"), (float) getConfig().getDouble("itemlocation."+i+".yaw"),
                    (float) getConfig().getDouble("itemlocation."+i+".pitch")));
        }
    }

    public void setLobbyLocation(Location loc)
    {
        getConfig().set("lobby.world", loc.getWorld().getName());
        getConfig().set("lobby.x", loc.getX());
        getConfig().set("lobby.y", loc.getY()+1);
        getConfig().set("lobby.z", loc.getZ());
        getConfig().set("lobby.yaw", loc.getYaw());
        getConfig().set("lobby.pitch", loc.getPitch());
    }

    public void setSpawnLocation(Location loc)
    {
        int i = 1;
        if(getConfig().isSet("spawn.1.x"))
            i = getConfig().getConfigurationSection("spawn").getKeys(false).size() + 1;

        getConfig().set("spawn."+i+".world", loc.getWorld().getName());
        getConfig().set("spawn."+i+".x", loc.getX());
        getConfig().set("spawn."+i+".y", loc.getY()+1);
        getConfig().set("spawn."+i+".z", loc.getZ());
        getConfig().set("spawn."+i+".yaw", loc.getYaw());
        getConfig().set("spawn."+i+".pitch", loc.getPitch());


    }

    public void setItemLocation(Location loc)
    {
        int i = 1;

        if(getConfig().isSet("itemlocation.1.x"))
            i = getConfig().getConfigurationSection("itemlocation").getKeys(false).size() + 1;

        getConfig().set("itemlocation."+i+".world", loc.getWorld().getName());
        getConfig().set("itemlocation."+i+".x", loc.getX());
        getConfig().set("itemlocation."+i+".y", loc.getY()+1);
        getConfig().set("itemlocation."+i+".z", loc.getZ());
        getConfig().set("itemlocation."+i+".yaw", loc.getYaw());
        getConfig().set("itemlocation."+i+".pitch", loc.getPitch());
    }

    private void spawnNewArrow(Arrow arrow, Player entity)
    {
        entity.setGameMode(GameMode.SPECTATOR);
        Projectile na = entity.launchProjectile(Arrow.class);
        na.setShooter(arrow.getShooter());
        na.setVelocity(arrow.getVelocity());
        na.setBounce(arrow.doesBounce());

        if(sipi.containsKey(arrow))
        {
            Particle p = sipi.get(arrow);
            sipi.put(na, p);
            sipi.remove(arrow);
        }

        arrow.remove();
        getServer().getScheduler().runTaskLater(this, () -> entity.setGameMode(GameMode.ADVENTURE), 3L);
    }

    // TODO DEBUG
    // SWORD THROWING
    private void createSword(Player player)
    {
        if(swordStand != null)
            destroySword();

        double ya = Math.toRadians(player.getLocation().getYaw() + 180.0f);
        swordStand = player.getWorld().spawn(
                player.getLocation().add(-1.0f*Math.cos(ya),
                        0, -1.0f*Math.sin(ya)), ArmorStand.class);

        swordStand.getLocation().setPitch(player.getEyeLocation().getPitch());
        swordStand.getLocation().setYaw(player.getEyeLocation().getYaw());

        swordStand.setArms(true);
        swordStand.setBasePlate(false);
        swordStand.setVisible(false);
        swordStand.setGravity(false);
        swordStand.setCustomName("sword");
        swordStand.setCustomNameVisible(false);

        swordStand.setRightArmPose(new EulerAngle(Math.toRadians(0.0D),
                Math.toRadians(-player.getLocation().getPitch()), Math.toRadians(90.0D)));
        swordStand.setItemInHand(player.getInventory().getItemInMainHand().clone());
        moveSword(player);
    }

    private void moveSword(Player player) // give PerriSvK iron_sword
    {
        Location location = swordStand.getLocation();
        double d4 = Math.toRadians(location.getYaw() + 90.0F);
        double d5 = Math.toRadians(location.getPitch() + 90.0F);
        double d6 = Math.sin(d5) * Math.cos(d4); // X
        double d7 = Math.sin(d5) * Math.sin(d4); // Z
        double d8 = Math.cos(d5); // Y
        int moveTime[] = {0};
        double speed = 1.0;

        moveTask = getServer().getScheduler().runTaskTimer(this, () ->
        {
            //getLogger().info("kkk: ");
            moveTime[0]++;

            if(swordStand == null)
            {
                destroySword();
            }

            location.setX(location.getX() + d6*speed);
            location.setY(location.getY() + d8*speed);
            location.setZ(location.getZ() + d7*speed);

            swordStand.teleport(location);

            checkSword(location, player, moveTime[0] > 5);

        }, 2L, 2L);
    }

    private void checkSword(Location location, Player player, boolean checkBlocks)
    {
        Location uplocation = location.clone();
        uplocation.add(0, 2, 0);
        if(uplocation.getBlock().getType() != Material.AIR) // && checkBlocks)
        {
            getLogger().info("REMOVE SWORD - BLOCK");
            destroySword();
            return;
        }

        for(Entity e : location.getWorld().getNearbyEntities(location, 0.75, 2, 0.75))
        {
            if(e.getType() == EntityType.PLAYER && ! e.getUniqueId().equals(player.getUniqueId()) &&
                    hra.getAlive().contains(hra.findClovek(((Player) e).getPlayer())))
            {
                if(hra.getKiller() != null)
                    hra.killPlayer(hra.getKiller().getPlayer(), (Player) e);

                getLogger().info("REMOVE SWORD - ENTITA");
                destroySword();
                break;
            }
        }
    }

    private void destroySword()
    {
        getLogger().info("destroying");
        getServer().getScheduler().cancelTask(moveTask.getTaskId());
        swordStand.remove();
        moveTask = null;
        swordStand = null;
    }

    private void stopSwordCounter(boolean throwing, Player player)
    {
        getLogger().info("stopping");
        getServer().getScheduler().cancelTask(swordTask.getTaskId());
        player.getInventory().setItemInOffHand(null);

        if(!throwing)
            return;

        player.getInventory().setItemInMainHand(null);
        getServer().getScheduler().runTaskLater(this, () -> player.getInventory().setItem(1,
                new ItemStack(hra.getKiller().getSword(), 1)), 60);
    }

    public Game getHra()
    {
        return hra;
    }
    ArmorStand getBowStand() { return bowStand; }
    void setBowStand(ArmorStand ne) { bowStand = ne; }
    Map<Projectile, Particle> getSipi() { return sipi; }
}

/*
TODO Percentage, DB
 */