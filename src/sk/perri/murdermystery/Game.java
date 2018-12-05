package sk.perri.murdermystery;

import java.util.*;
import me.mirek.devtools.api.DevTools;
import me.mirek.devtools.api.utils.TitleAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import sk.perri.murdermystery.enums.BetPackage;
import sk.perri.murdermystery.enums.DetectiveStatus;
import sk.perri.murdermystery.enums.GameOverReason;
import sk.perri.murdermystery.enums.GameState;
import sk.perri.murdermystery.enums.PlayerType;
import sk.perri.murdermystery.game.Clovek;
import sk.perri.murdermystery.game.Gadget;
import sk.perri.murdermystery.game.Ludia;
import sk.perri.murdermystery.utils.CenterMessage;

public class Game
{
    private Location bowLocation = null;
    private int time = 300;
    private BukkitTask task;
    private int countdown = -1;
    private GameState state;
    private DetectiveStatus detectiveStatus;
    private String deName;
    private String kiName;
    private int kkills;
    private int live;

    Game()
    {
        this.state = GameState.Lobby;
        this.detectiveStatus = DetectiveStatus.Null;
        this.deName = "";
        this.kiName = "";
        this.kkills = 0;
        this.live = 0;
    }

    Clovek addPlayer(Player player)
    {
        Clovek c = new Clovek(player, new SBManager(player));
        Ludia.getVsetci().forEach((n, h) -> h.getSBManager().registerPlayer(player));
        player.setGameMode(GameMode.ADVENTURE);
        c.setType(PlayerType.None);
        if ((this.state == GameState.Starting || this.state == GameState.Lobby) &&
                Ludia.pocet() < MainMurder.get().getConfig().getInt("maxplayers"))
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
            MainMurder.get().getServer().getOnlinePlayers().forEach((p) -> p.hidePlayer(player));
            Ludia.getVsetci().forEach((n, h) ->
            {
                if (!h.isAlive())
                {
                    player.hidePlayer(h.getPlayer());
                }

            });
            this.giveSpectItems(player);
        }

