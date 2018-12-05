package sk.perri.murdermystery;

import com.connorlinfoot.actionbarapi.ActionBarAPI;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import me.mirek.devtools.api.DevTools;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.WorldCreator;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import sk.perri.murdermystery.commands.GadgetCommand;
import sk.perri.murdermystery.commands.MmDebug;
import sk.perri.murdermystery.commands.Murder;
import sk.perri.murdermystery.commands.Setup;
import sk.perri.murdermystery.enums.BetPackage;
import sk.perri.murdermystery.enums.DetectiveStatus;
import sk.perri.murdermystery.enums.GameState;
import sk.perri.murdermystery.enums.PlayerType;
import sk.perri.murdermystery.enums.Traily;
import sk.perri.murdermystery.game.Clovek;
import sk.perri.murdermystery.game.Ludia;
import sk.perri.murdermystery.utils.DBExecutor;

public class MainMurder extends JavaPlugin implements Listener
{
    private static MainMurder plugin;
    private Game hra;
    private ArmorStand swordStand = null;
    private ArmorStand bowStand = null;
    private BukkitTask moveTask = null;
    private BukkitTask showTask = null;
    private String worldname = "";
    private Map<Projectile, Particle> sipi = new HashMap<>();
    private DBExecutor dbe;
    private Vector<String> top = new Vector<>();
    private Vector<String> ggs = new Vector<>();
    private GameMap gameMap;
    private static double SwordCooldown = 0.0D;

    public MainMurder() {
    }

    public static MainMurder get() {
        return plugin;
    }

    public void onEnable()
    {
        plugin = this;

        GadgetCommand gc = new GadgetCommand();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new PingListener(), this);
        getServer().getPluginManager().registerEvents(gc, this);

        createConfig();

        worldname = selectWorld();
        getLogger().info("Selecting world: " + worldname);
        (new WorldCreator(worldname)).createWorld();
        gameMap = new GameMap(worldname, this);

        hra = new Game();

        getCommand("setup").setExecutor(new Setup());
        getCommand("murder").setExecutor(new Murder());
        getCommand("mmdebug").setExecutor(new MmDebug());
        getCommand("gadget").setExecutor(gc);

        dbe = new DBExecutor();

        DevTools.registerChat();
        DevTools.getPermissions().enableUserPermissions();
        DevTools.getMinigameAPI().setTabTitle("MurderMystery", ChatColor.RED);
        DevTools.getJoinManager().enableOnlyProxyJoin();

        loadGadgets();

        swordTimer();
        showPercTimer();

        top = dbe.getTOP();

        getServer().getScheduler().runTaskTimer(this, () ->
        {
            if (getServer().getOnlinePlayers().size() == 0 && hra.getState() == GameState.Lobby)
            {
                dbe.aliveRequest();
                getLogger().info("DB Alive request send!");
            }

        }, 24000L, 24000L);

