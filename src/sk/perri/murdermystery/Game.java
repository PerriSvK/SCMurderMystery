package sk.perri.murdermystery;

import me.mirek.devtools.api.currencies.LuckyShardsAPI;
import me.mirek.devtools.api.currencies.PointsAPI;
import me.mirek.devtools.api.utils.BungeeAPI;
import me.mirek.devtools.api.utils.TitleAPI;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.golde.bukkit.corpsereborn.CorpseAPI.CorpseAPI;
import sk.perri.murdermystery.enums.DetectiveStatus;
import sk.perri.murdermystery.enums.GameOverReason;
import sk.perri.murdermystery.enums.GameState;
import sk.perri.murdermystery.enums.PlayerType;
import sk.perri.murdermystery.utils.CenterMessage;

import java.util.*;

import static sk.perri.murdermystery.enums.GameOverReason.*;

public class Game
{
    private ArrayList<Player> civilians = new ArrayList<>();
    private Vector<Clovek> alive = new Vector<>();
    private Vector<Clovek> spect = new Vector<>();
    private Vector<Location> spawn = new Vector<>();
    private Vector<Location> itemsLocation = new Vector<>();
    private Location bowLocation = null;
    private int ticks = 6000;
    private int time = 300;
    private Clovek killer = null;
    private Clovek detective = null;
    private Clovek hero = null;
    private BukkitTask task;
    private int countdown = -1;
    private GameState state;
    private DetectiveStatus detectiveStatus = DetectiveStatus.Null;
    private String deName = "";
    private Vector<String> inno = new Vector<>();

    Game()
    {
        state = GameState.Lobby;
    }

