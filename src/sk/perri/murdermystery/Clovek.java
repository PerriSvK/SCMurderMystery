package sk.perri.murdermystery;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.enums.PlayerType;

public class Clovek
{
    private Player player;
    private PlayerType type;
    private SBManager board;
    private int score;
    private Material sword = Material.IRON_SWORD;
    private Particle trail = null;
    private int ldec = 0;
    private int lkil = 0;
    private float perd = 0;
    private float perk = 0;
    private boolean alive = false;
    private boolean online = true;
    private int ingCol = 0;
    private int bowRea = 0;

    public Clovek(Player player, SBManager board)
    {
        this.player = player;
        this.board = board;
        score = 0;
    }

    public void setType(PlayerType type)
    {
        this.type = type;
    }

    public Player getPlayer()
    {
        return player;
    }

    public PlayerType getType()
    {
        return type;
    }

    public SBManager getSBManager()
    {
        return board;
    }

    public void addScore(int score)
    {
        this.score += score;
    }

    public int getScore()
    {
        return score;
    }

    public Material getSword()
    {
        return sword;
    }

    public void setSword(Material sword)
    {
        this.sword = sword;
    }

    public Particle getTrail()
    {
        return trail;
    }

    public void setTrail(Particle particle)
    {
        trail = particle;
    }

    public float getPerd()
    {
        return perd;
    }

    public float getPerk()
    {
        return perk;
    }

    public double getLdec()
    {
        return Math.max(ldec, 0.5);
    }

    public double getLkil()
    {
        return Math.max(lkil, 0.5);
    }

    public void setPerd(float perd)
    {
        this.perd = perd;
    }

    public void setPerk(float perk)
    {
        this.perk = perk;
    }

    public void setLdec(int ldec)
    {
        this.ldec = ldec;
    }

    public void setLkil(int lkil)
    {
        this.lkil = lkil;
    }

    public void setAlive(boolean alive)
    {
        this.alive = alive;
    }

    public boolean isAlive()
    {
        return alive;
    }

    public void setOnline(boolean online)
    {
        this.online = online;
    }

    public boolean isOnline()
    {
        return online;
    }

    public void addIngColl()
    {
        ingCol++;
    }

    public int getIngColl()
    {
        return ingCol;
    }

    public void addBowRead() { bowRea++; }
}