        getLogger().info("Plugin enabled!");
    }

    private void showPercTimer()
    {
        showTask = Bukkit.getScheduler().runTaskTimer(this, () ->
        {
            if (hra.getLive() != 0)
            {
                Ludia.getVsetci().forEach((n, cc) ->
                    ActionBarAPI.sendActionBar(cc.getPlayer(),
                            ChatColor.RED + "      Vrah: " + Math.round(cc.getPerk()) + "%      " +
                                    ChatColor.DARK_AQUA + "Detektiv: " + Math.round(cc.getPerd()) + "%"));
            }
        }, 0L, 10L);
    }

    void stopPercTimer() {
        Bukkit.getScheduler().cancelTask(showTask.getTaskId());
    }

    private void swordTimer()
    {
        Bukkit.getScheduler().runTaskTimer(this, () ->
        {
            StringBuilder toDisplay = new StringBuilder("§f§lVrhnutí §f - §a");
            if (SwordCooldown > 0.0D)
            {
                SwordCooldown -= 0.1D;
                int green = (int)((4.0D - SwordCooldown) / 0.1D);
                int gray = (int)(SwordCooldown / 0.1D);

                int x;
                for(x = 0; x < green; ++x)
                {
                    toDisplay.append(";");
                }

                toDisplay.append("§7");

                for(x = 0; x < gray; ++x)
                {
                    toDisplay.append(";");
                }

                ActionBarAPI.sendActionBar(Ludia.getVrah().getPlayer(), toDisplay.toString());
            }
        }, 2L, 2L);
    }

    public void onDisable()
    {

        getServer().unloadWorld(worldname, false);
        dbe.close();
        getLogger().info("Plugin disabled!");
    }

    void removeItems()
    {
        Bukkit.getScheduler().runTaskLater(this, () ->
            gameMap.getLobby().getWorld().getEntities().forEach(e ->
            {
                if (e.getType() != EntityType.DROPPED_ITEM && !(e instanceof Item))
                {
                    if (e.getType() == EntityType.ARMOR_STAND && e.getCustomName() != null &&
                            (e.getCustomName().contains("LUK") || e.getCustomName().contains("sword")))
                    {
                        e.remove();
                    }

                    if (e.getType() == EntityType.COW)
                    {
                        e.remove();
                    }

                    if (e.getType() == EntityType.ARROW)
                    {
                        e.remove();
                    }
                }
                else
                {
                    e.remove();
                }
            }), 60L);
    }

    private void loadGadgets()
    {

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        if (!gameMap.inSetup())
        {
            event.getPlayer().getInventory().clear();
            event.getPlayer().getInventory().setHeldItemSlot(0);

            ItemStack btl = new ItemStack(Material.BED, 1, (short)14);
            ItemMeta btlim = btl.getItemMeta();
            btlim.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "ZPĚT DO LOBBY");
            btl.setItemMeta(btlim);
            event.getPlayer().getInventory().setItem(8, btl);

            if (hra.getState() == GameState.Lobby || hra.getState() == GameState.Starting)
            {
                ItemStack dm = new ItemStack(Material.BLAZE_POWDER, 1);
                ItemMeta dmim = dm.getItemMeta();
                dmim.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "KOSMETIK MENU");
                dm.setItemMeta(dmim);
                event.getPlayer().getInventory().setItem(4, dm);

                ItemStack bm = new ItemStack(Material.DOUBLE_PLANT, 1);
                ItemMeta bmim = bm.getItemMeta();
                bmim.setDisplayName("§2§lTIP NA VRAHA");
                bm.setItemMeta(bmim);
                event.getPlayer().getInventory().setItem(1, bm);
            }

            Clovek c = hra.addPlayer(event.getPlayer());
            if (hra.getState() == GameState.Lobby || hra.getState() == GameState.Starting)
            {
                dbe.playerDB(event.getPlayer(), c);
            }

            if (hra.getState() != GameState.Lobby && hra.getState() != GameState.Starting)
            {
                event.setJoinMessage("");
            }
            else
            {
                event.setJoinMessage(Lang.PLAYER + ChatColor.RED + " " + event.getPlayer().getDisplayName() + " " + Lang.CONNECTED);
            }

            if ((hra.getState() == GameState.Lobby || hra.getState() == GameState.Starting) &&
                    Ludia.pocet() >= getConfig().getInt("maxplayers"))
            {
                hra.start(true);
            }
            else if (Ludia.pocet() > 1 && Ludia.pocet() >= getConfig().getInt("minplayers") &&
                    hra.getState() == GameState.Lobby)
            {
                getServer().broadcastMessage(Lang.ABLE_TO_START);
                hra.start(false);
            }

            //TitleAPI.setTabTitle(event.getPlayer(), "§4§lMurder §f§lMystery\n§7Server: §8" + Bukkit.getServerName(), "§7mc.stylecraft.cz");

            TextComponent mes = new TextComponent(ChatColor.DARK_PURPLE + "BugTracker");
            mes.setClickEvent(new ClickEvent(Action.OPEN_URL, "https://stylecraft.cz/issue-tracker"));
            TextComponent bbb = new TextComponent(ChatColor.YELLOW + "Ak nájdeš chybu napíš ju prosím na ");
            bbb.addExtra(mes);
            event.getPlayer().spigot().sendMessage(bbb);
            event.getPlayer().sendMessage("§eAlebo na §9perri@stylecraft.cz");

            gameMap.joinMessage().forEach((sas) -> event.getPlayer().sendMessage(sas));
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Pro nápovědu použij /murder help");
        }
    }

    @EventHandler
    public void onSpawn(PlayerSpawnLocationEvent event)
    {
        if (gameMap.getLobby() == null)
        {
            event.setSpawnLocation(getServer().getWorld(worldname).getSpawnLocation());
        }
        else
        {
            getLogger().info("MAP: " + getServer().getWorld(worldname).toString()
                    + " SPAWN LOCATION: " + gameMap.getLobby().toString());
            if(hra.getState() != GameState.Lobby && hra.getState() != GameState.Starting)
            {
                event.setSpawnLocation(gameMap.getSpawn().get(1));
            }
            else
            {
                event.setSpawnLocation(gameMap.getLobby());
            }

        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        if (hra.getState() != GameState.End)
        {
            hra.removePlayer(event.getPlayer());
        }

        getServer().getScheduler().runTaskLater(this, () ->
        {
            if (hra.getState() == GameState.Lobby || hra.getState() == GameState.Starting)
            {
                hra.calculatePercentage();
                Ludia.getVsetci().values().forEach((c) -> c.getSBManager().updateLobbyBoard());
                Ludia.getVsetci().values().forEach((c) -> c.getSBManager().updateLobbyBoard());
            }
        }, 2L);
        event.setQuitMessage("");
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickupItem(PlayerPickupItemEvent event) {
        if (hra.getState() != GameState.Setup) {
            if (hra.getState() != GameState.End && Ludia.getVsetci().containsKey(event.getPlayer()) &&
                    (Ludia.getVsetci().get(event.getPlayer())).isAlive())
            {
                if (event.getItem().getItemStack().getType() != Material.GOLD_INGOT)
                {
                    if (event.getItem().getItemStack().getType() == Material.BOW)
                    {
                        if (hra.getDetectiveStatus() != DetectiveStatus.Killed)
                        {
                            event.getItem().remove();
                            event.setCancelled(true);
                            return;
                        }

                        if (Ludia.getClovek(event.getPlayer()).getType() == PlayerType.Innocent)
                        {
                            if (event.getPlayer().getInventory().contains(Material.ARROW))
                            {
                                event.getPlayer().getInventory().remove(Material.ARROW);
                            }

                            event.getPlayer().getInventory().setItem(1, new ItemStack(Material.BOW, 1));
                            event.getPlayer().getInventory().setItem(2, new ItemStack(Material.ARROW, 1));
                            event.getItem().remove();
                            event.setCancelled(true);
                            Ludia.setBowOwner(Ludia.getClovek(event.getPlayer()));
                            Ludia.getVsetci().get(event.getPlayer()).addScore(ScoreTable.WEAP_PICK);
                            event.getPlayer().sendMessage(ChatColor.GOLD + "+" + ScoreTable.WEAP_PICK + " za sebraný luk!");
                            hra.removeCompass();
                            return;
                        }
                    }

                    event.setCancelled(true);
                }
                else
                {
                    Clovek c = Ludia.getVsetci().get(event.getPlayer());
                    int pocet = event.getItem().getItemStack().getAmount();
                    if (event.getPlayer().getInventory().getItem(8) != null)
                    {
                        pocet += event.getPlayer().getInventory().getItem(8).getAmount();
                    }

                    if (c.getType() == PlayerType.Innocent) {
                        c.addScore(ScoreTable.ITEM_PICK * event.getItem().getItemStack().getAmount());
                        c.getPlayer().sendMessage(ChatColor.GOLD + "+" + ScoreTable.ITEM_PICK * event.getItem().getItemStack().getAmount() + " za sebraný gold!");
                    }

                    if ((c.getType() == PlayerType.Innocent || c.getType() == PlayerType.Killer) && pocet >= 10) {
                        event.getPlayer().updateInventory();
                        event.getPlayer().getInventory().remove(Material.GOLD_INGOT);
                        if (pocet > 10) {
                            ItemStack is = new ItemStack(Material.GOLD_INGOT, pocet - 10);
                            event.getPlayer().getInventory().setItem(8, is);
                        }

                        event.getPlayer().getInventory().setItem(2, new ItemStack(Material.BOW, 1));
                        int poc = 0;
                        if (event.getPlayer().getInventory().getItem(3) != null && event.getPlayer().getInventory().getItem(3).getType() == Material.ARROW) {
                            poc = event.getPlayer().getInventory().getItem(3).getAmount();
                        }

                        event.getPlayer().getInventory().setItem(3, new ItemStack(Material.ARROW, poc + 1));
                    } else {
                        event.getPlayer().getInventory().setItem(8, new ItemStack(Material.GOLD_INGOT, pocet == 0 ? 1 : pocet));
                    }

                    event.getItem().remove();
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event)
    {
        if (hra.getState() != GameState.Setup || !(event.getWhoClicked() instanceof Player))
        {
            try
            {
                Player pll = (Player) event.getWhoClicked();

                if (event.getClickedInventory().getTitle().contains("KOSMETIK") &&
                        event.getCurrentItem().getType() != Material.AIR)
                {
                    if (event.getSlot() < 11 || event.getSlot() > 15 || !pll.hasPermission("murder.swords." +
                            event.getCurrentItem().getType().name().toLowerCase()) &&
                            event.getCurrentItem().getType() != Material.IRON_SWORD)
                    {
                        if (event.getSlot() >= 37 &&
                                pll.hasPermission("murder.trails." + Traily.getPerm()[event.getSlot() - 37]))
                        {
                            (Ludia.getVsetci().get(pll)).setTrail(Traily.getParticles()[event.getSlot() - 37]);
                        }
                        else if (event.getSlot() == 36)
                        {
                            (Ludia.getVsetci().get(pll)).setTrail(null);
                        }
                    }
                    else
                    {
                        (Ludia.getVsetci().get(pll)).setSword(event.getCurrentItem().getType());
                    }

                    pll.closeInventory();
                    getServer().getScheduler().runTaskLater(this, () ->
                            pll.openInventory(InvBuilder.buildKosmeticInv(pll)), 2L);
                }

                if (event.getClickedInventory().getTitle().contains("HRÁČI") &&
                        event.getCurrentItem().getType() != Material.AIR)
                {
                    if (Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName()) != null &&
                            Ludia.getVsetci().containsKey(Bukkit.getPlayer(
                                    event.getCurrentItem().getItemMeta().getDisplayName())))
                    {
                        pll.teleport(Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName()).getLocation());
                        pll.closeInventory();
                    }
                    else
                    {
                        pll.closeInventory();
                        getServer().getScheduler().runTaskLater(this, () ->
                            pll.openInventory(InvBuilder.buildCompassInv()), 2L);
                    }
                }

                if (event.getClickedInventory().getTitle().contains("VRAHA") &&
                        event.getCurrentItem().getType() != Material.AIR)
                {
                    if (event.getCurrentItem().getType() == Material.SKULL_ITEM)
                    {
                        (Ludia.getVsetci().get(pll)).setBetPlayer(event.getCurrentItem().getItemMeta().getDisplayName());
                        pll.sendMessage("§7Vsadil si si na hráča §2" + event.getCurrentItem().getItemMeta().getDisplayName());
                        pll.closeInventory();
                        return;
                    }

                    int pack = -1;
                    BetPackage bp = BetPackage.NONE;
                    switch(event.getCurrentItem().getType())
                    {
                        case BARRIER:
                            pack = 0;
                            bp = BetPackage.NONE;
                            break;
                        case IRON_BLOCK:
                            pack = 1;
                            bp = BetPackage.IRON;
                            break;
                        case GOLD_BLOCK:
                            pack = 2;
                            bp = BetPackage.GOLD;
                            break;
                        case DIAMOND_BLOCK:
                            pack = 3;
                            bp = BetPackage.DIAMOND;
                            break;
                        case EMERALD_BLOCK:
                            pack = 4;
                            bp = BetPackage.EMERALD;
                    }

                    if (pack != -1)
                    {
                        if (pack == 0)
                        {
                            pll.sendMessage("§7Tvoj tip bol §czrušený");
                        }

                        Ludia.getVsetci().get(pll).setBetPackage(pack);
                        Ludia.getVsetci().get(pll).setBp(bp);
                        pll.closeInventory();
                        getServer().getScheduler().runTaskLater(this, () ->
                            pll.openInventory(InvBuilder.buildBetInv(
                                    Ludia.getVsetci().get(pll))), 2L);
                    }
                }
            }
            catch (NullPointerException var7)
            {
                getLogger().warning("Null pointer - kosmetik menu");
            }
            finally
            {
                event.setCancelled(true);
            }

        }
    }

    @EventHandler
    public void onArrowPickup(PlayerPickupArrowEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDmg(EntityDamageEvent event)
    {
        if (event.getEntity() instanceof Player && event.getCause() == DamageCause.VOID)
        {
            hra.killPlayer(Ludia.getVsetci().get(event.getEntity()), true);
            event.getEntity().teleport(gameMap.getSpawn().get(0));
        }

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event)
    {
        if (Ludia.getVsetci().containsKey(event.getEntity()) && (Ludia.getVsetci().get(event.getEntity())).isAlive())
        {
            hra.killPlayer(Ludia.getVsetci().get(event.getEntity()), true);
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
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player &&
                hra.getState() == GameState.Ingame && isValidWeapon(event.getDamager()))
        {
            hra.killPlayer(getKiller(event.getDamager()), (Player)event.getEntity());
        }
        else if (event.getDamager() instanceof Arrow && ((Arrow)event.getDamager()).getShooter() instanceof Player &&
                event.getEntity() instanceof Player && hra.getState() == GameState.Ingame &&
                isValidWeapon(event.getDamager()))
        {
            if (!(Ludia.getVsetci().get(event.getEntity())).isAlive())
            {
                spawnNewArrow((Arrow)event.getDamager(), (Player)event.getEntity());
            }
            else
            {
                hra.killPlayer(getKiller(event.getDamager()), (Player)event.getEntity());
                event.getDamager().remove();
            }
        }
        else
        {
            if (event.getDamager() instanceof Player && event.getEntity() instanceof Player &&
                    ((Player)event.getDamager()).getInventory().getItemInMainHand().getType() == Material.STICK)
            {
                hra.testPlayer((Player)event.getEntity());
            }

        }
    }

    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == DamageCause.FALL) {
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onBreak(BlockBreakEvent event)
    {
        if (hra.getState() != GameState.Setup)
        {
            if (!event.getPlayer().isOp())
            {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event)
    {
        if (hra.getState() != GameState.Setup)
        {
            if (!event.getPlayer().isOp())
            {
                event.setCancelled(true);
            }

        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onArmorStandEvent(PlayerArmorStandManipulateEvent event) {
        event.setCancelled(true);
    }

    private boolean isClick(org.bukkit.event.block.Action action)
    {
        return action == org.bukkit.event.block.Action.LEFT_CLICK_AIR || action == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK || action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
    }

    @EventHandler
    public void onItemClick(PlayerInteractEvent event)
    {
        if (hra.getState() != GameState.Setup)
        {
            if(event.getClickedBlock() != null && event.getClickedBlock().getType().name().toLowerCase().contains("door"))
            {
                return;
            }

            if (isClick(event.getAction()) &&
                    event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BED)
            {
                event.setCancelled(true);
                DevTools.getBungeeCord().sendToLobby(event.getPlayer());
            }
            else if (isClick(event.getAction()) &&
                    event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BLAZE_POWDER)
            {
                getServer().getScheduler().runTaskLater(this, () -> event.getPlayer().openInventory(
                        InvBuilder.buildKosmeticInv(event.getPlayer())), 2L);
            }
            else if (isClick(event.getAction()) &&
                    event.getPlayer().getInventory().getItemInMainHand().getType() == Material.DOUBLE_PLANT)
            {
                getServer().getScheduler().runTaskLater(this, () ->
                        event.getPlayer().openInventory(InvBuilder.buildBetInv(
                                Ludia.getVsetci().get(event.getPlayer()))), 2L);
            }
            else if (!isClick(event.getAction()) ||
                    event.getPlayer().getInventory().getItemInMainHand().getType() != Material.COMPASS ||
                    (Ludia.getVsetci().get(event.getPlayer())).isAlive() &&
                            (Ludia.getVsetci().get(event.getPlayer())).getType() != PlayerType.None)
            {
                if (event.getAction() == org.bukkit.event.block.Action.PHYSICAL &&
                        event.getClickedBlock().getType() == Material.SOIL)
                {
                    event.setCancelled(true);
                    event.getClickedBlock().setType(Material.SOIL);
                }

                if (!Ludia.getVsetci().containsKey(event.getPlayer()) || !Ludia.getClovek(event.getPlayer()).isAlive())
                {
                    event.setCancelled(true);
                }
            }
            else
            {
                event.getPlayer().openInventory(InvBuilder.buildCompassInv());
            }
        }
    }

    @EventHandler
    public void onGadgetClick(PlayerInteractEvent event)
    {
        if(event.getPlayer() == null || event.getClickedBlock() == null || hra.getState() != GameState.Ingame)
            return;

        if(event.getPlayer().getInventory().getItemInMainHand().getType() == Material.POTION)
            return;

        if(event.getClickedBlock() != null && event.getClickedBlock().getType().name().toLowerCase().contains("door"))
            return;

        if(Ludia.getClovek(event.getPlayer()) == null)
            return;

        if(Ludia.getClovek(event.getPlayer()).isAlive())
        {
            gameMap.getGadgets().forEach(g ->
            {
                if(g.getLocation().getX() == event.getClickedBlock().getLocation().getX() &&
                        g.getLocation().getY() == event.getClickedBlock().getLocation().getY() &&
                        g.getLocation().getZ() == event.getClickedBlock().getLocation().getZ())
                {
                    g.onClick(event.getPlayer());
                    event.setCancelled(true);
                }
            });
        }
    }

    @EventHandler
    public void onPotion(PlayerItemConsumeEvent event)
    {
        if(event.getItem().getType() == Material.POTION)
        {
            getServer().getScheduler().runTaskLater(this, () ->
                    event.getPlayer().getInventory().remove(Material.GLASS_BOTTLE), 2L);
        }
    }

    @EventHandler
    public void onSword(PlayerInteractEvent event)
    {
        try
        {
            if (hra.getState() != GameState.Ingame)
            {
                return;
            }

            if (event.getItem() == null || event.getItem().getType() != Ludia.getVrah().getSword())
            {
                return;
            }

            if (event.getAction().name().toLowerCase().contains("right") && SwordCooldown <= 0.0D)
            {
                createSword(event.getPlayer());
                SwordCooldown = 4.0D;
            }
        }
        catch (Exception var3)
        {
            getLogger().warning("Exception - onSword: " + var3.toString());
        }

    }

    @EventHandler
    public void onBowShot(EntityShootBowEvent event)
    {
        if (event.getEntity() instanceof Player) {
            Clovek c = Ludia.getClovek((Player)event.getEntity());
            if (c == null) {
                return;
            }

            if (Ludia.getBowOwner() == c) {
                ((Player)event.getEntity()).getInventory().remove(Material.BOW);
                ((Player)event.getEntity()).getInventory().setItem(1, new ItemStack(Material.BOW, 1));
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (Ludia.getBowOwner().isAlive()) {
                        Ludia.getBowOwner().getPlayer().getInventory().setItem(2, new ItemStack(Material.ARROW, 1));
                    }

                }, 100L);
                if (c.getTrail() != null) {
                    sipi.put((Projectile)event.getProjectile(), c.getTrail());
                }
            }
        }

    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event)
    {
        if (sipi.containsKey(event.getEntity()))
        {
            sipi.remove(event.getEntity());
        }
    }

    @EventHandler
    public void onPaintingEvent(HangingBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onSwapEvent(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onIntercatAtEntityEvent(PlayerInteractAtEntityEvent event)
    {
        if (!(Ludia.getVsetci().get(event.getPlayer())).isAlive())
        {
            event.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String ass = event.getMessage().split(" ")[0];
        getLogger().info(event.getMessage());
        if (ass.equalsIgnoreCase("/msg") || ass.equalsIgnoreCase("/pm") || ass.equalsIgnoreCase("/tell") || ass.equalsIgnoreCase("/m") || ass.equalsIgnoreCase("/r")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Súkromné správy sa tu neposielaju!");
        }

    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event)
    {
        if (!(Ludia.getVsetci().get(event.getPlayer())).isAlive() && hra.getState() != GameState.End)
        {
            Ludia.getVsetci().values().forEach((c) -> event.getRecipients().remove(c.getPlayer()));
            String s = event.getMessage();
            event.setMessage(ChatColor.GRAY + s);
        }

        if (event.getMessage().equalsIgnoreCase("gg") && hra.getState() == GameState.End &&
                Ludia.getVsetci().containsKey(event.getPlayer()) &&
                !(Ludia.getVsetci().get(event.getPlayer())).getGG() && !ggs.contains(event.getPlayer().getDisplayName()))
        {
            Ludia.getVsetci().get(event.getPlayer()).gg();
            ggs.add(event.getPlayer().getDisplayName());
            DevTools.getUser(event.getPlayer()).addPoints(20, true);
        }

    }

    private boolean isValidWeapon(Entity damager)
    {
        try
        {
            return damager instanceof Arrow && ((Arrow)damager).getShooter() instanceof Player ||
                    damager instanceof Player &&
                            ((Player)damager).getInventory().getItemInMainHand().getType() == Ludia.getVrah().getSword();
        }
        catch (Exception e)
        {
            getLogger().warning("Error checking weapon: "+e.toString());
            if(damager instanceof Player)
                damager.sendMessage("§cNastal error, kontaktuj helpera alebo developera. Chyba: WeaponValidation");
            return false;
        }
    }

    private Player getKiller(Entity damager)
    {
        return damager instanceof Arrow ? (Player)((Arrow)damager).getShooter() : (Player)damager;
    }

    private void createConfig()
    {
        try {
            if (!getDataFolder().exists() && getDataFolder().mkdirs()) {
                getLogger().warning("Neviem vytvorit config priecinok!");
            }

            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                getLogger().info(Lang.CONFIG_CREATED);
            } else {
                getLogger().info(Lang.CONFIG_LOADED);
            }
        } catch (Exception var2) {
            get().getLogger().warning(Lang.CONFIG_LOAD_ERROR + " " + var2.toString());
        }

    }

    private String selectWorld() {
        if (!getConfig().isSet("maps")) {
            return !getConfig().isSet("lobby.world") ? "" : getConfig().getString("lobby.world");
        }
        else
        {
            List<String> maps = getConfig().getStringList("maps");
            getLogger().info("Available maps: " + maps);
            Random r = new Random();
            return (String)maps.toArray()[r.nextInt(maps.size())];
        }
    }

    private void spawnNewArrow(Arrow arrow, Player entity) {
        Location l = entity.getLocation();
        boolean isFl = entity.isFlying();
        entity.setGameMode(GameMode.SPECTATOR);
        Projectile na = entity.launchProjectile(Arrow.class);
        na.setShooter(arrow.getShooter());
        na.setVelocity(arrow.getVelocity());
        na.setBounce(arrow.doesBounce());
        if (sipi.containsKey(arrow)) {
            Particle p = sipi.get(arrow);
            sipi.put(na, p);
            sipi.remove(arrow);
        }

        arrow.remove();
        getServer().getScheduler().runTaskLater(this, () -> {
            entity.setGameMode(GameMode.ADVENTURE);
            entity.setAllowFlight(true);
            entity.setFlying(isFl);
        }, 3L);
        getServer().getScheduler().runTaskLater(this, () -> entity.teleport(l.add(0.0D, 0.5D, 0.0D)),
                5L);
    }

    private void createSword(Player player)
    {
        if (swordStand != null)
            destroySword();

        double ya = Math.toRadians((double)(player.getLocation().getYaw() + 180.0F));
        swordStand = player.getWorld().spawn(player.getLocation().add(-1.0D * Math.cos(ya), 0.0D, -1.0D * Math.sin(ya)), ArmorStand.class);
        swordStand.getLocation().setPitch(player.getEyeLocation().getPitch());
        swordStand.getLocation().setYaw(player.getEyeLocation().getYaw());
        swordStand.setArms(true);
        swordStand.setBasePlate(false);
        swordStand.setVisible(false);
        swordStand.setGravity(false);
        swordStand.setCustomName("sword");
        swordStand.setCustomNameVisible(false);
        swordStand.setRightArmPose(new EulerAngle(Math.toRadians(0.0D), Math.toRadians((double)(-player.getLocation().getPitch())), Math.toRadians(90.0D)));
        swordStand.setItemInHand(player.getInventory().getItemInMainHand().clone());
        moveSword(player);
    }

    private void moveSword(Player player)
    {
        Location location = swordStand.getLocation();
        double d4 = Math.toRadians((double)(location.getYaw() + 90.0F));
        double d5 = Math.toRadians((double)(location.getPitch() + 90.0F));
        double d6 = Math.sin(d5) * Math.cos(d4);
        double d7 = Math.sin(d5) * Math.sin(d4);
        double d8 = Math.cos(d5);
        int[] moveTime = new int[]{0};
        double speed = 3.0D;
        moveTask = getServer().getScheduler().runTaskTimer(this, () ->
        {
            ++moveTime[0];
            if (swordStand == null)
                destroySword();

            location.setX(location.getX() + d6 * speed);
            location.setY(location.getY() + d8 * speed);
            location.setZ(location.getZ() + d7 * speed);
            swordStand.teleport(location);
            checkSword(location, player);
        }, 2L, 2L);
    }

    private void checkSword(Location location, Player player)
    {
        Location uplocation = location.clone();
        uplocation.add(0.0D, 2.0D, 0.0D);
        if (uplocation.getBlock().getType() != Material.AIR)
        {
            getLogger().info("REMOVE SWORD - BLOCK");
            destroySword();
        }
        else
        {
            for(Entity e : location.getWorld().getNearbyEntities(location, 0.75D, 2.0D, 0.75D))
            {
                if (e.getType() == EntityType.PLAYER && !e.getUniqueId().equals(player.getUniqueId()) &&
                        Ludia.getVsetci().containsKey(((Player)e).getPlayer()))
                {
                    if (Ludia.getVrah() != null)
                    {
                        hra.killPlayer(Ludia.getVrah().getPlayer(), (Player)e);
                    }

                    getLogger().info("REMOVE SWORD - ENTITA");
                    destroySword();
                }
            }
        }
    }

    private void destroySword()
    {
        try
        {
            getLogger().info("destroying");
            getServer().getScheduler().cancelTask(moveTask.getTaskId());
            swordStand.remove();
            moveTask = null;
            swordStand = null;
        }
        catch (NullPointerException e)
        {
            getLogger().warning("NullPointer destroy sword: "+e.toString());
        }
    }

    /*private void stopSwordCounter(boolean throwing, Player player) {
        getLogger().info("stopping");
        getServer().getScheduler().cancelTask(swordTask.getTaskId());
        player.getInventory().setItemInOffHand((ItemStack)null);
        if (throwing) {
            player.getInventory().setItemInMainHand((ItemStack)null);
            getServer().getScheduler().runTaskLater(this, () -> {
                player.getInventory().setItem(1, new ItemStack(Ludia.getVrah().getSword(), 1));
            }, 60L);
        }
    }*/

    public Game getHra() {
        return hra;
    }

    ArmorStand getBowStand() {
        return bowStand;
    }

    ArmorStand getSwordStand() {
        return swordStand;
    }

    void setBowStand(ArmorStand ne) {
        bowStand = ne;
    }

    Map<Projectile, Particle> getSipi() {
        return sipi;
    }

    public Vector<String> getTop() {
        return top;
    }

    public GameMap getMap() {
        return gameMap;
    }

    public void setLobbyLocation(Location l) {
        gameMap.setLobby(l);
    }

    public void setSpawnLocation(Location l) {
        gameMap.addSpawn(l);
    }

    public void setItemLocation(Location l) {
        gameMap.addItem(l);
    }

    public DBExecutor getDBE() {
        return dbe;
    }
}
