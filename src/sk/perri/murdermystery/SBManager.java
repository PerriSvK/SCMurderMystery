package sk.perri.murdermystery;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.enums.GameState;
import sk.perri.murdermystery.enums.PlayerType;

import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class SBManager {
    private Scoreboard board = MainMurder.get().getServer().getScoreboardManager().getNewScoreboard();
    private Objective obj;

    SBManager(Player player)
    {
        player.setScoreboard(this.board);
        this.obj = this.board.registerNewObjective("main", "dummy");
        this.obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.obj.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Murder " + ChatColor.WHITE + "" + ChatColor.BOLD + "Mystery");
        if (MainMurder.get().getHra().getState() != GameState.Lobby && MainMurder.get().getHra().getState() != GameState.Starting)
        {
            this.createSpectBoard();
        }
        else
        {
            this.createLobbyTable();
        }

        MainMurder.get().getServer().getOnlinePlayers().forEach(this::registerPlayer);
    }

    void registerPlayer(Player player) {
        if (this.board.getTeam(player.getDisplayName()) == null)
        {
            Team team = this.board.registerNewTeam(player.getDisplayName());
            team.addPlayer(player);
            team.setNameTagVisibility(NameTagVisibility.NEVER);
            if (MainMurder.get().getHra().getState() == GameState.Lobby || MainMurder.get().getHra().getState() == GameState.Starting) {
                this.updateLobbyBoard();
            }

        }
    }

    void createLobbyTable()
    {
        this.obj.getScore("  ").setScore(7);
        Team a = this.board.registerNewTeam("a");
        a.addPlayer(MainMurder.get().getServer().getOfflinePlayer(ChatColor.GRAY + Lang.ONLINE_PLAYERS));
        a.setSuffix(ChatColor.RED + "" + MainMurder.get().getServer().getOnlinePlayers().size());
        this.obj.getScore(ChatColor.GRAY + Lang.ONLINE_PLAYERS).setScore(6);
        this.obj.getScore("   ").setScore(5);
        this.obj.getScore(ChatColor.GRAY + Lang.G_S_IN).setScore(4);
        Team b = this.board.registerNewTeam("b");
        b.addPlayer(MainMurder.get().getServer().getOfflinePlayer(ChatColor.RED + ""));
        b.setSuffix(Lang.W_F_PLRS);
        this.obj.getScore(ChatColor.RED + "").setScore(3);
        this.obj.getScore("    ").setScore(2);
        this.obj.getScore(ChatColor.GRAY + "mc.stylecraft.cz").setScore(1);
    }

    void updateLobbyBoard()
    {
        if (this.board.getObjective("main") == null) {
            this.createLobbyTable();
        } else {
            try {
                this.board.getTeam("a").setSuffix(ChatColor.RED + "" + MainMurder.get().getServer().getOnlinePlayers().size());
                this.board.getTeam("b").setSuffix(ChatColor.RED + "" + (MainMurder.get().getHra().getCountdown() < 0 ? Lang.W_F_PLRS : MainMurder.get().getHra().getCountdown()));
                ((Team)((Team)this.board.getTeams().toArray()[0])).getName();
                String[] s = new String[]{""};
                this.board.getTeams().forEach((t) -> {
                    s[0] = s[0] + t.getName() + " ";
                });
            } catch (NullPointerException var2) {
                MainMurder.get().getLogger().warning("UpdateLobbyBoard - null pointer");
            }

        }
    }

    void createGameBoard(PlayerType t) {
        if (this.board.getTeam("d") == null) {
            if (this.board.getObjective("main") != null) {
                this.board.getObjective("main").unregister();
            }

            this.obj = this.board.registerNewObjective("main", "dummy");
            this.obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            this.obj.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Murder " + ChatColor.WHITE + "" + ChatColor.BOLD + "Mystery");
            Team c = this.board.registerNewTeam("c");
            c.addPlayer(MainMurder.get().getServer().getOfflinePlayer(ChatColor.GRAY + Lang.ROLE));
            String role;
            if (t == PlayerType.Killer) {
                role = ChatColor.DARK_RED + "" + ChatColor.BOLD + Lang.KILLER;
            } else if (t == PlayerType.Detective) {
                role = ChatColor.BLUE + "" + ChatColor.BOLD + Lang.DETECTIVE;
            } else if (t == PlayerType.Innocent) {
                role = ChatColor.GREEN + "" + ChatColor.BOLD + Lang.INNOCENT;
            } else {
                role = ChatColor.GRAY + "NIC";
            }

            c.setSuffix(role);
            this.obj.getScore(" ").setScore(12);
            this.obj.getScore(ChatColor.GRAY + Lang.ROLE).setScore(11);
            this.obj.getScore("  ").setScore(10);
            Team d = this.board.registerNewTeam("d");
            d.addPlayer(MainMurder.get().getServer().getOfflinePlayer(ChatColor.GRAY + Lang.TIME));
            d.setSuffix(ChatColor.RED + "5:00");
            this.obj.getScore(ChatColor.GRAY + Lang.TIME).setScore(9);
            this.obj.getScore("   ").setScore(8);
            Team e = this.board.registerNewTeam("e");
            e.addPlayer(MainMurder.get().getServer().getOfflinePlayer(ChatColor.GRAY + Lang.I_ALIVE));
            e.setSuffix(ChatColor.RED + "" + MainMurder.get().getHra().getLive());
            this.obj.getScore(ChatColor.GRAY + Lang.I_ALIVE).setScore(7);
            this.obj.getScore("    ").setScore(6);
            Team f = this.board.registerNewTeam("f");
            f.addPlayer(MainMurder.get().getServer().getOfflinePlayer(ChatColor.RESET + "" + ChatColor.GRAY));
            f.setSuffix(Lang.DET_ALIVE);
            this.obj.getScore(ChatColor.RESET + "" + ChatColor.GRAY).setScore(5);
            this.obj.getScore("     ").setScore(4);
            Team g = this.board.registerNewTeam("g");
            g.addPlayer(MainMurder.get().getServer().getOfflinePlayer(ChatColor.GRAY + Lang.SCORE));
            g.setSuffix("0");
            this.obj.getScore(ChatColor.GRAY + Lang.SCORE).setScore(3);
            this.obj.getScore("      ").setScore(2);
            this.obj.getScore(ChatColor.GRAY + "mc.stylecraft.cz").setScore(1);
        }
    }

    void updateGameBoard(int score) {
        if (this.board.getTeam("d") != null) {
            this.board.getTeam("d").setSuffix(ChatColor.RED + MainMurder.get().getHra().getTimeString());
            this.board.getTeam("e").setSuffix(ChatColor.RED + "" + (MainMurder.get().getHra().getLive() - 1));
            String de = "";
            switch(MainMurder.get().getHra().getDetectiveStatus()) {
                case Alive:
                    de = Lang.DET_ALIVE;
                    break;
                case New:
                    de = Lang.BOW_PICKUPPED;
                    break;
                case Killed:
                    de = Lang.BOW_DROPPED;
                    break;
                case Null:
                    de = " ";
            }

            this.board.getTeam("f").setSuffix(de);
            this.board.getTeam("g").setSuffix(ChatColor.RED + "" + score);
        }
    }

    void createSpectBoard() {
        if (this.board.getTeam("h") == null) {
            if (this.board.getObjective("main") != null) {
                this.board.getObjective("main").unregister();
            }

            this.obj = this.board.registerNewObjective("main", "dummy");
            this.obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            this.obj.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Murder " + ChatColor.WHITE + "" + ChatColor.BOLD + "Mystery");
            this.obj.getScore(" ").setScore(8);
            Team h = this.board.registerNewTeam("h");
            h.addPlayer(MainMurder.get().getServer().getOfflinePlayer(ChatColor.GRAY + Lang.TIME));
            h.setSuffix(ChatColor.RED + "5:00");
            this.obj.getScore(ChatColor.GRAY + Lang.TIME).setScore(7);
            this.obj.getScore("  ").setScore(6);
            Team i = this.board.registerNewTeam("i");
            i.addPlayer(MainMurder.get().getServer().getOfflinePlayer(ChatColor.GRAY + Lang.I_ALIVE));
            i.setSuffix("-");
            this.obj.getScore(ChatColor.GRAY + Lang.I_ALIVE).setScore(5);
            this.obj.getScore("   ").setScore(4);
            Team j = this.board.registerNewTeam("j");
            j.addPlayer(MainMurder.get().getServer().getOfflinePlayer(ChatColor.GRAY + ""));
            j.setSuffix(Lang.DET_ALIVE);
            this.obj.getScore(ChatColor.GRAY + "").setScore(3);
            this.obj.getScore("    ").setScore(2);
            this.obj.getScore(ChatColor.GRAY + "mc.stylecraft.cz").setScore(1);
        }
    }

    void updateSpectBoard() {
        if (this.board.getTeam("h") != null) {
            this.board.getTeam("h").setSuffix(ChatColor.RED + MainMurder.get().getHra().getTimeString());
            this.board.getTeam("i").setSuffix(ChatColor.RED + "" + (MainMurder.get().getHra().getLive() - 1));
            String de = "";
            switch(MainMurder.get().getHra().getDetectiveStatus()) {
                case Alive:
                    de = Lang.DET_ALIVE;
                    break;
                case New:
                    de = Lang.BOW_PICKUPPED;
                    break;
                case Killed:
                    de = Lang.BOW_DROPPED;
                    break;
                case Null:
                    de = "";
            }

            this.board.getTeam("j").setSuffix(de);
        }
    }

    void deleteTeam(Player player) {
        if (this.board.getTeam(player.getDisplayName()) != null) {
            this.board.getTeam(player.getDisplayName()).unregister();
        }

    }

    Scoreboard getBoard() {
        return this.board;
    }
}
