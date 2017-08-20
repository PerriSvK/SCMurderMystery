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
    private Map<String, Clovek> ludia = new HashMap<>();
    //private Vector<Clovek> alive = new Vector<>();
    //private Vector<Clovek> spect = new Vector<>();
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
    private int live = 0;
    private final boolean USE_EXP = false;

    Game()
    {
    }

    Clovek addPlayer(Player player)
    {
        Clovek c = new Clovek(player, new SBManager(player));

        ludia.forEach((n, h) -> h.getSBManager().registerPlayer(player));

        player.setGameMode(GameMode.ADVENTURE);
        c.setType(PlayerType.None);

        if ((state == GameState.Starting || state == GameState.Lobby) && ludia.size() < Main.get().getConfig().getInt("maxplayers"))
        {
            player.setFlying(false);
            player.setAllowFlight(false);
            c.setAlive(true);
            live++;
        }
        else
        {
            player.setAllowFlight(true);
            player.setFlying(true);
            c.setAlive(false);

            Main.get().getServer().getOnlinePlayers().forEach(p -> p.hidePlayer(player));
            ludia.forEach((n, h) -> {if(!h.isAlive()) player.hidePlayer(h.getPlayer());});
            giveSpectItems(player);
        }

        ludia.put(player.getDisplayName(), c);

        return c;
    }

    void removePlayer(Player player)
    {
        if(!ludia.containsKey(player.getDisplayName()))
            return;

        if(ludia.get(player.getDisplayName()).isAlive() && state == GameState.Ingame)
        {
            ludia.get(player.getDisplayName()).addScore(ScoreTable.ALIVE_DISC);
            killPlayer(ludia.get(player.getDisplayName()), false);
            updateScoreInDB(ludia.get(player.getDisplayName()), 0);
        }

        ludia.get(player.getDisplayName()).setOnline(false);

        /*if (detective != null && detective.getPlayer().getUniqueId().equals(player.getUniqueId()))
            killPlayer(detective, false);*/

        ludia.forEach((n, h) -> h.getSBManager().deleteTeam(player));

        /*for (Clovek c : alive)
        {
            c.getSBManager().deleteTeam(player);
        }

        for (Clovek c : spect)
        {
            c.getSBManager().deleteTeam(player);
        }*/

        if((state == GameState.Starting || state == GameState.Start) && Main.get().getServer().getOnlinePlayers().size() < 3)
        {
            stopCountdown();
            state = GameState.Lobby;
            countdown = -1;
        }

        ludia.remove(player.getDisplayName());
        //alive.removeIf(c -> c.getPlayer().getUniqueId() == player.getUniqueId());
        //spect.removeIf(c -> c.getPlayer().getUniqueId() == player.getUniqueId());
    }

    void killPlayer(Player kille, Player victim)
    {
        //Clovek vrah = findClovek(kille);
        //Clovek obet = findClovek(victim);

        if (!ludia.get(kille.getDisplayName()).isAlive() || ludia.get(kille.getDisplayName()).getType() == PlayerType.None ||
                ludia.get(victim.getDisplayName()).getType() == PlayerType.None || !ludia.get(victim.getDisplayName()).isAlive())
            return;

        // Can't kill myself
        if(kille.getDisplayName().equalsIgnoreCase(victim.getDisplayName()))
            return;

        // Ino || detec kills inno
        if (!(ludia.get(kille.getDisplayName()).getType() == PlayerType.Killer ||
                ludia.get(victim.getDisplayName()).getType() == PlayerType.Killer))
        {
            ludia.get(kille.getDisplayName()).addScore(ScoreTable.I_KILL_I);
            ludia.get(kille.getDisplayName()).getPlayer().sendMessage(ChatColor.GOLD+""+ScoreTable.I_KILL_I+" za zabití občana!");
            killPlayer(ludia.get(kille.getDisplayName()), false);
        }

        // Killer kills somebody
        if (ludia.get(kille.getDisplayName()).getType() == PlayerType.Killer)
        {
            if (ludia.get(victim.getDisplayName()).getType() == PlayerType.Detective)
            {
                ludia.get(kille.getDisplayName()).addScore(ScoreTable.M_KILL_D);
                ludia.get(kille.getDisplayName()).getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.M_KILL_D+" za zabití detektiva!");
            }
            else
            {
                ludia.get(kille.getDisplayName()).addScore(ScoreTable.M_KILL_I);
                ludia.get(kille.getDisplayName()).getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.M_KILL_I+" za zabití občana!");
            }
            kkills++;
        }

        // somebody kills killer
        if (ludia.get(victim.getDisplayName()).getType() == PlayerType.Killer)
        {
            ludia.get(kille.getDisplayName()).addScore(ScoreTable.I_KILL_M);
            ludia.get(kille.getDisplayName()).getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.I_KILL_M+" za zabití vraha!");
            hero = ludia.get(kille.getDisplayName());
        }

        killPlayer(ludia.get(victim.getDisplayName()), false);
    }

    // void killPlayer(Player player) { killPlayer(findClovek(player)); }

    void killPlayer(Clovek clovek, boolean voi)
    {
        //alive.remove(clovek);
        live--;
        TitleAPI.sendTitle(clovek.getPlayer(),Lang.P_MSG_KILLED, 10, 60, 10);
        clovek.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
        Main.get().getServer().broadcastMessage(Lang.DEAD_MSG + " " + ChatColor.RED + clovek.getPlayer().getDisplayName());

        if (clovek.getType() == PlayerType.Detective)
        {
            resetBow(voi);
        }

        clovek.setAlive(false);

        //clovek.getPlayer().setGameMode(GameMode.SPECTATOR);

        Main.get().getServer().getOnlinePlayers().forEach(c ->
        {
            c.hidePlayer(clovek.getPlayer());
            c.getWorld().playSound(clovek.getPlayer().getLocation(), Sound.ENTITY_PLAYER_DEATH, 100, 1);
        });

        //spect.add(clovek);
        clovek.getSBManager().createSpectBoard();
        CorpseAPI.spawnCorpse(clovek.getPlayer(), clovek.getPlayer().getLocation());
        //CorpseAPI.spawnCorpse(clovek.getPlayer(), clovek.getPlayer().getLocation());

        // INV
        clovek.getPlayer().getInventory().clear();
        // GIVE ITEMS
        giveSpectItems(clovek.getPlayer());
        //FLY
        clovek.getPlayer().setAllowFlight(true);
        clovek.getPlayer().setFlying(true);
        //DB
        updateScoreInDB(clovek, 0);
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

            ludia.values().forEach(c -> c.getSBManager().updateLobbyBoard());

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

                ludia.forEach((n, c) ->
                {
                    TitleAPI.sendTitle(c.getPlayer(),cc + "" + countdown, 5, 10, 5);
                    c.getPlayer().getWorld().playSound(c.getPlayer().getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 25, 1);
                });
            }

            if (countdown < 1)
            {
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

        for (int i = 0; i < Main.get().getMap().getSpawn().size(); i++) { ik.add(i); }
        Collections.shuffle(ik);

        state = GameState.Ingame;
        Main.get().getServer().broadcastMessage(Lang.GAME_START);

        final int[] i = {0};
        live = 0;
        ludia.forEach((n, h) ->
        {
            if(h.isAlive())
            {
                h.getPlayer().teleport(Main.get().getMap().getSpawn().get(ik.get(i[0])));
                h.getPlayer().getInventory().clear();
                h.setType(PlayerType.Innocent);
                h.getPlayer().addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));

                i[0]++;
                live++;
            }
        });

        // Role role
        roleRole();
        giveWeapons();

        ludia.forEach((n, h) ->
        {
            h.getSBManager().createGameBoard(h.getType());
            h.getPlayer().getInventory().clear();
            h.getPlayer().getInventory().setHeldItemSlot(0);
        });

        Main.get().removeItems();
        Main.get().stopPercTimer();

        // main game loop
        task = Bukkit.getScheduler().runTaskTimer(Main.get(), () ->
        {
            countdown--; // time

            for(Map.Entry<Projectile, Particle>  en : Main.get().getSipi().entrySet())
            {
                en.getKey().getLocation().getWorld().spawnParticle(en.getValue(), en.getKey().getLocation(), 2, 0.2, 0.2, 0.2);
            }

            if(countdown % 20 != 0)
                return;

            time--;
            if (time % 3 == 0)
            {
                spawnItem();
                spawnItem();
            }

            if (time % 30 == 0)
            {
                ludia.forEach((n, h) ->
                {
                    if(h.isAlive() && h.getType() == PlayerType.Innocent)
                    {
                        h.addScore(ScoreTable.TIME_ALIVE);
                        h.getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.TIME_ALIVE+" za přežití dalších 30 sekund!");
                    }
                });
            }

            // Update scoreboards
            ludia.forEach((n, h) ->
            {
                if(h.isAlive())
                    h.getSBManager().updateGameBoard(h.getScore());
                else
                    h.getSBManager().updateSpectBoard();
            });

            // Update killer compass
            murderCompass();
            // check if anyone win
            winCheck();
        }, 10, 1);
    }

    // Countdown

    private void stopCountdown()
    {
        Bukkit.getScheduler().cancelTask(task.getTaskId());
        countdown = -1;
        task = null;
    }

    // Percentage
    void calculatePercentage()
    {
        final float[] sucet = {0, 0};
        ludia.forEach((n, h) ->
        {
            if(h.isAlive()) // TODO vyhody podla permisi
            {
                sucet[0] += h.getLkil();
                sucet[1] += h.getLdec();
            }
        });

        ludia.forEach((n, h) ->
        {
            if(h.isAlive())
            {
                h.setPerk((float) (h.getLkil() / sucet[0])*100);
                h.setPerd((float) (h.getLdec() / sucet[1])*100);
            }
        });
    }

    // functions
    private void roleRole()
    {
        // Create list
        List<Clovek> kilList = new ArrayList<>();
        List<Clovek> decList = new ArrayList<>();

        ludia.forEach((n, h) ->
        {
            if(h.isAlive())
            {
                for(int i = 0; i < h.getPerk(); i++)
                {
                    kilList.add(h);
                }

                for(int i = 0; i < h.getPerd(); i++)
                {
                    decList.add(h);
                }
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
        deName = detective.getPlayer().getDisplayName();

        ludia.forEach((n, h) ->
        {
            if(h != killer && h != detective)
            {
                TitleAPI.sendTitle(h.getPlayer(),Lang.INOCENT_INFO_1, 20, 60, 20);
                TitleAPI.sendSubTitle(h.getPlayer(),Lang.INOCENT_INFO_2, 20, 60, 20);
                h.getPlayer().sendMessage(Lang.INOCENT_INFO_1 + " " + Lang.INOCENT_INFO_2
                        + " Seber 10 goldů, získej luk a zabij vraha!");
            }
        });

        // DB INSERT - UPDATE
        try
        {
            Statement st = Main.get().getConn().createStatement();
            for(Map.Entry<String, Clovek> en : ludia.entrySet())
            {
                String sql;
                Clovek cl = en.getValue();
                int lk = cl.getLkil() < 1 ? 0 : (int) Math.round(cl.getLkil());
                int ld = cl.getLdec() < 1 ? 0 : (int) Math.round(cl.getLdec());
                String ggg = "games = "+(cl.getGames()+1);
                String swo = "sword = '"+cl.getSword().toString()+"'";

                String t = "NULL";
                if(cl.getTrail() != null)
                    t = cl.getTrail().toString();

                String tra = "trail = '"+t+"'";
                String pri = ggg+", "+swo+", "+tra;

                if(cl == killer)
                {
                    sql = "UPDATE murder SET lkil = 0, ldet = "+(ld+1)+", "+pri+" WHERE name = '"+killer.getPlayer().getDisplayName()+"';";
                }
                else if(cl == detective)
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

    private void giveWeapons()
    {
        Main.get().getServer().broadcastMessage(Lang.W_CD_MSG);

        Bukkit.getScheduler().runTaskLater(Main.get(), () ->
        {
            Main.get().getServer().broadcastMessage(Lang.W_H_G);

            killer.getPlayer().getInventory().setItem(1, new ItemStack(killer.getSword(), 1));

            if (detective != null)
            {
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

    private void giveBowCompass()
    {
        ludia.forEach((n, h) ->
        {
            if(h.isAlive() && h != killer && bowLocation != null)
            {
                h.getPlayer().setCompassTarget(bowLocation);
                ItemStack com = new ItemStack(Material.COMPASS, 1);
                h.getPlayer().getInventory().setItem(4, com);
            }
        });

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
        ludia.forEach((n, h) ->
        {
            if(h.isAlive() && h != killer)
                h.getPlayer().getInventory().remove(Material.COMPASS);
        });

        if(Main.get().getBowStand() != null)
        {
            Main.get().getBowStand().setCustomNameVisible(false);
            Main.get().getBowStand().remove();
            Main.get().setBowStand(null);
        }
    }

    private void murderCompass()
    {
        if (live == 2)
        {
            if (!killer.getPlayer().getInventory().contains(Material.COMPASS))
            {
                killer.getPlayer().getInventory().setItem(4, new ItemStack(Material.COMPASS, 1));
            }

            final Location[] ll = new Location[1];
            ludia.forEach((n, h) ->
            {
                if(h.isAlive() && h != killer)
                    ll[0] = h.getPlayer().getLocation();
            });

            if(ll[0] != null)
                killer.getPlayer().setCompassTarget(ll[0]);
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

        if(killer == null)
        {
            go = true;
            reason = KILLER_LEFT;
        }
        else if (!killer.isAlive())
        {
            go = true;
            reason = KILLER_DEAD;
        }
        else if (live == 1)
        {
            go = true;
            reason = ALL_DEAD;
        }
        else if (time <= 0)
        {
            go = true;
            reason = TIME_OUT;
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
        if(live > 1)
            Main.get().getServer().broadcastMessage(Lang.DET_DEAD);
        giveBowCompass();
    }

    private void gameOver(GameOverReason reason)
    {
        Bukkit.getScheduler().cancelAllTasks();

        Main.get().getLogger().info("Game end with reason: "+reason.name());

        if(Main.get().getSwordStand() != null)
            Main.get().getSwordStand().remove();

        Vector<String> ss = new Vector<>();
        ChatColor cc = ChatColor.RED;

        if (killer == null)
            return;

        killer.getPlayer().getWorld().playSound(killer.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 100, 1);

        ludia.forEach((n, h) ->
        {
            switch(reason)
            {
                case ALL_DEAD:
                    if(h == killer)
                    {
                        TitleAPI.sendTitle(h.getPlayer(),Lang.WIN, 10, 80, 10);
                        TitleAPI.sendSubTitle(h.getPlayer(),Lang.KILLER_WIN_REASON, 10, 80, 10);
                        PointsAPI.addPoints(h.getPlayer(), 100);
                        LuckyShardsAPI.addLuckyShards(h.getPlayer(), 10);
                        if(USE_EXP)
                            LevelAPI.addXp(h.getPlayer(), 50);
                        h.getPlayer().sendMessage("§8[] §e§l+ 10 LuckyShards");
                        if(USE_EXP)
                            h.getPlayer().sendMessage("§8[] §2§l+ 50 Experiences");
                        h.getPlayer().sendMessage("§8[] §9§l+ 100 StylePoints");
                    }
                    else
                    {
                        TitleAPI.sendTitle(h.getPlayer(), Lang.LOOSE_MORE, 10, 80, 10);
                        TitleAPI.sendSubTitle(h.getPlayer(), Lang.I_LOOSE_REASON, 10, 80, 10);
                    }
                    break;

                case KILLER_LEFT:
                case TIME_OUT:
                    TitleAPI.sendTitle(h.getPlayer(), Lang.WIN_MORE, 10, 80, 10);
                    if(h == killer)
                    {
                        TitleAPI.sendTitle(h.getPlayer(),Lang.LOOSE, 10, 80, 10);
                        TitleAPI.sendSubTitle(h.getPlayer(),Lang.KILLER_TIME_LOOSE, 10, 80, 10);
                    }
                    else if(h.isAlive())
                    {
                        h.addScore(ScoreTable.END_ALIVE);
                        h.getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.END_ALIVE+" za přežití!");
                        PointsAPI.addPoints(h.getPlayer(), 50);
                        LuckyShardsAPI.addLuckyShards(h.getPlayer(), 5);
                        if(USE_EXP)
                            LevelAPI.addXp(h.getPlayer(), 20);
                        h.getPlayer().sendMessage("§8[] §e§l+ 5 LuckyShards");
                        if(USE_EXP)
                            h.getPlayer().sendMessage("§8[] §2§l+ 20 Experiences");
                        h.getPlayer().sendMessage("§8[] §9§l+ 50 StylePoints");
                    }
                    break;

                default:
                    TitleAPI.sendTitle(h.getPlayer(), Lang.WIN_MORE, 10, 80, 10);
                    if(h == killer)
                    {
                        TitleAPI.sendTitle(h.getPlayer(),Lang.LOOSE, 10, 80, 10);
                        TitleAPI.sendSubTitle(h.getPlayer(),Lang.KILLER_LOOSE_REASON, 10, 80, 10);
                    }
                    else if(hero == h)
                    {
                        PointsAPI.addPoints(h.getPlayer(), 100);
                        LuckyShardsAPI.addLuckyShards(h.getPlayer(), 10);
                        if(USE_EXP)
                            LevelAPI.addXp(h.getPlayer(), 50);
                        h.getPlayer().sendMessage("§8[] §e§l+ 10 LuckyShards");
                        if(USE_EXP)
                            h.getPlayer().sendMessage("§8[] §2§l+ 50 Experiences");
                        h.getPlayer().sendMessage("§8[] §9§l+ 100 StylePoints");
                    }
                    else if(h.isAlive())
                    {
                        h.addScore(ScoreTable.END_ALIVE);
                        h.getPlayer().sendMessage(ChatColor.GOLD+"+"+ScoreTable.END_ALIVE+" za přežití!");
                        PointsAPI.addPoints(h.getPlayer(), 50);
                        LuckyShardsAPI.addLuckyShards(h.getPlayer(), 5);
                        if(USE_EXP)
                            LevelAPI.addXp(h.getPlayer(), 20);
                        h.getPlayer().sendMessage("§8[] §e§l+ 5 LuckyShards");
                        if(USE_EXP)
                            h.getPlayer().sendMessage("§8[] §2§l+ 20 Experiences");
                        h.getPlayer().sendMessage("§8[] §9§l+ 50 StylePoints");
                    }
            }
        });

        switch (reason)
        {
            case ALL_DEAD:
                ss.add("Vyhrává: "+ChatColor.RED+""+ChatColor.BOLD+"VRAH");
                ss.add("");
                ss.add(ChatColor.GRAY+"Vrah: "+killer.getPlayer().getDisplayName()+" ("+kkills+")");
                ss.add(ChatColor.GRAY+""+ChatColor.STRIKETHROUGH+"Detektív: "+deName);
                break;

            case KILLER_LEFT:
            case TIME_OUT:
                ss.add("Vyhrávají: "+ChatColor.GREEN+""+ChatColor.BOLD+"OBČANÉ");
                cc = ChatColor.GREEN;
                ss.add(ChatColor.GRAY+"Vrah: "+killer.getPlayer().getDisplayName()+" ("+kkills+")");
                ss.add(ChatColor.GRAY+""+((detective != null && detective.isAlive()) ? "" : ChatColor.STRIKETHROUGH)+"Detektív: "+deName);
                break;

            default:
                if(hero == detective)
                    cc = ChatColor.BLUE;
                else
                    cc = ChatColor.GREEN;

                ss.add(hero == detective ? "Vyhrává: "+ChatColor.BLUE+""+ChatColor.BOLD+"DETEKTIV" : "Vyhrávají: "+ChatColor.GREEN+""+ChatColor.BOLD+"OBČANÉ");
                ss.add("");
                ss.add(ChatColor.GRAY+""+ChatColor.STRIKETHROUGH+"Vrah: "+killer.getPlayer().getDisplayName()+" ("+kkills+")");
                ss.add(ChatColor.GRAY+""+( detective.isAlive() ? ChatColor.STRIKETHROUGH : "")+"Detektiv: "+deName);
                if(hero != null && hero.getPlayer() != null && hero != detective)
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

        Bukkit.getScheduler().runTaskLater(Main.get(), () -> ludia.values().forEach(c -> updateScoreInDB(c,
                reason == ALL_DEAD ? 1 : (hero == detective ? 2 : 0))), 10L);

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

    /*Vector<Clovek> getAlive()
    {
        return alive;
    }
    Vector<Clovek> getSpect()
    {
        return spect;
    }*/

    int getLive()
    {
        return live;
    }

    public Map<String, Clovek> getLudia()
    {
        return ludia;
    }

    public Clovek getDetective() {
        return detective;
    }

    void setDetective(Player hrac)
    {
        detective = ludia.get(hrac.getDisplayName());
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
    /*public Clovek findClovek(Player player) {
        Vector<Clovek> cc = new Vector<>();
        cc.addAll(alive);
        cc.addAll(spect);

        for (Clovek c : cc) {
            if (c.getPlayer().getUniqueId() == player.getUniqueId())
                return c;
        }

        return null;
    }*/

    private void updateScoreInDB(Clovek c, int stat) // 0 - disconnect, 1 - killer wins, 2 - detective wins
    {
        try
        {
            Statement st = Main.get().getConn().createStatement();
            String s = "";

            if(c.getType() == PlayerType.Killer && stat == 1)
                s = ", wink = "+(c.getWink()+1);

            if(c.getType() == PlayerType.Detective && stat == 2)
                s = ", wind = "+(c.getWind()+1);


            String sql = "UPDATE murder SET karma = "+c.getScore()+s+" WHERE name = '"+c.getPlayer().getDisplayName()+"';";
            st.execute(sql);
            st.close();
        }
        catch (SQLException e)
        {
            Main.get().getLogger().warning("SQL Error - Game#updateScore - e: "+e.getMessage());
        }
    }
}