        Ludia.addClovek(c);
        return c;
    }

    void removePlayer(Player player)
    {
        Clovek c = Ludia.removeClovek(player);
        if (c.isAlive() && this.state == GameState.Ingame) {
            c.addScore(ScoreTable.ALIVE_DISC);
            this.killPlayer(c, false);
            MainMurder.get().getDBE().updateScoreInDB(c, 0);
        }

        Ludia.getVsetci().forEach((n, h) -> h.getSBManager().deleteTeam(player));
        if ((this.state == GameState.Starting || this.state == GameState.Start) &&
                MainMurder.get().getServer().getOnlinePlayers().size() < 3)
        {
            this.stopCountdown();
            this.state = GameState.Lobby;
            this.countdown = -1;
        }

    }

    void killPlayer(Player killer, Player victim)
    {
        Clovek vrah = Ludia.getClovek(killer);
        Clovek obet = Ludia.getClovek(victim);
        if (vrah.isAlive() && vrah.getType() != PlayerType.None &&
                obet.getType() != PlayerType.None && obet.isAlive())
        {
            if (killer != victim)
            {
                MainMurder.get().getLogger().info(killer.getDisplayName() + " kills " + victim.getDisplayName());
                if (vrah.getType() != PlayerType.Killer && obet.getType() != PlayerType.Killer)
                {
                    vrah.addScore(ScoreTable.I_KILL_I);
                    vrah.getPlayer().sendMessage(ChatColor.GOLD + "" + ScoreTable.I_KILL_I + " za zabití občana!");
                    this.killPlayer(vrah, false);
                }

                if (vrah.getType() == PlayerType.Killer)
                {
                    if (obet.getType() == PlayerType.Detective)
                    {
                        vrah.addScore(ScoreTable.M_KILL_D);
                        vrah.getPlayer().sendMessage(ChatColor.GOLD + "+" + ScoreTable.M_KILL_D + " za zabití detektiva!");
                    }
                    else
                    {
                        vrah.addScore(ScoreTable.M_KILL_I);
                        vrah.getPlayer().sendMessage(ChatColor.GOLD + "+" + ScoreTable.M_KILL_I + " za zabití občana!");
                    }

                    ++this.kkills;
                }

                if (obet.getType() == PlayerType.Killer) {
                    vrah.addScore(ScoreTable.I_KILL_M);
                    vrah.getPlayer().sendMessage(ChatColor.GOLD + "+" + ScoreTable.I_KILL_M + " za zabití vraha!");
                    Ludia.setHero(vrah);
                }
                this.killPlayer(obet, false);
            }
        }
    }

    void killPlayer(Clovek clovek, boolean voi)
    {
        if(!clovek.isAlive())
            return;

        live--;
        TitleAPI.sendTitle(clovek.getPlayer(), Lang.P_MSG_KILLED, 1/2f, 3, 1/2f);
        clovek.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
        MainMurder.get().getServer().broadcastMessage(Lang.DEAD_MSG + " " + ChatColor.RED + clovek.getPlayer().getDisplayName());
        if (Ludia.getBowOwner() == clovek)
        {
            this.resetBow(voi);
        }

        clovek.setAlive(false);
        MainMurder.get().getServer().getOnlinePlayers().forEach((c) ->
        {
            c.hidePlayer(clovek.getPlayer());
            c.getWorld().playSound(clovek.getPlayer().getLocation(), Sound.ENTITY_PLAYER_DEATH, 100.0F, 1.0F);
        });
        clovek.getSBManager().createSpectBoard();
        clovek.getPlayer().getInventory().clear();
        this.giveSpectItems(clovek.getPlayer());
        clovek.getPlayer().setAllowFlight(true);
        clovek.getPlayer().setFlying(true);
        MainMurder.get().getDBE().updateScoreInDB(clovek, 0);
    }

    void testPlayer(Player subject)
    {
        Clovek sub = Ludia.getClovek(subject);
        if (sub.isAlive())
        {
            if (sub.getType() == PlayerType.Killer)
            {
                MainMurder.get().getServer().broadcastMessage("§7Hráč §c" + subject.getDisplayName() + "§7 je " + ChatColor.DARK_RED + ChatColor.BOLD + "VRAH§r§7!");
                subject.setCustomNameVisible(true);
                subject.setPlayerListName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "VRAH " + ChatColor.RESET + subject.getDisplayName());
                Ludia.getVsetci().values().forEach((v) -> v.getSBManager().deleteTeam(subject));
            }
            else if (sub.getType() == PlayerType.Detective)
            {
                MainMurder.get().getServer().broadcastMessage("§7Hráč §c" + subject.getDisplayName() + "§7 je " + ChatColor.DARK_AQUA + ChatColor.BOLD + "DETEKTIV§r§7!");
                subject.setCustomNameVisible(true);
                subject.setPlayerListName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "DETEKTIV " + ChatColor.RESET + subject.getDisplayName());
                Ludia.getVsetci().values().forEach((v) -> v.getSBManager().deleteTeam(subject));
            }
            else
            {
                MainMurder.get().getServer().broadcastMessage("§7Hráč §c" + subject.getDisplayName() + " je §a§lOBCAN§r§7!");
            }

            ItemStack is = Ludia.getTester().getPlayer().getInventory().getItemInMainHand();
            if (is.getAmount() == 1)
            {
                Ludia.getTester().getPlayer().getInventory().setItemInMainHand(null);
            }
            else
            {
                is.setAmount(is.getAmount() - 1);
            }

        }
    }

    public void start(boolean force)
    {
        if (this.countdown > 0)
        {
            if (force && this.countdown > 16)
            {
                this.countdown = 15;
            }

        }
        else
        {
            this.countdown = force ? 6 : 60;
            this.state = GameState.Starting;
            this.task = Bukkit.getScheduler().runTaskTimer(MainMurder.get(), () ->
            {
                --this.countdown;
                Ludia.getVsetci().values().forEach((c) ->
                {
                    c.getSBManager().updateLobbyBoard();
                    if (!c.isLoaded())
                    {
                        MainMurder.get().getDBE().playerDB(c.getPlayer(), c);
                    }

                });
                if (this.countdown <= 5)
                {
                    this.state = GameState.Start;
                    ChatColor cc;
                    switch(this.countdown)
                    {
                        case 2:
                            cc = ChatColor.RED;
                            break;
                        case 3:
                            cc = ChatColor.GOLD;
                            break;
                        case 4:
                            cc = ChatColor.YELLOW;
                            break;
                        case 5:
                            cc = ChatColor.GREEN;
                            break;
                        default:
                            cc = ChatColor.DARK_RED;
                    }

                    Ludia.getVsetci().forEach((n, c) ->
                    {
                        TitleAPI.sendTitle(c.getPlayer(), cc + "" + countdown, 1/4f, 20, 1/4f);
                        c.getPlayer().getWorld().playSound(c.getPlayer().getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 25.0F, 1.0F);
                    });
                }

                if (this.countdown < 1)
                {
                    this.stopCountdown();
                    this.loop();
                }

                if (this.countdown % 10 == 0)
                {
                    MainMurder.get().getServer().broadcastMessage(Lang.G_W_START_IN + " " + ChatColor.RED + this.countdown + Lang.SECONDS);
                }

            }, 0L, 20L);
        }
    }

    private void loop()
    {
        this.completeBets();
        List<Integer> ik = new ArrayList<>();

        for(int i = 0; i < MainMurder.get().getMap().getSpawn().size(); ++i)
        {
            ik.add(i);
        }

        Collections.shuffle(ik);
        this.state = GameState.Ingame;
        MainMurder.get().getServer().broadcastMessage(Lang.GAME_START);
        int[] i = new int[]{0};
        this.live = 0;
        Ludia.getVsetci().forEach((n, h) ->
        {
            if (h.isAlive())
            {
                h.getPlayer().teleport(MainMurder.get().getMap().getSpawn().get(ik.get(i[0])));
                h.getPlayer().getInventory().clear();
                h.setType(PlayerType.Innocent);
                h.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 2147483647, 0, false, false));
                ++i[0];
                ++this.live;
            }
        });

        try
        {
            this.roleRole();
            this.giveWeapons();
            Ludia.putObcania();
            MainMurder.get().getDBE().updatePlayersStats();
        }
        catch (Exception e)
        {
            Bukkit.getLogger().warning("Error while init game: "+e.toString());
        }


        Ludia.getVsetci().forEach((n, h) ->
        {
            if (h.isAlive())
            {
                h.getSBManager().createGameBoard(h.getType());
                h.getPlayer().getInventory().clear();
                h.getPlayer().getInventory().setHeldItemSlot(0);
            }
            else
            {
                h.getSBManager().createSpectBoard();
                h.getPlayer().getInventory().clear();
                h.getPlayer().getInventory().setHeldItemSlot(0);
                this.giveSpectItems(h.getPlayer());
            }

        });

        MainMurder.get().removeItems();
        MainMurder.get().stopPercTimer();
        this.task = Bukkit.getScheduler().runTaskTimer(MainMurder.get(), () ->
        {
            --this.countdown;

            MainMurder.get().getSipi().forEach((ena, enb) -> ena.getLocation().getWorld().spawnParticle(enb,
                    ena.getLocation(), 2, 0.2D, 0.2D, 0.2D));

            if (this.countdown % 20 == 0)
            {
                --this.time;
                if (this.time % 3 == 0)
                {
                    this.spawnItem();
                    this.spawnItem();
                }

                if (this.time % 30 == 0)
                {
                    Ludia.getObcania().forEach((n, h) ->
                    {
                        if (h.isAlive())
                        {
                            h.addScore(ScoreTable.TIME_ALIVE);
                            h.getPlayer().sendMessage(ChatColor.GOLD + "+" + ScoreTable.TIME_ALIVE + " za přežití dalších 30 sekund!");
                        }

                    });
                }

                Ludia.getVsetci().forEach((n, h) ->
                {
                    if (h.isAlive()) {
                        h.getSBManager().updateGameBoard(h.getScore());
                    } else {
                        h.getSBManager().updateSpectBoard();
                    }

                });
                this.murderCompass();
                this.winCheck();
            }
        }, 10L, 1L);
    }

    private void completeBets()
    {
        final String[] info = {"BETS info: "};

        Ludia.getVsetci().forEach((p, c) ->
        {
            if (c.getBp() != BetPackage.NONE && !c.getBetPlayer().equals("")) {
                info[0] = info[0] + "[" + c.getPlayer().getDisplayName() + " bet on " + c.getBetPlayer() + " (" + c.getBetPackage() + ")";
                if (DevTools.getUser(c.getPlayer()).getPoints() < c.getBp().getCostStylepoints()) {
                    c.getPlayer().sendMessage("§cNemáš dostatok §6StylePoints§c! Tvoj tip nebude započítaný!");
                    c.setBp(BetPackage.NONE);
                    c.setBetPlayer("");
                    info[0] = info[0] + " NA -> SP], ";
                } else if (DevTools.getUser(c.getPlayer()).getCredits() < c.getBp().getCostStylekredits()) {
                    c.getPlayer().sendMessage("§cNemáš dostatok §aStyleKreditov§c! Tvoj tip nebude započítaný!");
                    c.setBp(BetPackage.NONE);
                    c.setBetPlayer("");
                    info[0] = info[0] + " NA -> KR], ";
                } else {
                    if (c.getBp().getCostStylekredits() != 0) {
                        DevTools.getUser(c.getPlayer()).removeCredits(c.getBp().getCostStylekredits(), false);
                    }

                    DevTools.getUser(c.getPlayer()).removePoints(c.getBp().getCostStylepoints());
                    c.getPlayer().sendMessage("§aTvoj tip sa započítal! Výhru môžeš získať pri vyhodnotení!");
                    info[0] = info[0] + " A], ";
                }
            } else {
                info[0] = info[0] + "[" + c.getPlayer().getDisplayName() + " makes no bets], ";
            }
        });

        MainMurder.get().getLogger().info(info[0]);
    }

    private void evaluateBets()
    {
        Ludia.getVsetci().values().forEach(c ->
        {
            if (c.getBp() != BetPackage.NONE && !c.getBetPlayer().equals(""))
            {
                if (this.kiName.contains(c.getBetPlayer()))
                {
                    c.getPlayer().sendMessage("§a§lGRATULUJEME! §r§aPodarilo sa ti uhádnuť vraha! Tu je tvoja odmena:");
                    DevTools.getUser(c.getPlayer()).addPoints(c.getBp().getPrizeStylepoints(), true);
                    DevTools.getLevelManager().addExp(c.getPlayer().getDisplayName(), c.getBp().getPrizeExperience(), true);
                }
                else
                {
                    c.getPlayer().sendMessage("§cBohužial sa ti nepodarilo odhaliť vraha");
                }
            }
        });
    }

    private void stopCountdown()
    {
        Bukkit.getScheduler().cancelTask(this.task.getTaskId());
        this.countdown = -1;
        this.task = null;
    }

    public void calculatePercentage()
    {
        float[] sucet = new float[]{0.0F, 0.0F};
        Ludia.getVsetci().forEach((n, h) ->
        {
            if (h.isAlive())
            {
                sucet[0] = (float)((double)sucet[0] + h.getLkil());
                sucet[1] = (float)((double)sucet[1] + h.getLdec());
            }

        });
        Ludia.getVsetci().forEach((n, h) ->
        {
            if (h.isAlive())
            {
                h.setPerk((float)(h.getLkil() / (double)sucet[0]) * 100.0F);
                h.setPerd((float)(h.getLdec() / (double)sucet[1]) * 100.0F);
            }

        });
    }

    private void roleRole() throws Exception
    {
        List<Clovek> kilList = new ArrayList<>();
        List<Clovek> decList = new ArrayList<>();
        Ludia.getVsetci().forEach((n, h) ->
        {
            if (h.isAlive())
            {
                int i;
                for(i = 0; (float)i < h.getPerk(); ++i)
                {
                    kilList.add(h);
                }

                for(i = 0; (float)i < h.getPerd(); ++i)
                {
                    decList.add(h);
                }
            }
        });

        Random r = new Random();
        int k = r.nextInt(kilList.size());
        Ludia.setVrah(kilList.get(k));
        MainMurder.get().getLogger().info("Killer - POS: " + k + " NAME: " + Ludia.getVrah().getPlayer().getDisplayName());
        Ludia.getVrah().setType(PlayerType.Killer);
        TitleAPI.sendTitle(Ludia.getVrah().getPlayer(), Lang.MURDERER_INFO_1, 1, 3, 1);
        TitleAPI.sendSubTitle(Ludia.getVrah().getPlayer(), Lang.MURDERER_INFO_2, 1, 3, 1);
        Ludia.getVrah().getPlayer().sendMessage(Lang.MURDERER_INFO_1 + " " + Lang.MURDERER_INFO_2 + " Meč hodíš podržením pravého tlačítka myši!");
        int d = r.nextInt(decList.size());
        Ludia.setDetektiv(decList.get(d));

        while(Ludia.getVrah().getType() == Ludia.getDetektiv().getType())
        {
            d = r.nextInt(decList.size());
            Ludia.setDetektiv(decList.get(d));
        }

        MainMurder.get().getLogger().info("Detective - POS: " + d + " NAME: " + Ludia.getDetektiv().getPlayer().getDisplayName());
        Ludia.getDetektiv().setType(PlayerType.Detective);
        TitleAPI.sendTitle(Ludia.getDetektiv().getPlayer(), Lang.DETECTIVE_INFO_1, 1, 3, 1);
        TitleAPI.sendSubTitle(Ludia.getDetektiv().getPlayer(), Lang.DETECTIVE_INFO_2, 1, 3, 1);
        this.detectiveStatus = DetectiveStatus.Alive;
        Ludia.getDetektiv().getPlayer().sendMessage(Lang.DETECTIVE_INFO_1 + " " + Lang.DETECTIVE_INFO_2 + " Po vystrelení získaš šíp po 4 sekundách!");
        this.deName = Ludia.getDetektiv().getPlayer().getDisplayName();
        this.kiName = Ludia.getVrah().getPlayer().getDisplayName();

        List<Clovek> tes = new ArrayList<>();
        Ludia.getVsetci().values().forEach(ccl -> { if(ccl.isAlive()) tes.add(ccl); });
        if (tes.size() > 2)
        {
            do
            {
                int te = r.nextInt(tes.size());
                if (Ludia.getVrah() != tes.get(te) && Ludia.getDetektiv() != tes.get(te))
                {
                    Ludia.setTester(tes.get(te));
                    MainMurder.get().getLogger().info("Tester: " + (tes.get(te)).getPlayer().getDisplayName());
                    (tes.get(te)).getPlayer().sendMessage("§2Super! Máš paličku na odhalenie hráča! Klikni na niekoho a všetkým sa ukáže jeho rola!");
                    break;
                }
            } while(Ludia.getTester() == null);
        }

        Ludia.getVsetci().forEach((n, h) ->
        {
            if (h != Ludia.getVrah() && h != Ludia.getDetektiv())
            {
                TitleAPI.sendTitle(h.getPlayer(), Lang.INOCENT_INFO_1, 1, 3, 1);
                TitleAPI.sendSubTitle(h.getPlayer(), Lang.INOCENT_INFO_2, 1, 3, 1);
                h.getPlayer().sendMessage(Lang.INOCENT_INFO_1 + " " + Lang.INOCENT_INFO_2 + " Seber 10 goldů, získej luk a zabij vraha!");
            }

        });
    }

    private void giveWeapons()
    {
        MainMurder.get().getServer().broadcastMessage(Lang.W_CD_MSG);
        Bukkit.getScheduler().runTaskLater(MainMurder.get(), () ->
        {
            MainMurder.get().getServer().broadcastMessage(Lang.W_H_G);
            Ludia.getVrah().getPlayer().getInventory().setItem(1, new ItemStack(Ludia.getVrah().getSword(), 1));
            if (Ludia.getDetektiv() != null)
            {
                Ludia.getDetektiv().getPlayer().getInventory().setItem(1, new ItemStack(Material.BOW, 1));
                Ludia.getDetektiv().getPlayer().getInventory().setItem(2, new ItemStack(Material.ARROW, 1));
            }
            else
            {
                if (this.bowLocation == null)
                {
                    this.bowLocation = MainMurder.get().getMap().getSpawn().get(0);
                }

                Ludia.getVrah().getPlayer().getWorld().dropItemNaturally(this.bowLocation, new ItemStack(Material.BOW, 1));
                MainMurder.get().getServer().broadcastMessage(Lang.DET_DEAD);
                this.giveBowCompass();
            }

            if (Ludia.getTester() != null)
            {
                Ludia.getTester().getPlayer().getInventory().setItem(0, new ItemStack(Material.STICK, 3));
            }

        }, 300L);
    }

    private void giveBowCompass()
    {
        try
        {
            Ludia.getVsetci().forEach((n, h) ->
            {
                if (h.isAlive() && h != Ludia.getVrah() && this.bowLocation != null)
                {
                    h.getPlayer().setCompassTarget(this.bowLocation);
                    ItemStack com = new ItemStack(Material.COMPASS, 1);
                    h.getPlayer().getInventory().setItem(4, com);
                }

            });

            if (this.bowLocation != null)
            {
                ArmorStand bs = this.bowLocation.getWorld().spawn(this.bowLocation, ArmorStand.class);
                bs.setVisible(false);
                bs.setCustomName(ChatColor.BLUE + "" + ChatColor.BOLD + "LUK");
                bs.setCustomNameVisible(true);
                MainMurder.get().setBowStand(bs);
            }
        }
        catch(Exception e)
        {
            MainMurder.get().getLogger().warning("Some error has occured in giveBowCompass: "+e.toString());
        }
    }

    void removeCompass()
    {
        try
        {
            Ludia.getVsetci().forEach((n, h) ->
            {
                if (h.isAlive() && h != Ludia.getVrah())
                {
                    h.getPlayer().getInventory().remove(Material.COMPASS);
                }

            });
            if (MainMurder.get().getBowStand() != null)
            {
                MainMurder.get().getBowStand().setCustomNameVisible(false);
                MainMurder.get().getBowStand().remove();
                MainMurder.get().setBowStand(null);
            }

            this.detectiveStatus = DetectiveStatus.New;
        }
        catch (Exception e)
        {
            MainMurder.get().getLogger().warning("Some error has occured in removeCompass: "+e.toString());
        }
    }

    private void murderCompass()
    {
        try {
            if (this.live == 2) {
                if (!Ludia.getVrah().getPlayer().getInventory().contains(Material.COMPASS)) {
                    Ludia.getVrah().getPlayer().getInventory().setItem(4, new ItemStack(Material.COMPASS, 1));
                }

                Location[] ll = new Location[1];
                Ludia.getVsetci().forEach((n, h) -> {
                    if (h.isAlive() && h != Ludia.getVrah()) {
                        ll[0] = h.getPlayer().getLocation();
                    }

                });
                if (ll[0] != null) {
                    Ludia.getVrah().getPlayer().setCompassTarget(ll[0]);
                }
            }
        } catch (Exception var2) {
            MainMurder.get().getLogger().warning("Exception - murderCompass: " + var2.toString());
        }

    }

    private void spawnItem()
    {
        MainMurder.get().getMap().getItems().get(0).getWorld().dropItemNaturally(
                MainMurder.get().getMap().getItems().get((new Random()).nextInt(
                        MainMurder.get().getMap().getItems().size())), new ItemStack(Material.GOLD_INGOT, 1));
    }

    private void winCheck()
    {
        boolean go = false;
        GameOverReason reason = GameOverReason.NULL;
        if (Ludia.getVrah() == null)
        {
            go = true;
            reason = GameOverReason.KILLER_LEFT;
        }
        else if (!Ludia.getVrah().isAlive())
        {
            go = true;
            reason = GameOverReason.KILLER_DEAD;
        }
        else if (this.live == 1)
        {
            go = true;
            reason = GameOverReason.ALL_DEAD;
        }
        else if (this.time <= 0)
        {
            go = true;
            reason = GameOverReason.TIME_OUT;
        }

        if (go)
        {
            this.state = GameState.End;
            try
            {
                this.gameOver(reason);
            }
            catch (Exception e)
            {
                MainMurder.get().getLogger().warning("Some error has occured in gameOver: "+e.toString());
            }
        }

    }

    private void resetBow(boolean voi)
    {
        try
        {
            this.detectiveStatus = DetectiveStatus.Killed;
            this.bowLocation = voi ? MainMurder.get().getMap().getSpawn().get(0) : Ludia.getBowOwner().getPlayer().getLocation();
            if (Ludia.getVrah() != null)
            {
                Ludia.getVrah().getPlayer().getWorld().dropItemNaturally(this.bowLocation, new ItemStack(Material.BOW, 1));
            }

            if (this.live > 1)
            {
                MainMurder.get().getServer().broadcastMessage(Lang.DET_DEAD);
            }

            this.giveBowCompass();
        }
        catch (NullPointerException var3)
        {
            MainMurder.get().getLogger().warning("NULL POINTER - Reset bow: " + var3.toString());
        }
    }

    private void gameOver(GameOverReason reason) throws Exception
    {
        Bukkit.getScheduler().cancelAllTasks();
        MainMurder.get().getLogger().info("Game end with reason: " + reason.name());
        if (MainMurder.get().getSwordStand() != null)
        {
            MainMurder.get().getSwordStand().remove();
        }

        Vector<String> ss = new Vector<>();
        ChatColor cc = ChatColor.RED;
        MainMurder.get().getMap().getSpawn().get(0).getWorld().playSound(MainMurder.get().getMap().getSpawn().get(0), Sound.ENTITY_PLAYER_LEVELUP, 100.0F, 1.0F);
        Ludia.getVsetci().forEach((n, h) ->
        {
            switch (reason)
            {
                case ALL_DEAD:
                    if (h == Ludia.getVrah())
                    {
                        TitleAPI.sendTitle(h.getPlayer(), Lang.WIN, 1, 4, 1);
                        TitleAPI.sendSubTitle(h.getPlayer(), Lang.KILLER_WIN_REASON, 1, 4, 1);
                        DevTools.getUser(h.getPlayer()).addPoints(100, true);
                        DevTools.getUser(h.getPlayer()).addLuckyShards(10, true);
                        DevTools.getLevelManager().addExp(h.getPlayer().getDisplayName(), 50, true);
                    }
                    else
                    {
                        TitleAPI.sendTitle(h.getPlayer(), Lang.LOOSE_MORE, 1, 4, 1);
                        TitleAPI.sendSubTitle(h.getPlayer(), Lang.I_LOOSE_REASON, 1, 4, 1);
                    }
                    break;
                case KILLER_LEFT:
                case TIME_OUT:
                    TitleAPI.sendTitle(h.getPlayer(), Lang.WIN_MORE, 1, 4, 1);
                    if (h == Ludia.getVrah())
                    {
                        TitleAPI.sendTitle(h.getPlayer(), Lang.LOOSE, 1, 4, 1);
                        TitleAPI.sendSubTitle(h.getPlayer(), Lang.KILLER_TIME_LOOSE, 1, 4, 1);
                    }
                    else if (h.isAlive())
                    {
                        h.addScore(ScoreTable.END_ALIVE);
                        h.getPlayer().sendMessage(ChatColor.GOLD + "+" + ScoreTable.END_ALIVE + " za přežití!");
                        MainMurder.get().getLogger().info("Add points to: " + h.getPlayer().getDisplayName());
                        DevTools.getUser(h.getPlayer()).addPoints(50, true);
                        DevTools.getUser(h.getPlayer()).addLuckyShards(5, true);
                        DevTools.getLevelManager().addExp(h.getPlayer().getDisplayName(), 20, true);
                    }
                    break;
                default:
                    TitleAPI.sendTitle(h.getPlayer(), Lang.WIN_MORE, 1, 4, 1);
                    if (h == Ludia.getVrah())
                    {
                        TitleAPI.sendTitle(h.getPlayer(), Lang.LOOSE, 1, 4, 1);
                        TitleAPI.sendSubTitle(h.getPlayer(), Lang.KILLER_LOOSE_REASON, 1, 4, 1);
                    }
                    else if (Ludia.getHero() == h)
                    {
                        MainMurder.get().getLogger().info("Add points to: " + h.getPlayer().getDisplayName() + " " + h.getPlayer().getName() + " p: " + (Bukkit.getPlayer(h.getPlayer().getDisplayName()) == null));
                        DevTools.getUser(h.getPlayer()).addPoints(100, true);
                        DevTools.getUser(h.getPlayer()).addLuckyShards(10, true);
                        DevTools.getLevelManager().addExp(h.getPlayer().getDisplayName(), 50, true);
                    }
                    else if (h.isAlive())
                    {
                        MainMurder.get().getLogger().info("Add points to: " + h.getPlayer().getDisplayName());
                        h.addScore(ScoreTable.END_ALIVE);
                        h.getPlayer().sendMessage(ChatColor.GOLD + "+" + ScoreTable.END_ALIVE + " za přežití!");
                        DevTools.getUser(h.getPlayer()).addPoints(50, true);
                        DevTools.getUser(h.getPlayer()).addLuckyShards(5, true);
                        DevTools.getLevelManager().addExp(h.getPlayer().getDisplayName(), 20, true);
                    }
            }

        });
        switch(reason)
        {
            case ALL_DEAD:
                ss.add("Vyhrává: " + ChatColor.RED + "" + ChatColor.BOLD + "VRAH");
                ss.add("");
                ss.add(ChatColor.GRAY + "Vrah: " + this.kiName + " (" + this.kkills + ")");
                ss.add(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "Detektív: " + this.deName);
                break;
            case KILLER_LEFT:
            case TIME_OUT:
                ss.add("Vyhrávají: " + ChatColor.GREEN + "" + ChatColor.BOLD + "OBČANÉ");
                cc = ChatColor.GREEN;
                ss.add(ChatColor.GRAY + "Vrah: " + this.kiName + " (" + this.kkills + ")");
                ss.add(ChatColor.GRAY + "" + (Ludia.getDetektiv() != null && Ludia.getDetektiv().isAlive() ? "" : ChatColor.STRIKETHROUGH) + "Detektív: " + this.deName);
                break;
            default:
                if (Ludia.getHero() == Ludia.getDetektiv())
                {
                    cc = ChatColor.BLUE;
                }
                else
                {
                    cc = ChatColor.GREEN;
                }

                ss.add(Ludia.getHero() == Ludia.getDetektiv() ? "Vyhrává: " + ChatColor.BLUE + "" + ChatColor.BOLD + "DETEKTIV" : "Vyhrávají: " + ChatColor.GREEN + "" + ChatColor.BOLD + "OBČANÉ");
                ss.add("");
                ss.add(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "Vrah: " + Ludia.getVrah().getPlayer().getDisplayName() + " (" + this.kkills + ")");
                ss.add(ChatColor.GRAY + "" + (Ludia.getDetektiv() != null && Ludia.getDetektiv().isAlive() ? "" : ChatColor.STRIKETHROUGH) + "Detektiv: " + this.deName);
                if (Ludia.getHero() != null && Ludia.getHero().getPlayer() != null && !Ludia.getHero().getPlayer().getDisplayName().contains(this.deName)) {
                    ss.add(ChatColor.GRAY + "Hrdina: " + Ludia.getHero().getPlayer().getDisplayName());
                }
        }

        List<String> mess = new ArrayList<>();
        mess.add(cc + "" + ChatColor.STRIKETHROUGH + "-----------------------------------------------------");
        mess.add(ChatColor.DARK_RED + "    " + ChatColor.BOLD + "Murder " + ChatColor.WHITE + ChatColor.BOLD + "Mystery");
        mess.add("");
        mess.addAll(ss);
        mess.add(cc + "" + ChatColor.STRIKETHROUGH + "-----------------------------------------------------");

        mess.forEach(s -> MainMurder.get().getLogger().info(s));

        String isMurderAlive = Ludia.getVrah() != null && Ludia.getVrah().isAlive() ? "Yep" : "Nope";
        String isDetectiveAlive = Ludia.getDetektiv() != null && Ludia.getDetektiv().isAlive() ? "Yep" : "Nope";
        MainMurder.get().getLogger().info("Is murder alive -> " + isMurderAlive + " Is detective alive -> " + isDetectiveAlive);

        MainMurder.get().getServer().getOnlinePlayers().forEach(pl ->
                mess.forEach(sss -> CenterMessage.sendCenteredMessage(pl, sss)));

        this.evaluateBets();
        MainMurder.get().getServer().broadcastMessage(Lang.SERVER_RESTART);
        Bukkit.getScheduler().runTaskLater(MainMurder.get(), () ->
            Ludia.getVsetci().values().forEach((c) ->
                MainMurder.get().getDBE().updateScoreInDB(c, reason == GameOverReason.ALL_DEAD ? 1 :
                        (Ludia.getHero() == Ludia.getDetektiv() ? 2 : 0))), 10L);

        Bukkit.getScheduler().runTaskLater(MainMurder.get(), () ->
            MainMurder.get().getServer().getOnlinePlayers().forEach(pls -> DevTools.getBungeeCord().sendToLobby(pls))
        , 400L);

        Gadget.reviveBlocks();

        Bukkit.getScheduler().runTaskLater(MainMurder.get(), () -> MainMurder.get().getServer().shutdown(), 550L);
    }

    private void giveSpectItems(Player player) {
        ItemStack btl = new ItemStack(Material.BED, 1);
        ItemMeta btlim = btl.getItemMeta();
        btlim.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "ZPĚT DO LOBBY");
        btl.setItemMeta(btlim);
        player.getInventory().setItem(8, btl);
        ItemStack co = new ItemStack(Material.COMPASS, 1);
        ItemMeta coim = co.getItemMeta();
        coim.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "HRÁČI");
        co.setItemMeta(coim);
        player.getInventory().setItem(0, co);
    }

    public GameState getState() {
        return this.state;
    }

    public void setState(GameState newState) { state = newState; }

    int getCountdown() {
        return this.countdown;
    }

    int getLive() {
        return this.live;
    }

    String getTimeString() {
        int mins = Math.floorDiv(this.time, 60);
        String secs = String.format("%02d", this.time % 60);
        return mins + ":" + secs;
    }

    DetectiveStatus getDetectiveStatus() {
        return this.detectiveStatus;
    }
}
