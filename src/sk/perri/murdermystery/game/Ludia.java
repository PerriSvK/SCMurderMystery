package sk.perri.murdermystery.game;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.enums.PlayerType;

public class Ludia {
    private static Map<Player, Clovek> vsetci = new HashMap();
    private static Clovek vrah;
    private static Clovek detektiv;
    private static Clovek hero;
    private static Clovek tester = null;
    private static Clovek bowOwner = null;
    private static Map<Player, Clovek> obcania = new HashMap();

    private Ludia() {
    }

    public static void addClovek(Clovek clovek) {
        vsetci.put(clovek.getPlayer(), clovek);
    }

    public static Clovek getClovek(Player player) {
        return (Clovek)vsetci.get(player);
    }

    public static Clovek removeClovek(Player player) {
        Clovek res = null;
        if (vsetci.containsKey(player)) {
            res = (Clovek)vsetci.get(player);
            if (obcania.containsKey(player)) {
                obcania.remove(player);
            }

            if (vsetci.get(player) == vrah) {
                vrah = null;
            }

            if (vsetci.get(player) == hero) {
                hero = null;
            }

            if (vsetci.get(player) == detektiv) {
                detektiv = null;
            }

            vsetci.remove(player);
        }

        return res;
    }

    public static void putObcania() {
        vsetci.forEach((p, c) -> {
            if (c.getType() == PlayerType.Innocent) {
                obcania.put(p, c);
            }

        });
    }

    public static Clovek getVrah() {
        return vrah;
    }

    public static Clovek getDetektiv() {
        return detektiv;
    }

    public static Clovek getHero() {
        return hero;
    }

    public static Clovek getTester() {
        return tester;
    }

    public static Map<Player, Clovek> getObcania() {
        return obcania;
    }

    public static Map<Player, Clovek> getVsetci() {
        return vsetci;
    }

    public static Clovek getBowOwner() {
        return bowOwner;
    }

    public static void setBowOwner(Clovek clovek) {
        bowOwner = clovek;
    }

    public static void setVrah(Clovek clovek) {
        vrah = clovek;
    }

    public static void setHero(Clovek clovek) {
        hero = clovek;
    }

    public static void setDetektiv(Clovek clovek) {
        detektiv = clovek;
        bowOwner = clovek;
    }

    public static void setTester(Clovek clovek) {
        tester = clovek;
    }

    public static int pocet() {
        return vsetci.size();
    }

    public static int obcanov() {
        return obcania.size();
    }
}
