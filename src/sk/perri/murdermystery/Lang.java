package sk.perri.murdermystery;

import org.bukkit.ChatColor;

class Lang
{
    private Lang() { }

    static String NOT_SETUP = ChatColor.RED+""+ChatColor.BOLD+"TATO MINIHRA JEŠTĚ NEBYLA NASTAVENÁ!";
    static String NOT_SETUP_OP = ChatColor.DARK_RED+""+ChatColor.BOLD+"NAPIŠ /setup help PRO NASTAVENÍ MINIHRY!";
    static String CONFIG_LOADED = "Soubor config.yml se načetl úspěšně!";
    static String CONFIG_CREATED = "Soubor config.yml byl vytvořen, připoj se na server jako OP a napiš \"/setup help\"!";
    static String CONFIG_LOAD_ERROR = "Při načítání souboru config.yml nastala chyba!";
    static String PLAYER = ChatColor.DARK_GREEN+"Hráč";
    static String CONNECTED = ChatColor.DARK_GREEN+"se připojil.";
    static String ABLE_TO_START = ChatColor.GREEN+"Připojilo se dostatečné množství hráčů, hra začne za "+ChatColor.RED+
            "60 sekund"+ChatColor.GREEN+"!";
    static String G_W_START_IN = ChatColor.GREEN+"Hra začne za";
    static String SECONDS = " sekund";
    static String MURDERER_INFO_1 = ChatColor.DARK_RED+"JSI VRAH!";
    static String MURDERER_INFO_2 = ChatColor.DARK_RED+"Zabij každého!";
    static String DETECTIVE_INFO_1 = ChatColor.BLUE+"JSI DETEKTIV!";
    static String DETECTIVE_INFO_2 = ChatColor.BLUE+"Zabij vraha!";
    static String INOCENT_INFO_1 = ChatColor.GREEN+"JSI OBČAN!";
    static String INOCENT_INFO_2 = ChatColor.GREEN+"Zůstaň co nejdéle naživu!";
    static String W_CD_MSG = ChatColor.DARK_GREEN+"Vrah a detektiv dostanou svoje zbraně za "+ ChatColor.RED+
            "15 sekund"+ChatColor.DARK_GREEN+"!";
    static String W_H_G = ChatColor.YELLOW+"Vrah a detektiv mají svoje zbraně!";
    static String P_MSG_KILLED = ChatColor.DARK_RED+"Umřel si!";
    static String GAME_START = ChatColor.DARK_GREEN+"HRA ZAČÍNÁ!";
    static String GAME_OVER = ChatColor.DARK_GREEN+"HRA SKONČILA!";
    static String DETECTIVE = ChatColor.BLUE+""+ChatColor.BOLD+"DETEKTIV";
    static String KILLER = ChatColor.RED+""+ChatColor.BOLD+"VRAH";
    static String INNOCENT = ChatColor.GREEN+""+ChatColor.BOLD+"OBČAN";
    static String GAME_WON = ChatColor.DARK_GREEN+"Hru vyhrává ";
    static String KILLER_WAS = ChatColor.YELLOW+"Vrah byl: ";
    static String WIN_MORE = ChatColor.GREEN+""+ChatColor.BOLD+"VYHRÁLI JSTE!";
    static String KILLER_STOPPED = ChatColor.GREEN+"Vrah byl zastaven!";
    static String LOOSE = ChatColor.RED+""+ChatColor.BOLD+"PROHRÁL JSI!";
    static String KILLER_LOOSE_REASON = ChatColor.RED+"Nezabil jsi všechny!";
    static String LOOSE_MORE = ChatColor.RED+""+ChatColor.BOLD+"PROHRÁLI JSTE!";
    static String I_LOOSE_REASON = ChatColor.RED+"Všichni občané zemřeli!";
    static String WIN = ChatColor.GREEN+""+ChatColor.BOLD+"VYHRAL JSI!";
    static String KILLER_WIN_REASON = ChatColor.GREEN+"Zabil jsi všechny!";
    static String SERVER_RESTART = ChatColor.YELLOW+"Server se restartuje za 20 sekund!";
    static String KICK_MSG = "Minihra skončila";
    static String DET_DEAD = ChatColor.GOLD+"Detektiv padl! Na místě jeho úmrtí zůstal item! Seber ho a zabij vraha!";
    static String DEAD_MSG = ChatColor.YELLOW+"Zemřel hráč ";
    static String NEW_DET = ChatColor.YELLOW+"Někdo se stal novým detektivem!";

    // SCOREBOARD
    static String GAME_NAME = ChatColor.RED+"Murder Mystery";
    static String ONLINE_PLAYERS = "Hráči: ";
    static String G_S_IN = "Hra začne za:";
    static String W_F_PLRS = "Málo hráčů";
    static String ROLE = "Role: ";
    static String TIME = "Času zůstává: ";
    static String I_ALIVE = "Občanů: ";
    static String SCORE = "Karma: ";
    static String DET_ALIVE = "Detektiv: "+ChatColor.BLUE+"ŽIJE";
    static String BOW_DROPPED = ChatColor.GOLD+"Seber luk!";
    static String BOW_PICKUPPED = ChatColor.YELLOW+"Někdo má luk!";
}
