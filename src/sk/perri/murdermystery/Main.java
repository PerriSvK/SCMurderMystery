package sk.perri.murdermystery;

import com.connorlinfoot.actionbarapi.ActionBarAPI;

import me.mirek.devtools.api.DevTools;
import me.mirek.devtools.api.currencies.PointsAPI;
import me.mirek.devtools.api.database.DBPool;
import me.mirek.devtools.api.database.Database;
import me.mirek.devtools.api.utils.BungeeAPI;
import me.mirek.devtools.api.utils.TitleAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import sk.perri.murdermystery.commands.MmDebug;
import sk.perri.murdermystery.commands.Murder;
import sk.perri.murdermystery.commands.Setup;
import sk.perri.murdermystery.enums.DetectiveStatus;
import sk.perri.murdermystery.enums.GameState;
import sk.perri.murdermystery.enums.PlayerType;
import sk.perri.murdermystery.enums.Traily;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class Main extends JavaPlugin implements Listener
{
    private static Main plugin;
    private Game hra;
    private ArmorStand swordStand = null;
    private ArmorStand bowStand = null;
    private BukkitTask swordTask = null;
    private BukkitTask moveTask = null;
    private BukkitTask showTask = null;
    private String worldname = "";
    private Map<Projectile, Particle> sipi = new HashMap<>();
    private Database db = DBPool.STATS;
    private Connection conn;
    //private Map<String, Integer> top = new HashMap<>();
    private Vector<String> top = new Vector<>();
    private GameMap gameMap;


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
        getServer().getPluginManager().registerEvents(new PingListener(),this);
        removeItems();
        createConfig();
        worldname = selectWorld();
        getLogger().info("Selecting world: "+worldname);

        /*for(int i = 0; i < getServer().getWorlds().size(); i++)
            getServer().unloadWorld(getServer().getWorlds().get(i), true);*/

        //getServer().getWorlds().clear();

        new WorldCreator(worldname).createWorld();
        getLogger().info("Svety: "+getServer().getWorlds().toString());
        gameMap = new GameMap(worldname, this);
        hra = new Game();

        //loadLocations();
        getCommand("setup").setExecutor(new Setup());
        getCommand("murder").setExecutor(new Murder());
        getCommand("mmdebug").setExecutor(new MmDebug());

        // DB
        db.openConnection();
        conn = db.getConnection();

        DevTools.registerChat();

        swordTimer();
        showPercTimer();

        // TOP PLRS
        try
        {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM murder ORDER BY karma DESC LIMIT 3");
            while(rs.next())
            {
                top.add(rs.getString("name")+" - "+rs.getInt("karma"));
            }

            rs = st.executeQuery("SELECT * FROM murder ORDER BY games DESC LIMIT 3");
            while(rs.next())
            {
                top.add(rs.getString("name")+" - "+rs.getInt("games"));
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            getLogger().info("NO DB!");
        }

        getLogger().info("Plugin enabled!");
    }

    private void showPercTimer()
    {
        showTask = Bukkit.getScheduler().runTaskTimer(this, () ->
        {
            if(hra.getLive() != 0)
            {
                hra.getLudia().forEach((n, cc) -> ActionBarAPI.sendActionBar(cc.getPlayer(),
                    ChatColor.RED+"      Vrah: "+Math.round(cc.getPerk())+"%      "+ChatColor.DARK_AQUA+
                            "Detektiv: "+Math.round(cc.getPerd())+"%"));
            }

        }, 0, 10);
    }

    void stopPercTimer()
    {
        Bukkit.getScheduler().cancelTask(showTask.getTaskId());
    }

    private void swordTimer()
    {
        swordTask = Bukkit.getScheduler().runTaskTimer(this, () ->
        {
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
        db.closeConnection();
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

                    if (localEntity.getType() == EntityType.ARMOR_STAND && (localEntity.getCustomName() != null &&
                        (localEntity.getCustomName().contains("LUK") || localEntity.getCustomName().contains("sword"))))
                        localEntity.remove();

                    if (localEntity.getType() == EntityType.COW)
                        localEntity.remove();

                    if(localEntity.getType() == EntityType.ARROW)
                        localEntity.remove();
                }
            }
        }, 60L);
    }

    // Listeners
    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        if(gameMap.inSetup())
            return;

        // INV
        event.getPlayer().getInventory().clear();
        event.getPlayer().getInventory().setHeldItemSlot(0);

        // BACK TO LOBBY IS
        ItemStack btl = new ItemStack(Material.BED, 1, (byte) 14);
        ItemMeta btlim = btl.getItemMeta();
        btlim.setDisplayName(ChatColor.RED+""+ChatColor.BOLD+"ZPĚT DO LOBBY");
        btl.setItemMeta(btlim);
        event.getPlayer().getInventory().setItem(8, btl);

        // DECORATION MENU IS
        if(hra.getState() == GameState.Lobby || hra.getState() == GameState.Starting)
        {
            ItemStack dm = new ItemStack(Material.BLAZE_POWDER, 1);
            ItemMeta dmim = dm.getItemMeta();
            dmim.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "KOSMETIK MENU");
            dm.setItemMeta(dmim);
            event.getPlayer().getInventory().setItem(4, dm);
        }

        // Hide players nickname in TAB
        //event.getPlayer().setPlayerListName(ChatColor.MAGIC+"------");

        Clovek c = hra.addPlayer(event.getPlayer());

        // DB
        if(hra.getState() == GameState.Lobby || hra.getState() == GameState.Starting)
        {
            try
            {
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT * FROM murder WHERE name = '" + event.getPlayer().getDisplayName() + "';");
                if (rs.next())
                {
                    c.setLdec(rs.getInt("ldet"));
                    c.setLkil(rs.getInt("lkil"));
                    c.setScore(rs.getInt("karma"));
                    c.setGames(rs.getInt("games"));
                    c.setWind(rs.getInt("wind"));
                    c.setWink(rs.getInt("wink"));
                    c.setSword(Material.matchMaterial(rs.getString("sword")));
                    if(rs.getString("trail").equalsIgnoreCase("null"))
                        c.setTrail(null);
                    else
                        c.setTrail(Particle.valueOf(rs.getString("trail")));
                }
                else
                {
                    String sql = "INSERT INTO " +
                            "murder(name, uuid, lkil, ldet, karma, games, sword, trail, wink, wind)" +
                            "VALUES('" + event.getPlayer().getDisplayName() + "', '" + event.getPlayer()
                            .getUniqueId() + "', 0, 0, 0, 0, 'IRON_SWORD', 'NULL', 0, 0);";

                    st.execute(sql);
                    getLogger().info("Player "+event.getPlayer().getDisplayName()+" has connected for first time, "+
                    "creating row in DB!");
                    c.setLdec(0);
                    c.setLkil(0);
                }
                st.close();
                hra.calculatePercentage();
            }
            catch (SQLException e)
            {
                getLogger().warning("SQLError - onJoin e:" + e.getMessage());
            }
            catch (Exception e)
            {
                getLogger().warning("Error with SQL: "+e.toString());
            }
        }

        if((hra.getState() != GameState.Lobby) && (hra.getState() != GameState.Starting))
            event.setJoinMessage("");
        else
            event.setJoinMessage(Lang.PLAYER+ChatColor.RED+" "+event.getPlayer().getDisplayName()+" "+Lang.CONNECTED);

        if(hra.getLudia().size() >= getConfig().getInt("maxplayers"))
        {
            hra.start(true);
            return;
        }

        if(hra.getLudia().size() > 1 && hra.getLudia().size() >= getConfig().getInt("minplayers") &&
                hra.getState() == GameState.Lobby)
        {
            getServer().broadcastMessage(Lang.ABLE_TO_START);
            hra.start(false);
        }
        TitleAPI.setTabTitle(event.getPlayer(),"§4§lMurder §f§lMystery\n§7Server: §8" + Bukkit.getServerName(),"§7mc.stylecraft.cz");

        // BETA INFO
        TextComponent mes = new TextComponent(ChatColor.DARK_PURPLE+"BugTracker");
        mes.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://stylecraft.cz/issue-tracker"));  //.event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://stylecraft.cz/issue-tracker")).create();

        TextComponent aaa = new TextComponent(ChatColor.YELLOW+"Ahoj, táto hra je ešte stále vo vývoji!");
        TextComponent bbb = new TextComponent(ChatColor.YELLOW+"Ak nájdeš chybu napíš ju prosím na ");
        bbb.addExtra(mes);

        event.getPlayer().spigot().sendMessage(aaa);
        event.getPlayer().spigot().sendMessage(bbb);

        gameMap.joinMessage().forEach(sas -> event.getPlayer().sendMessage(sas));
        event.getPlayer().sendMessage(ChatColor.YELLOW+"Pro nápovědu použij /murder help");
    }

    @EventHandler
    public void onSpawn(PlayerSpawnLocationEvent event)
    {
        if(gameMap.getLobby() == null)
        {
            event.setSpawnLocation(getServer().getWorld(worldname).getSpawnLocation());
            return;
        }

        getLogger().info("MAP: "+getServer().getWorld(worldname).toString()+" SPAWN LOCATION: "+gameMap.getLobby().toString());

        if(hra.getState() == GameState.Lobby || hra.getState() == GameState.Starting)
            event.setSpawnLocation(gameMap.getLobby());
        else
            event.setSpawnLocation(gameMap.getSpawn().get(1));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        if(hra.getState() != GameState.End)
            hra.removePlayer(event.getPlayer());

        getServer().getScheduler().runTaskLater(this, () ->
                {
                    if (hra.getState() == GameState.Lobby || hra.getState() == GameState.Starting)
                    {
                        hra.calculatePercentage();
                        hra.getLudia().values().forEach(c -> c.getSBManager().updateLobbyBoard());
                        hra.getLudia().values().forEach(c -> c.getSBManager().updateLobbyBoard());
                    }
                }, 2L);

        event.setQuitMessage("");
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event)
    {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickupItem(PlayerPickupItemEvent event)
    {
        if(hra.getState() == GameState.Setup)
            return;

        if(hra.getState() == GameState.End || !hra.getLudia().containsKey(event.getPlayer().getDisplayName()) ||
                !hra.getLudia().get(event.getPlayer().getDisplayName()).isAlive())
        {
            event.setCancelled(true);
            return;
        }

        if(event.getItem().getItemStack().getType() == Material.GOLD_INGOT)
        {
            Clovek c = hra.getLudia().get(event.getPlayer().getDisplayName());

            int pocet = event.getItem().getItemStack().getAmount();
            if((event.getPlayer()).getInventory().getItem(8) != null)
                pocet += (event.getPlayer()).getInventory().getItem(8).getAmount();

            if(c.getType() == PlayerType.Innocent)
            {
                c.addScore(ScoreTable.ITEM_PICK*event.getItem().getItemStack().getAmount());
                c.getPlayer().sendMessage(ChatColor.GOLD+"+"+
                        (ScoreTable.ITEM_PICK*event.getItem().getItemStack().getAmount())+" za sebraný gold!");
            }

            if((c.getType() == PlayerType.Innocent || c.getType() == PlayerType.Killer) && pocet >= 10)
            {
                (event.getPlayer()).updateInventory();
                (event.getPlayer()).getInventory().remove(Material.GOLD_INGOT);
                if(pocet > 10)
                {
                    ItemStack is = new ItemStack(Material.GOLD_INGOT, pocet-10);
                    (event.getPlayer()).getInventory().setItem(8, is);
                }
                (event.getPlayer()).getInventory().setItem(2, new ItemStack(Material.BOW, 1));

                int poc = 0;

                if((event.getPlayer()).getInventory().getItem(3) != null &&
                        (event.getPlayer()).getInventory().getItem(3).getType() == Material.ARROW)
                    poc = (event.getPlayer()).getInventory().getItem(3).getAmount();

                (event.getPlayer()).getInventory().setItem(3, new ItemStack(Material.ARROW, poc + 1));
            }
            else
            {
                (event.getPlayer()).getInventory().setItem(8, new ItemStack(Material.GOLD_INGOT, pocet == 0 ? 1 : pocet));
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

            if(hra.getLudia().get(event.getPlayer().getDisplayName()).getType() == PlayerType.Innocent)
            {
                if((event.getPlayer()).getInventory().contains(Material.ARROW))
                    (event.getPlayer()).getInventory().remove(Material.ARROW);

                (event.getPlayer()).getInventory().setItem(1, new ItemStack(Material.BOW, 1));
                (event.getPlayer()).getInventory().setItem(2, new ItemStack(Material.ARROW, 1));
                event.getItem().remove();
                event.setCancelled(true);
                hra.setDetective(event.getPlayer());
                hra.getLudia().get(event.getPlayer().getDisplayName()).addScore(ScoreTable.WEAP_PICK);
                event.getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.WEAP_PICK+" za sebraný luk!");
                return;
            }
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event)
    {
        if(hra.getState() == GameState.Setup)
            return;

        try
        {
            if (event.getClickedInventory().getTitle().contains("KOSMETIK") && event.getCurrentItem().getType() != Material.AIR)
            {
                if(event.getSlot() >= 11 && event.getSlot() <= 15 &&
                        (event.getWhoClicked().hasPermission("murder.swords."+
                        event.getCurrentItem().getType().name().toLowerCase()) ||
                        event.getCurrentItem().getType() == Material.IRON_SWORD))
                    hra.getLudia().get(((Player) event.getWhoClicked()).getDisplayName()).setSword(event.getCurrentItem().getType());
                else if(event.getSlot() >= 37 && event.getWhoClicked().hasPermission("murder.trails."+
                        Traily.getPerm()[event.getSlot()-37]))
                    hra.getLudia().get(((Player) event.getWhoClicked()).getDisplayName()).setTrail(Traily.getParticles()[event.getSlot()-37]);
                else if(event.getSlot() == 36)
                    hra.getLudia().get(((Player) event.getWhoClicked()).getDisplayName()).setTrail(null);

                event.getWhoClicked().closeInventory();
                getServer().getScheduler().runTaskLater(this, () ->
                        event.getWhoClicked().openInventory(InvBuilder.buildKosmeticInv((Player) event.getWhoClicked())),2L);
            }

            if(event.getClickedInventory().getTitle().contains("HRÁČI") && event.getCurrentItem().getType() != Material.AIR)
            {
                if(Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName()) != null &&
                        hra.getLudia().containsKey(Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName()).getDisplayName()))
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
            hra.killPlayer(hra.getLudia().get(((Player) event.getEntity()).getDisplayName()), true);
            event.getEntity().teleport(gameMap.getSpawn().get(0));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event)
    {
        if(hra.getLudia().containsKey(event.getEntity().getDisplayName()) && hra.getLudia().get(event.getEntity().getDisplayName()).isAlive())
        {
            hra.killPlayer(hra.getLudia().get(event.getEntity().getDisplayName()), true);
        }
        else
        {
            event.setKeepInventory(true);
        }
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
            if(!hra.getLudia().get(((Player) event.getEntity()).getDisplayName()).isAlive())
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
        if(hra.getState() == GameState.Setup)
            return;

        if(!event.getPlayer().isOp())
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event)
    {
        if(hra.getState() == GameState.Setup)
            return;

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

    // funkcia na spravny klik
    private boolean isClick(Action action)
    {
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK ||
                action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    @EventHandler
    public void onItemClick(PlayerInteractEvent event)
    {
        if(hra.getState() == GameState.Setup)
            return;

        if(isClick(event.getAction()) && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BED)
        {
            event.setCancelled(true);

            BungeeAPI.sendToLobby(event.getPlayer());
            return;
        }

        if(isClick(event.getAction()) && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BLAZE_POWDER)
        {
            getServer().getScheduler().runTaskLater(this, () ->
                    event.getPlayer().openInventory(InvBuilder.buildKosmeticInv(event.getPlayer())), 2L);
            return;
        }

        if(isClick(event.getAction()) && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.COMPASS &&
                (!hra.getLudia().get(event.getPlayer().getDisplayName()).isAlive() ||
                hra.getLudia().get(event.getPlayer().getDisplayName()).getType() == PlayerType.None))
        {
            event.getPlayer().openInventory(InvBuilder.buildCompassInv());
            return;
        }

        if(event.getAction() == Action.PHYSICAL && event.getClickedBlock().getType() == Material.SOIL)
        {
            event.setCancelled(true);
            event.getClickedBlock().setType(Material.SOIL);
        }

        if(!hra.getLudia().containsKey(event.getPlayer().getDisplayName()))
            event.setCancelled(true);
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
    }

    @EventHandler
    public void onBowShot(EntityShootBowEvent event)
    {
        if(event.getEntity() instanceof Player)
        {
            Clovek c = hra.getLudia().get(((Player) event.getEntity()).getDisplayName());
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
    public void onCommand(PlayerCommandPreprocessEvent event)
    {
        String ass = event.getMessage().split(" ")[0];
        if(ass.equalsIgnoreCase("/msg") || ass.equalsIgnoreCase("/pm") ||
                ass.equalsIgnoreCase("/tell") || ass.equalsIgnoreCase("/m") || ass.equalsIgnoreCase("/r"))
        {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED+"Súkromné správy sa tu neposielaju!");
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event)
    {
        if(!hra.getLudia().get(event.getPlayer().getDisplayName()).isAlive() && hra.getState() != GameState.End)
        {
            hra.getLudia().values().forEach(c -> event.getRecipients().remove(c.getPlayer()));
            String s = event.getMessage();
            event.setMessage(ChatColor.GRAY+s);
        }

        if(event.getMessage().equalsIgnoreCase("gg") && hra.getState() == GameState.End &&
                hra.getLudia().containsKey(event.getPlayer().getDisplayName()) &&
                !hra.getLudia().get(event.getPlayer().getDisplayName()).getGG())
        {
            hra.getLudia().get(event.getPlayer().getDisplayName()).gg();
            PointsAPI.addPoints(event.getPlayer(), 20);
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
                getLogger().info(Lang.CONFIG_CREATED);
            else
                getLogger().info(Lang.CONFIG_LOADED);

            //saveDefaultConfig();
        }
        catch (Exception e)
        {
            Main.get().getLogger().warning(Lang.CONFIG_LOAD_ERROR+" "+e.toString());
        }
    }

    private String selectWorld()
    {
        if(!getConfig().isSet("maps"))
        {
            if(!getConfig().isSet("lobby.world"))
                return "";
            else
                return getConfig().getString("lobby.world");
        }

        List<String> maps = getConfig().getStringList("maps");
        getLogger().info("Available maps: "+maps);

        Random r = new Random();
        return (String) maps.toArray()[r.nextInt(maps.size())];
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
        double speed = 3.0;

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
                    hra.getLudia().containsKey(((Player) e).getPlayer().getDisplayName()))
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
    ArmorStand getSwordStand() { return swordStand; }
    void setBowStand(ArmorStand ne) { bowStand = ne; }
    Map<Projectile, Particle> getSipi() { return sipi; }
    public Vector<String> getTop() { return top; }

    Connection getConn()
    {
        return conn;
    }
    Database getDb()
    {
        return db;
    }

    public GameMap getMap()
    {
        return gameMap;
    }

    public void setLobbyLocation(Location l)
    {
        gameMap.setLobby(l);
    }

    public void setSpawnLocation(Location l)
    {
        gameMap.addSpawn(l);
    }

    public void setItemLocation(Location l)
    {
        gameMap.addItem(l);
    }
}