    void addPlayer(Player player) {
        Clovek c = new Clovek(player, new SBManager(player));

        alive.forEach(cl -> cl.getSBManager().registerPlayer(player));
        spect.forEach(cl -> cl.getSBManager().registerPlayer(player));

        if ((state == GameState.Starting || state == GameState.Lobby) && alive.size() < Main.get().getConfig().getInt("maxplayers"))
        {
            alive.add(c);
            player.setGameMode(GameMode.ADVENTURE);
            player.setFlying(false);
            player.setAllowFlight(false);
            c.setType(PlayerType.None);
        }
        else
        {
            spect.add(c);
            c.setType(PlayerType.Spectator);
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);

            Main.get().getServer().getOnlinePlayers().forEach(p -> p.hidePlayer(player));
            spect.forEach(sp -> player.hidePlayer(sp.getPlayer()));
            giveSpectItems(player);
        }
    }

    void removePlayer(Player player)
    {
        if (detective != null && detective.getPlayer().getUniqueId().equals(player.getUniqueId()))
            killPlayer(detective, false);

        for (Clovek c : alive) {
            c.getSBManager().deleteTeam(player);
        }

        for (Clovek c : spect) {
            c.getSBManager().deleteTeam(player);
        }

        alive.removeIf(c -> c.getPlayer().getUniqueId() == player.getUniqueId());
        spect.removeIf(c -> c.getPlayer().getUniqueId() == player.getUniqueId());
    }

    void killPlayer(Player kille, Player victim)
    {
        Clovek vrah = findClovek(kille);
        Clovek obet = findClovek(victim);

        if (vrah.getType() == PlayerType.Spectator || vrah.getType() == PlayerType.None ||
                obet.getType() == PlayerType.None || obet.getType() == PlayerType.Spectator)
            return;

        if (!(vrah.getType() == PlayerType.Killer || obet.getType() == PlayerType.Killer))
        {
            vrah.addScore(ScoreTable.I_KILL_I);
            vrah.getPlayer().sendMessage(ChatColor.GOLD+""+ScoreTable.I_KILL_I+" za zabití občana!");
            killPlayer(vrah, false);
        }

        if (vrah.getType() == PlayerType.Killer)
        {
            if (obet.getType() == PlayerType.Detective)
            {
                vrah.addScore(ScoreTable.M_KILL_D);
                vrah.getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.M_KILL_D+" za zabití detektiva!");
            }
            else
            {
                vrah.addScore(ScoreTable.M_KILL_I);
                vrah.getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.M_KILL_I+" za zabití občana!");
            }
        }

        if (obet.getType() == PlayerType.Killer)
        {
            vrah.addScore(ScoreTable.I_KILL_M);
            vrah.getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.I_KILL_M+" za zabití vraha!");
            if(detectiveStatus != DetectiveStatus.Alive || vrah.getType() == PlayerType.Innocent)
                hero = vrah;
        }

        if(vrah.getPlayer().getUniqueId() != obet.getPlayer().getUniqueId())
            killPlayer(obet, false);
    }

    // void killPlayer(Player player) { killPlayer(findClovek(player)); }

    void killPlayer(Clovek clovek, boolean voi)
    {
        alive.remove(clovek);
        TitleAPI.sendTitle(clovek.getPlayer(),Lang.P_MSG_KILLED, 10, 60, 10);
        clovek.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
        //Main.get().getServer().broadcastMessage(Lang.DEAD_MSG + " " + ChatColor.RED + clovek.getPlayer().getDisplayName());

        if (clovek.getType() == PlayerType.Detective)
        {
            resetBow(voi);
        }

        clovek.setType(PlayerType.Spectator);

        //clovek.getPlayer().setGameMode(GameMode.SPECTATOR);

        Main.get().getServer().getOnlinePlayers().forEach(c -> c.getPlayer().hidePlayer(clovek.getPlayer()));
        spect.add(clovek);
        clovek.getSBManager().createSpectBoard();

        CorpseAPI.spawnCorpse(clovek.getPlayer(), clovek.getPlayer().getLocation());

        // SOUND
        for(Player p : Main.get().getServer().getOnlinePlayers())
        {
            p.getWorld().playSound(clovek.getPlayer().getLocation(), Sound.ENTITY_PLAYER_DEATH, 100, 1);
        }

        // INV
        clovek.getPlayer().getInventory().clear();

        // GIVE ITEMS
        giveSpectItems(clovek.getPlayer());

        //FLY
        clovek.getPlayer().setAllowFlight(true);
        clovek.getPlayer().setFlying(true);
    }

    // Positions
    Vector<Location> getSpawn() {
        return spawn;
    }

    Vector<Location> getItemsLocation() {
        return itemsLocation;
    }

    Location getSpectSpawnLocation() {
        return getSpawn().firstElement();
    }

    public Clovek getKiller() {
        return killer;
    }

    // Main game STRUCTURE
    public void start(boolean force) {
        // start countdown
        if (countdown > 0)
        {
            if (force && countdown > 6)
                countdown = 5;

            return;
        }

        countdown = force ? 6 : 60;
        state = GameState.Starting;

        task = Bukkit.getScheduler().runTaskTimer(Main.get(), () ->
        {
            countdown--;

            alive.forEach(c -> c.getSBManager().updateLobbyBoard());

            if (countdown <= 5) {
                ChatColor cc;
                state = GameState.Start;

                switch (countdown) {
                    case 5:
                        cc = ChatColor.GREEN;
                        break;
                    case 4:
                        cc = ChatColor.YELLOW;
                        break;
                    case 3:
                        cc = ChatColor.GOLD;
                        break;
                    case 2:
                        cc = ChatColor.RED;
                        break;
                    default:
                        cc = ChatColor.DARK_RED;
                }

                alive.forEach(c -> TitleAPI.sendTitle(c.getPlayer(),cc + "" + countdown, 5, 10, 5));
            }

            if (countdown < 1) {
                stopCountdown();
                loop();
            }

            if (countdown % 10 == 0)
                Main.get().getServer().broadcastMessage(Lang.G_W_START_IN + " " + ChatColor.RED + countdown + Lang.SECONDS);

        }, 0L, 20L);
    }

    private void loop()
    {
        // port players to the spawn
        List<Integer> ik = new ArrayList<>();

        for (int i = 0; i < spawn.size(); i++) {
            ik.add(i);
        }
        Collections.shuffle(ik);
        state = GameState.Ingame;

        Main.get().getServer().broadcastMessage(Lang.GAME_START);

        for (int i = 0; i < alive.size(); i++)
        {
            Clovek c = alive.get(i);
            c.getPlayer().teleport(spawn.get(ik.get(i)));
            c.getPlayer().getInventory().clear();
            c.setType(PlayerType.Innocent);
        }

        // Role role
        roleRole();
        giveWeapons();
        alive.forEach(c -> c.getSBManager().createGameBoard(c.getType()));
        Main.get().removeItems();

        // main game loop
        task = Bukkit.getScheduler().runTaskTimer(Main.get(), () ->
        {
            countdown--;

            for(Map.Entry<Projectile, Particle>  en : Main.get().getSipi().entrySet())
            {
                en.getKey().getLocation().getWorld().spawnParticle(en.getValue(), en.getKey().getLocation(), 2, 0.2, 0.2, 0.2);
            }

            if(countdown % 20 != 0)
            {
                return;
            }

            time--;
            if (time % 3 == 0) {
                spawnItem();
                spawnItem();
            }

            if (time % 30 == 0) {
                for (Clovek c : alive) {
                    if (c == killer || c == detective)
                        continue;

                    c.addScore(ScoreTable.TIME_ALIVE);
                    c.getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.TIME_ALIVE+" za přežití dalších 30 sekund!");
                }
            }
            // Update scoreboards
            alive.forEach(c -> c.getSBManager().updateGameBoard(c.getScore()));
            spect.forEach(c -> c.getSBManager().updateSpectBoard());
            // Update killer compassw
            murderCompass();
            // check if anyone win
            winCheck();
        }, 10, 1);
    }

    // Countdown

    private void stopCountdown() {
        Bukkit.getScheduler().cancelTask(task.getTaskId());
        countdown = -1;
        task = null;
    }

    // functions
    private void roleRole()
    {
        Random r = new Random();
        killer = alive.get(r.nextInt(alive.size()));
        killer.setType(PlayerType.Killer);
        TitleAPI.sendTitle(killer.getPlayer(),Lang.MURDERER_INFO_1, 20, 60, 20);
        TitleAPI.sendSubTitle(killer.getPlayer(),Lang.MURDERER_INFO_2, 20, 60, 20);
        killer.getPlayer().sendMessage(Lang.MURDERER_INFO_1 + " " + Lang.MURDERER_INFO_2 +
                " Meč hodíš podržením pravého tlačítka myši!");

        detective = alive.get(r.nextInt(alive.size()));
        while (killer.getType() == detective.getType()) {
            detective = alive.get(r.nextInt(alive.size()));
        }

        detective.setType(PlayerType.Detective);
        TitleAPI.sendTitle(detective.getPlayer(),Lang.DETECTIVE_INFO_1, 20, 60, 20);
        TitleAPI.sendSubTitle(detective.getPlayer(),Lang.DETECTIVE_INFO_2, 20, 60, 20);
        detectiveStatus = DetectiveStatus.Alive;
        detective.getPlayer().sendMessage(Lang.DETECTIVE_INFO_1 + " " + Lang.DETECTIVE_INFO_2 +
                " Po vystrelení získaš šíp po 4 sekundách!");
        civilians.add(detective.getPlayer());
        deName = detective.getPlayer().getDisplayName();

        for (Clovek c : alive) {
            if (c != killer && c != detective) {
                TitleAPI.sendTitle(c.getPlayer(),Lang.INOCENT_INFO_1, 20, 60, 20);
                TitleAPI.sendSubTitle(c.getPlayer(),Lang.INOCENT_INFO_2, 20, 60, 20);
                c.getPlayer().sendMessage(Lang.INOCENT_INFO_1 + " " + Lang.INOCENT_INFO_2
                        + " Seber 10 goldů získej luk a zabij vraha!");
                civilians.add(c.getPlayer());
            }
        }
    }

    private void giveWeapons() {
        Main.get().getServer().broadcastMessage(Lang.W_CD_MSG);
        Bukkit.getScheduler().runTaskLater(Main.get(), () ->
        {
            Main.get().getServer().broadcastMessage(Lang.W_H_G);
            killer.getPlayer().getInventory().setItem(1, new ItemStack(killer.getSword(), 1));
            if (detective != null) {
                detective.getPlayer().getInventory().setItem(1, new ItemStack(Material.BOW, 1));
                detective.getPlayer().getInventory().setItem(2, new ItemStack(Material.ARROW, 1));
            }
            else
            {
                if (bowLocation == null)
                    bowLocation = spawn.get(0);

                killer.getPlayer().getWorld().dropItemNaturally(bowLocation, new ItemStack(Material.BOW, 1));
                Main.get().getServer().broadcastMessage(Lang.DET_DEAD);
                giveBowCompass();
            }
        }, 300);
    }

    private void giveBowCompass() {
        for (Clovek c : alive) {
            if (c.getType() == PlayerType.Killer || bowLocation == null)
                continue;

            c.getPlayer().setCompassTarget(bowLocation);
            ItemStack com = new ItemStack(Material.COMPASS, 1);
            c.getPlayer().getInventory().setItem(4, com);
        }

        //Setting bow text stand
        if(bowLocation != null)
        {
            Location bsl = new Location(bowLocation.getWorld(), bowLocation.getX(), bowLocation.getY()+1, bowLocation.getZ());
            ArmorStand bs = bowLocation.getWorld().spawn(bowLocation, ArmorStand.class);
            bs.setVisible(false);
            bs.setCustomName(ChatColor.BLUE+""+ChatColor.BOLD+"LUK");
            bs.setCustomNameVisible(true);
            Main.get().setBowStand(bs);
        }

    }

    private void removeCompass()
    {
        for (Clovek o : alive) {
            if (o.getType() == PlayerType.Killer)
                continue;

            o.getPlayer().getInventory().remove(Material.COMPASS);
        }

        if(Main.get().getBowStand() != null)
        {
            Main.get().getBowStand().setCustomNameVisible(false);
            Main.get().getBowStand().remove();
            Main.get().setBowStand(null);
        }
    }

    private void murderCompass() {
        if (alive.size() == 2) {
            if (!killer.getPlayer().getInventory().contains(Material.COMPASS)) {
                killer.getPlayer().getInventory().setItem(4, new ItemStack(Material.COMPASS, 1));
            }

            killer.getPlayer().setCompassTarget(alive.get(alive.get(0).getType() == PlayerType.Killer ? 1 : 0).getPlayer().getLocation());
        }
    }

    private void spawnItem()
    {
        itemsLocation.get(0).getWorld().dropItemNaturally(itemsLocation.get(new Random().nextInt(itemsLocation.size())),
                new ItemStack(Material.GOLD_INGOT, 1));
    }

    void winCheck()
    {
        boolean go = false;
        GameOverReason reason = NULL;

        if (!alive.contains(killer))
        {
            go = true;
            reason = KILLER_DEAD;
        }
        else if (alive.size() == 1)
        {
            go = true;
            reason = ALL_DEAD;
        }
        else if (time <= 0)
        {
            go = true;
            reason = TIME_OUT;
        }
        else if(killer == null)
        {
            go = true;
            reason = KILLER_LEFT;
        }

        if (go)
        {
            state = GameState.End;
            gameOver(reason);
        }
    }

    private void resetBow(boolean voi)
    {
        detectiveStatus = DetectiveStatus.Killed;
        bowLocation = voi ? spawn.get(0) : detective.getPlayer().getLocation();

        detective = null;
        if (killer != null)
            killer.getPlayer().getWorld().dropItemNaturally(bowLocation, new ItemStack(Material.BOW, 1));
        Main.get().getServer().broadcastMessage(Lang.DET_DEAD);
        giveBowCompass();
    }

    private void gameOver(GameOverReason reason)
    {
        Bukkit.getScheduler().cancelAllTasks();
        Vector<String> ss = new Vector<>();
        ChatColor cc = ChatColor.RED;
        int winner = 0; //0 - Murder; 1 - DET; 2 - HERO

        if (killer == null)
            return;

        killer.getPlayer().getWorld().playSound(killer.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 100, 1);

        switch (reason)
        {
            case ALL_DEAD:
                Main.get().getServer().getOnlinePlayers().forEach(p ->
                {
                    TitleAPI.sendTitle(p,Lang.LOOSE_MORE, 10, 80, 10);
                    TitleAPI.sendSubTitle(p,Lang.I_LOOSE_REASON, 10, 80, 10);
                });
                TitleAPI.sendTitle(killer.getPlayer(),Lang.WIN, 10, 80, 10);
                TitleAPI.sendSubTitle(killer.getPlayer(),Lang.KILLER_WIN_REASON, 10, 80, 10);
                PointsAPI.addPoints(killer.getPlayer(), 100);
                LuckyShardsAPI.addLuckyShards(killer.getPlayer(), 10);
                killer.getPlayer().sendMessage("§8[] §e§l+ 10 LuckyShards");
                killer.getPlayer().sendMessage("§8[] §9§l+ 100 StylePoints");
                ss.add("Vyhrává: "+ChatColor.RED+""+ChatColor.BOLD+"VRAH");
                ss.add("");
                ss.add(ChatColor.GRAY+"Vrah: "+killer.getPlayer().getDisplayName());
                ss.add(ChatColor.GRAY+""+ChatColor.STRIKETHROUGH+"Detektív: "+deName);
                break;
            default:
                civilians.forEach(p ->
                {
                    if ((hero != null && p.getDisplayName().equalsIgnoreCase(hero.getPlayer().getDisplayName()))
                            || (detectiveStatus == DetectiveStatus.Alive &&
                        p.getDisplayName().equalsIgnoreCase(detective.getPlayer().getDisplayName())))
                    {
                        PointsAPI.addPoints(p, 50);
                        LuckyShardsAPI.addLuckyShards(p, 5);
                        p.sendMessage("§8[] §e§l+ 10 LuckyShards");
                        p.sendMessage("§8[] §9§l+ 100 StylePoints");
                        TitleAPI.sendTitle(p, Lang.WIN_MORE, 10, 80, 10);
                        TitleAPI.sendSubTitle(p, Lang.KILLER_STOPPED, 10, 80, 10);
                    }
                    else
                    {
                        PointsAPI.addPoints(p, 100);
                        LuckyShardsAPI.addLuckyShards(p, 10);
                        p.sendMessage("§8[] §e§l+5 LuckyShards");
                        p.sendMessage("§8[] §9§l+50 StylePoints");
                    }
                });

                if(hero == null)
                    cc = ChatColor.BLUE;
                else
                    cc = ChatColor.GREEN;

                TitleAPI.sendTitle(killer.getPlayer(),Lang.LOOSE, 10, 80, 10);
                TitleAPI.sendSubTitle(killer.getPlayer(),Lang.KILLER_LOOSE_REASON, 10, 80, 10);
                ss.add(hero == null ? "Vyhrává: "+ChatColor.BLUE+""+ChatColor.BOLD+"DETEKTIV" : "Vyhrávají: "+ChatColor.GREEN+""+ChatColor.BOLD+"OBČANÉ");
                ss.add("");
                ss.add(ChatColor.GRAY+""+ChatColor.STRIKETHROUGH+"Vrah: "+killer.getPlayer().getDisplayName());
                ss.add(ChatColor.GRAY+""+(hero != null ? ChatColor.STRIKETHROUGH : "")+"Detektiv: "+deName);
                if(hero != null)
                {
                    ss.add(ChatColor.GRAY+"Hrdina: "+hero.getPlayer().getDisplayName());
                }
        }

        List<String> mess = new ArrayList<>();
        mess.add(cc+""+ChatColor.STRIKETHROUGH+"-----------------------------------------------------");
        mess.add(ChatColor.DARK_RED+"    "+ChatColor.BOLD+"Murder "+ChatColor.WHITE+ChatColor.BOLD+"Mystery");
        mess.add("");
        mess.addAll(ss);
        mess.add(cc+""+ChatColor.STRIKETHROUGH+"-----------------------------------------------------");

        for(Player p : Main.get().getServer().getOnlinePlayers())
        {
            for(String sss : mess)
            {
                CenterMessage.sendCenteredMessage(p, sss);
            }
        }

        Main.get().getServer().broadcastMessage(Lang.SERVER_RESTART);
        Bukkit.getScheduler().runTaskLater(Main.get(), () ->
                Main.get().getServer().getOnlinePlayers().forEach(BungeeAPI::sendToLobby), 400);
        Bukkit.getScheduler().runTaskLater(Main.get(), () -> Main.get().getServer().shutdown(), 550);
    }

    private void giveSpectItems(Player player)
    {
        // BACK TO LOBBY IS
        ItemStack btl = new ItemStack(Material.BED, 1);
        ItemMeta btlim = btl.getItemMeta();
        btlim.setDisplayName(ChatColor.RED+""+ChatColor.BOLD+"ZPĚT DO LOBBY");
        btl.setItemMeta(btlim);
        player.getInventory().setItem(8, btl);

        // Compass
        ItemStack co = new ItemStack(Material.COMPASS, 1);
        ItemMeta coim = co.getItemMeta();
        coim.setDisplayName(ChatColor.AQUA+""+ChatColor.BOLD+"HRÁČI");
        co.setItemMeta(coim);
        player.getInventory().setItem(0, co);
    }

    // Set + Get
    public void setState(GameState state)
    {
        this.state = state;
    }

    public GameState getState()
    {
        return state;
    }

    int getCountdown()
    {
        return countdown;
    }

    Vector<Clovek> getAlive()
    {
        return alive;
    }
    Vector<Clovek> getSpect()
    {
        return spect;
    }

    public Clovek getDetective() {
        return detective;
    }

    void setDetective(Player hrac) {
        detective = findClovek(hrac);
        detective.setType(PlayerType.Detective);
        Main.get().getServer().broadcastMessage(Lang.NEW_DET);
        removeCompass();
        detectiveStatus = DetectiveStatus.New;
    }

    String getTimeString() {
        int mins = Math.floorDiv(time, 60);
        String secs = String.format("%02d", time % 60);

        return mins + ":" + secs;
    }

    DetectiveStatus getDetectiveStatus() {
        return detectiveStatus;
    }

    void setDetectiveStatus(DetectiveStatus newStatus) {
        detectiveStatus = newStatus;
    }

    // utils
    Clovek findClovek(Player player) {
        Vector<Clovek> cc = new Vector<>();
        cc.addAll(alive);
        cc.addAll(spect);

        for (Clovek c : cc) {
            if (c.getPlayer().getUniqueId() == player.getUniqueId())
                return c;
        }

        return null;
    }
}
