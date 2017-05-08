package sk.perri.murdermystery;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import sk.perri.murdermystery.enums.GameState;
import sk.perri.murdermystery.enums.PlayerType;

class SBManager
{
    private Scoreboard board;
    private Objective obj;

    SBManager(Player player)
    {
        board = Main.get().getServer().getScoreboardManager().getNewScoreboard();
        player.setScoreboard(board);

        obj = board.registerNewObjective("main", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(ChatColor.DARK_RED+""+ChatColor.BOLD+"Murder "+ChatColor.WHITE+""+ChatColor.BOLD+"Mystery");

        if((Main.get().getHra().getState() == GameState.Lobby) || Main.get().getHra().getState() == GameState.Starting)
            createLobbyTable();
        else
            createSpectBoard();

        for(Player p : Main.get().getServer().getOnlinePlayers())
        {
            registerPlayer(p);
        }
    }

    void registerPlayer(Player player)
    {
        if(board.getTeam(player.getDisplayName()) != null)
            return;

        Team team = board.registerNewTeam(player.getDisplayName());
        team.addPlayer(player);
        team.setNameTagVisibility(NameTagVisibility.NEVER);

        if((Main.get().getHra().getState() == GameState.Lobby) || Main.get().getHra().getState() == GameState.Starting)
            updateLobbyBoard();
    }

    void createLobbyTable()
    {
        /*
            Murder Mystery
                            7
        Hraci: 3            6
                            5
        Hra zacne o:        4
        Nie je dost hracov  3
                            2
        mc.stylecraft.cz    1
         */

        obj.getScore("  ").setScore(7);
        Team a = board.registerNewTeam("a");
        a.addPlayer(Main.get().getServer().getOfflinePlayer(ChatColor.GRAY+Lang.ONLINE_PLAYERS));
        a.setSuffix(ChatColor.RED+""+Main.get().getServer().getOnlinePlayers().size());
        obj.getScore(ChatColor.GRAY+Lang.ONLINE_PLAYERS).setScore(6);
        obj.getScore("   ").setScore(5);
        obj.getScore(ChatColor.GRAY+Lang.G_S_IN).setScore(4);
        Team b = board.registerNewTeam("b");
        b.addPlayer(Main.get().getServer().getOfflinePlayer(ChatColor.RED+""));
        b.setSuffix(Lang.W_F_PLRS);
        obj.getScore(ChatColor.RED+"").setScore(3);
        obj.getScore("    ").setScore(2);
        obj.getScore(ChatColor.GRAY+"mc.stylecraft.cz").setScore(1);
    }

    void updateLobbyBoard()
    {
        if(board.getObjective("main") == null)
        {
            createLobbyTable();
            return;
        }

        board.getTeam("a").setSuffix(ChatColor.RED+""+Main.get().getServer().getOnlinePlayers().size());
        board.getTeam("b").setSuffix(ChatColor.RED+""+
                (Main.get().getHra().getCountdown() < 0 ? Lang.W_F_PLRS : Main.get().getHra().getCountdown()));
    }

    void createGameBoard(PlayerType t)
    {
        /*
           Murder Mystery
                           12
        Rola: Obcan        11
                           10
        Cas: 3:45           9
                            8
        Pocet obcanov: 8    7
                            6
        Detektiv: ZIJE      5
                            4
        Skore: 1042         3
                            2
        mc.stylecraft.cz    1
         */

        if(board.getObjective("main") != null)
            board.getObjective("main").unregister();

        obj = board.registerNewObjective("main", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(ChatColor.DARK_RED+""+ChatColor.BOLD+"Murder "+ChatColor.WHITE+""+ChatColor.BOLD+"Mystery");

        Team c = board.registerNewTeam("c");
        c.addPlayer(Main.get().getServer().getOfflinePlayer(ChatColor.GRAY+Lang.ROLE));

        String role;
        if(t == PlayerType.Killer)
            role = ChatColor.DARK_RED+""+ChatColor.BOLD+Lang.KILLER;
        else if(t == PlayerType.Detective)
            role = ChatColor.BLUE+""+ChatColor.BOLD+Lang.DETECTIVE;
        else if(t == PlayerType.Innocent)
            role = ChatColor.GREEN+""+ChatColor.BOLD+Lang.INNOCENT;
        else
            role = ChatColor.GRAY+"NIC";
        c.setSuffix(role);

        obj.getScore(" ").setScore(12);
        obj.getScore(ChatColor.GRAY+Lang.ROLE).setScore(11);
        obj.getScore("  ").setScore(10);
        Team d = board.registerNewTeam("d");
        d.addPlayer(Main.get().getServer().getOfflinePlayer(ChatColor.GRAY+Lang.TIME));
        d.setSuffix(ChatColor.RED+"5:00");
        obj.getScore(ChatColor.GRAY+Lang.TIME).setScore(9);
        obj.getScore("   ").setScore(8);
        Team e = board.registerNewTeam("e");
        e.addPlayer(Main.get().getServer().getOfflinePlayer(ChatColor.GRAY+Lang.I_ALIVE));
        e.setSuffix(ChatColor.RED+""+Main.get().getHra().getAlive().size());
        obj.getScore(ChatColor.GRAY+Lang.I_ALIVE).setScore(7);
        obj.getScore("    ").setScore(6);
        Team f = board.registerNewTeam("f");
        f.addPlayer(Main.get().getServer().getOfflinePlayer(ChatColor.RESET+""+ChatColor.GRAY));
        f.setSuffix(Lang.DET_ALIVE);
        obj.getScore(ChatColor.RESET+""+ChatColor.GRAY).setScore(5);
        obj.getScore("     ").setScore(4);
        Team g = board.registerNewTeam("g");
        g.addPlayer(Main.get().getServer().getOfflinePlayer(ChatColor.GRAY+Lang.SCORE));
        g.setSuffix("0");
        obj.getScore(ChatColor.GRAY+Lang.SCORE).setScore(3);
        obj.getScore("      ").setScore(2);
        obj.getScore(ChatColor.GRAY+"mc.stylecraft.cz").setScore(1);
    }

    void updateGameBoard(int score)
    {
        //cas
        board.getTeam("d").setSuffix(ChatColor.RED+Main.get().getHra().getTimeString());
        //obcani
        board.getTeam("e").setSuffix(ChatColor.RED+""+Main.get().getHra().getAlive().size());
        // detektiv

        String de = "";
        switch (Main.get().getHra().getDetectiveStatus())
        {
            case Alive: de = Lang.DET_ALIVE; break;
            case New: de = Lang.BOW_PICKUPPED; break;
            case Killed: de = Lang.BOW_DROPPED; break;
            case Null: de = " "; break;
        }

        board.getTeam("f").setSuffix(de);
        //score
        board.getTeam("g").setSuffix(ChatColor.RED+""+score);
    }

    void createSpectBoard()
    {
        /*
          Murder Mystery
                            8
        Cas: 1:32           7
                            6
        Obcanov: 2          5
                            4
        Zbran je na zemi!   3
                            2
        mc.stylecraft.cz    1
         */

        if(board.getObjective("main") != null)
            board.getObjective("main").unregister();

        obj = board.registerNewObjective("main", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(ChatColor.DARK_RED+""+ChatColor.BOLD+"Murder "+ChatColor.WHITE+""+ChatColor.BOLD+"Mystery");

        obj.getScore(" ").setScore(8);
        Team h = board.registerNewTeam("h");
        h.addPlayer(Main.get().getServer().getOfflinePlayer(ChatColor.GRAY+Lang.TIME));
        h.setSuffix(ChatColor.RED+"5:00");
        obj.getScore(ChatColor.GRAY+Lang.TIME).setScore(7);
        obj.getScore("  ").setScore(6);
        Team i = board.registerNewTeam("i");
        i.addPlayer(Main.get().getServer().getOfflinePlayer(ChatColor.GRAY+Lang.I_ALIVE));
        i.setSuffix("-");
        obj.getScore(ChatColor.GRAY+Lang.I_ALIVE).setScore(5);
        obj.getScore("   ").setScore(4);
        Team j = board.registerNewTeam("j");
        j.addPlayer(Main.get().getServer().getOfflinePlayer(ChatColor.GRAY+""));
        j.setSuffix(Lang.DET_ALIVE);
        obj.getScore(ChatColor.GRAY+"").setScore(3);
        obj.getScore("    ").setScore(2);
        obj.getScore(ChatColor.GRAY+"mc.stylecraft.cz").setScore(1);
    }

    void updateSpectBoard()
    {
        //cas
        board.getTeam("h").setSuffix(ChatColor.RED+Main.get().getHra().getTimeString());
        //obcani
        board.getTeam("i").setSuffix(ChatColor.RED+""+Main.get().getHra().getAlive().size());
        // detektiv
        String de = "";
        switch (Main.get().getHra().getDetectiveStatus())
        {
            case Alive: de = Lang.DET_ALIVE; break;
            case New: de = Lang.BOW_PICKUPPED; break;
            case Killed: de = Lang.BOW_DROPPED; break;
            case Null: de = ""; break;
        }

        board.getTeam("j").setSuffix(de);
    }

    void deleteTeam(Player player)
    {
        board.getTeam(player.getDisplayName()).unregister();
    }

    Scoreboard getBoard()
    {
        return board;
    }
}
