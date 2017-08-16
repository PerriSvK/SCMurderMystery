package sk.perri.murdermystery;

import me.mirek.devtools.api.currencies.LuckyShardsAPI;
import me.mirek.devtools.api.currencies.PointsAPI;
import me.mirek.devtools.api.utils.*;
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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static sk.perri.murdermystery.enums.GameOverReason.*;

public class Game
{
    private ArrayList<Player> civilians = new ArrayList<>();
    private Vector<Clovek> alive = new Vector<>();
    private Vector<Clovek> spect = new Vector<>();
    private Location bowLocation = null;
    private int time = 300;
    private Clovek killer = null;
    private Clovek detective = null;
    private Clovek hero = null;
    private BukkitTask task;
    private int countdown = -1;
    private GameState state = GameState.Lobby;
    private DetectiveStatus detectiveStatus = DetectiveStatus.Null;
    private String deName = "";
    private int kkills = 0;
    private final boolean USE_EXP = false;

    Game()
    {
    }

    Clovek addPlayer(Player player)
    {
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
            c.setAlive(true);
        }
        else
        {
            spect.add(c);
            c.setType(PlayerType.None);
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);

            Main.get().getServer().getOnlinePlayers().forEach(p -> p.hidePlayer(player));
            spect.forEach(sp -> player.hidePlayer(sp.getPlayer()));
            giveSpectItems(player);
        }

        return c;
    }

    void removePlayer(Player player)
    {
        Clovek os = findClovek(player);
        if(os == null)
            return;

        if(os.isAlive() && alive.contains(findClovek(player)) && state == GameState.Ingame)
        {
            os.addScore(ScoreTable.ALIVE_DISC);
            killPlayer(os, false);
            updateScoreInDB(os);
        }

        os.setOnline(false);

        /*if (detective != null && detective.getPlayer().getUniqueId().equals(player.getUniqueId()))
            killPlayer(detective, false);*/

        for (Clovek c : alive)
        {
            c.getSBManager().deleteTeam(player);
        }

        for (Clovek c : spect)
        {
            c.getSBManager().deleteTeam(player);
        }

        if((state == GameState.Starting || state == GameState.Start) && Main.get().getServer().getOnlinePlayers().size() < 2)
        {
            stopCountdown();
            countdown = 60;
        }

        alive.removeIf(c -> c.getPlayer().getUniqueId() == player.getUniqueId());
        spect.removeIf(c -> c.getPlayer().getUniqueId() == player.getUniqueId());
    }

    void killPlayer(Player kille, Player victim)
    {
        Clovek vrah = findClovek(kille);
        Clovek obet = findClovek(victim);

        if (!vrah.isAlive() || vrah.getType() == PlayerType.None ||
                obet.getType() == PlayerType.None || !obet.isAlive())
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

            kkills++;
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
        Main.get().getServer().broadcastMessage(Lang.DEAD_MSG + " " + ChatColor.RED + clovek.getPlayer().getDisplayName());

        if (clovek.getType() == PlayerType.Detective)
        {
            resetBow(voi);
        }

        clovek.setAlive(false);

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

        //DB
        updateScoreInDB(clovek);
    }

    public Clovek getKiller() {
        return killer;
    }

    // Main game STRUCTURE
    public void start(boolean force) {
        // start countdown
        if (countdown > 0)
        {
            if (force && countdown > 16)
                countdown = 15;

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

        for (int i = 0; i < Main.get().getMap().getSpawn().size(); i++) {
            ik.add(i);
        }
        Collections.shuffle(ik);
        state = GameState.Ingame;

        Main.get().getServer().broadcastMessage(Lang.GAME_START);

        for (int i = 0; i < alive.size(); i++)
        {
            Clovek c = alive.get(i);
            c.getPlayer().teleport(Main.get().getMap().getSpawn().get(ik.get(i)));
            c.getPlayer().getInventory().clear();
            c.setType(PlayerType.Innocent);
            c.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        }

        // Role role
        roleRole();
        giveWeapons();
        alive.forEach(c ->
            {
                c.getSBManager().createGameBoard(c.getType());
                // INV
                c.getPlayer().getInventory().clear();
                c.getPlayer().getInventory().setHeldItemSlot(0);
            });
        Main.get().removeItems();
        Main.get().stopPercTimer();

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

    // Percentage
    void calculatePercentage()
    {
        final float[] sucet = {0, 0};
        alive.forEach(c ->
        {
            sucet[0] += c.getLkil();
            sucet[1] += c.getLdec();
        });

        alive.forEach(c ->
        {
            c.setPerk((float) (c.getLkil() / sucet[0])*100);
            c.setPerd((float) (c.getLdec() / sucet[1])*100);
        });
    }

    // functions
    private void roleRole()
    {
        // Create list
        List<Clovek> kilList = new ArrayList<>();
        List<Clovek> decList = new ArrayList<>();

        alive.forEach(c ->
        {
            for(int i = 0; i < c.getPerk(); i++)
            {
                kilList.add(c);
            }

            for(int i = 0; i < c.getPerd(); i++)
            {
                decList.add(c);
            }
        });

        // Random element from list - Killer
        Random r = new Random();
        int k = r.nextInt(kilList.size());
        killer = kilList.get(k);
        Main.get().getLogger().info("Killer - POS: "+k+" NAME: "+killer.getPlayer().getDisplayName());
        killer.setType(PlayerType.Killer);
        TitleAPI.sendTitle(killer.getPlayer(),Lang.MURDERER_INFO_1, 20, 60, 20);
        TitleAPI.sendSubTitle(killer.getPlayer(),Lang.MURDERER_INFO_2, 20, 60, 20);
        killer.getPlayer().sendMessage(Lang.MURDERER_INFO_1 + " " + Lang.MURDERER_INFO_2 +
                " Meč hodíš podržením pravého tlačítka myši!");

        // Random element from list - Detective
        int d = r.nextInt(decList.size());
        detective = decList.get(d);
        while (killer.getType() == detective.getType())
        {
            d = r.nextInt(decList.size());
            detective = decList.get(d);
        }
        Main.get().getLogger().info("Detective - POS: "+d+" NAME: "+detective.getPlayer().getDisplayName());
        detective.setType(PlayerType.Detective);
        TitleAPI.sendTitle(detective.getPlayer(),Lang.DETECTIVE_INFO_1, 20, 60, 20);
        TitleAPI.sendSubTitle(detective.getPlayer(),Lang.DETECTIVE_INFO_2, 20, 60, 20);
        detectiveStatus = DetectiveStatus.Alive;
        detective.getPlayer().sendMessage(Lang.DETECTIVE_INFO_1 + " " + Lang.DETECTIVE_INFO_2 +
                " Po vystrelení získaš šíp po 4 sekundách!");
        civilians.add(detective.getPlayer());
        deName = detective.getPlayer().getDisplayName();

        for (Clovek c : alive)
        {
            if (c != killer && c != detective)
            {
                TitleAPI.sendTitle(c.getPlayer(),Lang.INOCENT_INFO_1, 20, 60, 20);
                TitleAPI.sendSubTitle(c.getPlayer(),Lang.INOCENT_INFO_2, 20, 60, 20);
                c.getPlayer().sendMessage(Lang.INOCENT_INFO_1 + " " + Lang.INOCENT_INFO_2
                        + " Seber 10 goldů, získej luk a zabij vraha!");
                civilians.add(c.getPlayer());
            }
        }

        // DB INSERT - UPDATE
        try
        {
            Statement st = Main.get().getConn().createStatement();
            for(Clovek cl : alive)
            {
                String sql;
                int lk = cl.getLkil() < 1 ? 0 : (int) Math.round(cl.getLkil());
                int ld = cl.getLdec() < 1 ? 0 : (int) Math.round(cl.getLdec());
                String ggg = "games = "+(cl.getGames()+1);
                String swo = "sword = '"+cl.getSword().toString()+"'";

                String t = "NULL";
                if(cl.getTrail() != null)
                    t = cl.getTrail().toString();

                String tra = "trail = '"+t+"'";
                String pri = ggg+", "+swo+", "+tra;

                if(cl.getPlayer().getDisplayName().equalsIgnoreCase(killer.getPlayer().getDisplayName()))
                {
                    sql = "UPDATE murder SET lkil = 0, ldet = "+(ld+1)+", "+pri+" WHERE name = '"+killer.getPlayer().getDisplayName()+"';";
                }
                else if(cl.getPlayer().getDisplayName().equalsIgnoreCase(detective.getPlayer().getDisplayName()))
                {
                    sql = "UPDATE murder SET ldet = 0, lkil = "+(lk+1)+", "+pri+" WHERE name = '"+detective.getPlayer().getDisplayName()+"';";
                }
                else
                {
                    sql = "UPDATE murder SET ldet = "+(ld+1)+", lkil = "+(lk+1)+", "+pri+" WHERE name = '"+cl.getPlayer().getDisplayName()+"';";
                }

                st.execute(sql);
            }

            st.close();
        }
        catch (SQLException e)
        {
            Main.get().getLogger().warning("SQL Error - Game#roleRole - e: "+e.getMessage());
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
                    bowLocation = Main.get().getMap().getSpawn().get(0);

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
            // Location bsl = new Location(bowLocation.getWorld(), bowLocation.getX(), bowLocation.getY()+1, bowLocation.getZ());
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
        Main.get().getMap().getItems().get(0).getWorld().dropItemNaturally(
                Main.get().getMap().getItems().get(new Random().nextInt(Main.get().getMap().getItems().size())),
                new ItemStack(Material.GOLD_INGOT, 1));
    }

    private void winCheck()
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
        bowLocation = voi ? Main.get().getMap().getSpawn().get(0) : detective.getPlayer().getLocation();

        detective = null;
        if (killer != null)
            killer.getPlayer().getWorld().dropItemNaturally(bowLocation, new ItemStack(Material.BOW, 1));
        Main.get().getServer().broadcastMessage(Lang.DET_DEAD);
        giveBowCompass();
    }

    private void gameOver(GameOverReason reason)
    {
        Bukkit.getScheduler().cancelAllTasks();

        if(Main.get().getSwordStand() != null)
            Main.get().getSwordStand().remove();

        Vector<String> ss = new Vector<>();
        ChatColor cc = ChatColor.RED;

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
                if(USE_EXP)
                    PointsAPI.addPoints(killer.getPlayer(), 100);
                LuckyShardsAPI.addLuckyShards(killer.getPlayer(), 10);
                LevelAPI.addXp(killer.getPlayer(), 50);
                killer.getPlayer().sendMessage("§8[] §e§l+ 10 LuckyShards");
                if(USE_EXP)
                    killer.getPlayer().sendMessage("§8[] §2§l+ 50 Experiences");
                killer.getPlayer().sendMessage("§8[] §9§l+ 100 StylePoints");
                ss.add("Vyhrává: "+ChatColor.RED+""+ChatColor.BOLD+"VRAH");
                ss.add("");
                ss.add(ChatColor.GRAY+"Vrah: "+killer.getPlayer().getDisplayName()+" ("+kkills+")");
                ss.add(ChatColor.GRAY+""+ChatColor.STRIKETHROUGH+"Detektív: "+deName);
                break;

            case KILLER_LEFT:
            case TIME_OUT:
                alive.forEach(c ->
                {
                    if(killer != c)
                    {
                        c.addScore(ScoreTable.END_ALIVE);
                        c.getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.END_ALIVE+" za přežití!");
                        PointsAPI.addPoints(c.getPlayer(), 50);
                        LuckyShardsAPI.addLuckyShards(c.getPlayer(), 5);
                        if(USE_EXP)
                            LevelAPI.addXp(c.getPlayer(), 20);
                        c.getPlayer().sendMessage("§8[] §e§l+ 5 LuckyShards");
                        if(USE_EXP)
                            c.getPlayer().sendMessage("§8[] §2§l+ 20 Experiences");
                        c.getPlayer().sendMessage("§8[] §9§l+ 50 StylePoints");
                        TitleAPI.sendTitle(c.getPlayer(), Lang.WIN_MORE, 10, 80, 10);
                    }
                });
                TitleAPI.sendTitle(killer.getPlayer(),Lang.LOOSE, 10, 80, 10);
                TitleAPI.sendSubTitle(killer.getPlayer(),Lang.KILLER_TIME_LOOSE, 10, 80, 10);
                ss.add("Vyhrávají: "+ChatColor.GREEN+""+ChatColor.BOLD+"OBČANÉ");
                cc = ChatColor.GREEN;
                break;

            default:
                alive.forEach(c ->
                {
                    if(killer != c)
                    {
                        if(((hero != null && c.getPlayer().getDisplayName().equalsIgnoreCase(hero.getPlayer().getDisplayName()))
                                || (detectiveStatus == DetectiveStatus.Alive &&
                                c.getPlayer().getDisplayName().equalsIgnoreCase(detective.getPlayer().getDisplayName()))))
                        {
                            PointsAPI.addPoints(c.getPlayer(), 100);
                            LuckyShardsAPI.addLuckyShards(c.getPlayer(), 10);
                            if(USE_EXP)
                                LevelAPI.addXp(c.getPlayer(), 50);
                            c.getPlayer().sendMessage("§8[] §e§l+ 10 LuckyShards");
                            if(USE_EXP)
                                c.getPlayer().sendMessage("§8[] §2§l+ 50 Experiences");
                            c.getPlayer().sendMessage("§8[] §9§l+ 100 StylePoints");
                        }
                        else
                        {
                            PointsAPI.addPoints(c.getPlayer(), 50);
                            LuckyShardsAPI.addLuckyShards(c.getPlayer(), 5);
                            if(USE_EXP)
                                LevelAPI.addXp(c.getPlayer(), 20);
                            c.getPlayer().sendMessage("§8[] §e§l+ 5 LuckyShards");
                            if(USE_EXP)
                                c.getPlayer().sendMessage("§8[] §2§l+ 20 Experiences");
                            c.getPlayer().sendMessage("§8[] §9§l+ 50 StylePoints");
                        }

                        c.addScore(ScoreTable.END_ALIVE);
                        c.getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.END_ALIVE+" za přežití!");
                        TitleAPI.sendTitle(c.getPlayer(), Lang.WIN_MORE, 10, 80, 10);
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
                ss.add(ChatColor.GRAY+""+ChatColor.STRIKETHROUGH+"Vrah: "+killer.getPlayer().getDisplayName()+" ("+kkills+")");
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

        Bukkit.getScheduler().runTaskLater(Main.get(), () -> alive.forEach(this::updateScoreInDB), 10L);

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
    void setState(GameState state)
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

    void setDetective(Player hrac)
    {
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

    /*void setDetectiveStatus(DetectiveStatus newStatus) {
        detectiveStatus = newStatus;
    }*/

    // utils
    public Clovek findClovek(Player player) {
        Vector<Clovek> cc = new Vector<>();
        cc.addAll(alive);
        cc.addAll(spect);

        for (Clovek c : cc) {
            if (c.getPlayer().getUniqueId() == player.getUniqueId())
                return c;
        }

        return null;
    }

    private void updateScoreInDB(Clovek c)
    {
        try
        {
            Statement st = Main.get().getConn().createStatement();
            String sql = "UPDATE murder SET karma = "+c.getScore()+" WHERE name = '"+c.getPlayer().getDisplayName()+"';";
            st.execute(sql);
            st.close();
        }
        catch (SQLException e)
        {
            Main.get().getLogger().warning("SQL Error - Game#updateScore - e: "+e.getMessage());
        }
    }
}
