package sk.perri.murdermystery;

import me.mirek.devtools.api.currencies.LuckyShardsAPI;
import me.mirek.devtools.api.currencies.PointsAPI;
import me.mirek.devtools.api.utils.BungeeAPI;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.golde.bukkit.corpsereborn.CorpseAPI.CorpseAPI;
import sk.perri.murdermystery.enums.DetectiveStatus;
import sk.perri.murdermystery.enums.GameOverReason;
import sk.perri.murdermystery.enums.GameState;
import sk.perri.murdermystery.enums.PlayerType;

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
    private int time = 300;
    private Clovek killer = null;
    private Clovek detective = null;
    private BukkitTask task;
    private int countdown = -1;
    private GameState state;
    private DetectiveStatus detectiveStatus = DetectiveStatus.Null;
    private Vector<String> inno = new Vector<>();

    Game() {
        state = GameState.Lobby;
    }

    void addPlayer(Player player) {
        Clovek c = new Clovek(player, new SBManager(player));

        alive.forEach(cl -> cl.getSBManager().registerPlayer(player));
        spect.forEach(cl -> cl.getSBManager().registerPlayer(player));

        if ((state == GameState.Starting || state == GameState.Lobby) && alive.size() < Main.get().getConfig().getInt("maxplayers")) {
            alive.add(c);
            player.setGameMode(GameMode.ADVENTURE);
            c.setType(PlayerType.None);
        } else {
            spect.add(c);
            c.setType(PlayerType.Spectator);
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    void removePlayer(Player player) {
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

    void killPlayer(Player killer, Player victim) {
        Clovek vrah = findClovek(killer);
        Clovek obet = findClovek(victim);

        if (vrah.getType() == PlayerType.Spectator || vrah.getType() == PlayerType.None ||
                obet.getType() == PlayerType.None || obet.getType() == PlayerType.Spectator)
            return;

        if (!(vrah.getType() == PlayerType.Killer || obet.getType() == PlayerType.Killer)) {
            vrah.addScore(ScoreTable.I_KILL_I);
            Main.get().getLogger().info("I KILL I");
            killPlayer(vrah, false);
        }

        if (vrah.getType() == PlayerType.Killer) {
            if (obet.getType() == PlayerType.Detective)
                vrah.addScore(ScoreTable.M_KILL_D);
            else
                vrah.addScore(ScoreTable.M_KILL_I);
        }

        if (obet.getType() == PlayerType.Killer)
            vrah.addScore(ScoreTable.I_KILL_M);

        killPlayer(obet, false);
    }

    // void killPlayer(Player player) { killPlayer(findClovek(player)); }

    void killPlayer(Clovek clovek, boolean voi) {
        alive.remove(clovek);
        clovek.getPlayer().sendTitle(Lang.P_MSG_KILLED, "", 10, 60, 10);
        Main.get().getServer().broadcastMessage(Lang.DEAD_MSG + " " + ChatColor.RED + clovek.getPlayer().getDisplayName());

        if (clovek.getType() == PlayerType.Detective) {
            resetBow(voi);
        }
        clovek.setType(PlayerType.Spectator);
        clovek.getPlayer().setGameMode(GameMode.SPECTATOR);
        spect.add(clovek);
        clovek.getSBManager().createSpectBoard();

        CorpseAPI.spawnCorpse(clovek.getPlayer(), clovek.getPlayer().getLocation());
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

    Clovek getKiller() {
        return killer;
    }

    // Main game STRUCTURE
    public void start(boolean force) {
        // start countdown
        if (countdown > 0) {
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

                alive.forEach(c -> c.getPlayer().sendTitle(cc + "" + ChatColor.BOLD + countdown, "", 5, 10, 5));
            }

            if (countdown <= 1) {
                stopCountdown();
                loop();
            }

            if (countdown % 10 == 0)
                Main.get().getServer().broadcastMessage(Lang.G_W_START_IN + " " + ChatColor.RED + countdown + Lang.SECONDS);

        }, 0L, 20L);
    }

    private void loop() {
        // port players to the spawn
        List<Integer> ik = new ArrayList<>();

        for (int i = 0; i < spawn.size(); i++) {
            ik.add(i);
        }
        Collections.shuffle(ik);
        state = GameState.Ingame;

        Main.get().getServer().broadcastMessage(Lang.GAME_START);

        for (int i = 0; i < alive.size(); i++) {
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
            time--;
            if (time % 3 == 0) {
                spawnItem();
            }

            if (time % 30 == 0) {
                for (Clovek c : alive) {
                    if (c == killer || c == detective)
                        continue;

                    c.addScore(ScoreTable.TIME_ALIVE);
                }
            }
            // Update scoreboards
            alive.forEach(c -> c.getSBManager().updateGameBoard(c.getScore()));
            spect.forEach(c -> c.getSBManager().updateSpectBoard());
            // Update killer compass
            murderCompass();
            // check if anyone win
            winCheck();
        }, 10, 20);
    }

    // Countdown

    private void stopCountdown() {
        Bukkit.getScheduler().cancelTask(task.getTaskId());
        countdown = -1;
        task = null;
    }

    // functions
    private void roleRole() {
        Random r = new Random();
        killer = alive.get(r.nextInt(alive.size()));
        killer.setType(PlayerType.Killer);
        killer.getPlayer().sendTitle(Lang.MURDERER_INFO_1, Lang.MURDERER_INFO_2, 20, 60, 20);
        killer.getPlayer().sendMessage(Lang.MURDERER_INFO_1 + " " + Lang.MURDERER_INFO_2 +
                " Meč hodíš podržením pravého tlačítka myši!");

        detective = alive.get(r.nextInt(alive.size()));
        while (killer.getType() == detective.getType()) {
            detective = alive.get(r.nextInt(alive.size()));
        }

        detective.setType(PlayerType.Detective);
        detective.getPlayer().sendTitle(Lang.DETECTIVE_INFO_1, Lang.DETECTIVE_INFO_2, 20, 60, 20);
        detectiveStatus = DetectiveStatus.Alive;
        detective.getPlayer().sendMessage(Lang.DETECTIVE_INFO_1 + " " + Lang.DETECTIVE_INFO_2 +
                " Po vystrelení získaš šíp po 4 sekundách!");
        civilians.add(detective.getPlayer());

        for (Clovek c : alive) {
            if (c != killer && c != detective) {
                c.getPlayer().sendTitle(Lang.INOCENT_INFO_1, Lang.INOCENT_INFO_2, 20, 60, 20);
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
            killer.getPlayer().getInventory().setItem(1, new ItemStack(Material.IRON_SWORD, 1));
            if (detective != null) {
                detective.getPlayer().getInventory().setItem(1, new ItemStack(Material.BOW, 1));
                detective.getPlayer().getInventory().setItem(2, new ItemStack(Material.ARROW, 1));
            } else {
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

    private void removeCompass() {
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

    private void spawnItem() {
        itemsLocation.get(0).getWorld().dropItemNaturally(itemsLocation.get(new Random().nextInt(itemsLocation.size())),
                new ItemStack(Material.GOLD_INGOT, 1));
    }

    void winCheck() {
        boolean go = false;
        GameOverReason reason = NULL;

        if (!alive.contains(killer)) {
            go = true;
            reason = KILLER_DEAD;
        } else if (alive.size() == 1) {
            go = true;
            reason = ALL_DEAD;
        } else if (time <= 0) {
            go = true;
            reason = TIME_OUT;
        }

        if (go) {
            state = GameState.End;
            gameOver(reason);
        }
    }

    private void resetBow(boolean voi) {
        detectiveStatus = DetectiveStatus.Killed;
        bowLocation = voi ? spawn.get(0) : detective.getPlayer().getLocation();

        detective = null;
        if (killer != null)
            killer.getPlayer().getWorld().dropItemNaturally(bowLocation, new ItemStack(Material.BOW, 1));
        Main.get().getServer().broadcastMessage(Lang.DET_DEAD);
        giveBowCompass();
    }

    private void gameOver(GameOverReason reason) {
        Bukkit.getScheduler().cancelAllTasks();

        String s = "";

        if (killer == null)
            return;

        switch (reason) {
            case ALL_DEAD:
                s += Lang.KILLER;
                Main.get().getServer().getOnlinePlayers().forEach(p ->
                        p.sendTitle(Lang.LOOSE_MORE, Lang.I_LOOSE_REASON, 10, 80, 10));
                killer.getPlayer().sendTitle(Lang.WIN, Lang.KILLER_WIN_REASON, 10, 80, 10);
                PointsAPI.addPoints(killer.getPlayer(), 100);
                LuckyShardsAPI.addLuckyShards(killer.getPlayer(), 10);
                killer.getPlayer().sendMessage("§8[] §e§l+ 10 LuckyShards");
                killer.getPlayer().sendMessage("§8[] §9§l+ 100 StylePoints");
                break;
            default:
                s += Lang.DETECTIVE;
                civilians.forEach(p -> {
                            PointsAPI.addPoints(p, 50);
                            LuckyShardsAPI.addLuckyShards(p, 5);
                            p.sendMessage("§8[] §e§l+ 5 LuckyShards");
                            p.sendMessage("§8[] §9§l+ 50 StylePoints");
                            p.sendTitle(Lang.WIN_MORE, Lang.KILLER_STOPPED, 10, 80, 10);
                        }

                );
                killer.getPlayer().sendTitle(Lang.LOOSE, Lang.KILLER_LOOSE_REASON, 10, 80, 10);
                break;
        }

        Main.get().getServer().broadcastMessage(Lang.GAME_OVER + " " + Lang.GAME_WON + s + "! " +
                Lang.KILLER_WAS + ChatColor.DARK_RED + ChatColor.BOLD + killer.getPlayer().getDisplayName());
        Main.get().getServer().broadcastMessage(Lang.SERVER_RESTART);
        Bukkit.getScheduler().runTaskLater(Main.get(), () ->
        {
            Main.get().getServer().getOnlinePlayers().forEach(BungeeAPI::sendToLobby);
        }, 400);
        Bukkit.getScheduler().runTaskLater(Main.get(), () ->
        {
            Main.get().getServer().shutdown();
        }, 550);

    }

    // Set + Get
    public void setState(GameState state) {
        this.state = state;
    }

    GameState getState() {
        return state;
    }

    int getCountdown() {
        return countdown;
    }

    Vector<Clovek> getAlive() {
        return alive;
    }

    Clovek getDetective() {
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
