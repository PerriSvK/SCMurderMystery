package sk.perri.murdermystery.game;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.SBManager;
import sk.perri.murdermystery.enums.BetPackage;
import sk.perri.murdermystery.enums.PlayerType;

public class Clovek
{
  private Player player;
  private PlayerType type;
  private SBManager board;
  private int score = 0;
  private Material sword = Material.IRON_SWORD;
  private Particle trail = null;
  private int ldec = 0;
  private int lkil = 0;
  private float perd = 0.0F;
  private float perk = 0.0F;
  private int games = 0;
  private int wink = 0;
  private int wind = 0;
  private boolean alive = false;
  private boolean loaded = false;
  private boolean gg = false;
  private int betPackage = 1;
  private BetPackage bp = BetPackage.IRON;
  private String betPlayer = "";
  
  public Clovek(Player player, SBManager board)
  {
    this.player = player;
    this.board = board;
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
  
  public void setScore(int score)
  {
    this.score = score;
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
    return Math.max(ldec, 0.5D);
  }
  
  public double getLkil()
  {
    return Math.max(lkil, 0.5D);
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
  
  public int getGames()
  {
    return games;
  }
  
  public void setGames(int games)
  {
    this.games = games;
  }
  
  public void setAlive(boolean alive)
  {
    this.alive = alive;
  }
  
  public void setWind(int wind)
  {
    this.wind = wind;
  }
  
  public void setWink(int wink)
  {
    this.wink = wink;
  }
  
  public int getWind()
  {
    return wind;
  }
  
  public int getWink()
  {
    return wink;
  }
  
  public boolean isAlive()
  {
    return alive;
  }
  
  public boolean getGG()
  {
    return gg;
  }
  
  public void gg()
  {
    gg = true;
  }
  
  public void setBetPackage(int betPackage)
  {
    this.betPackage = betPackage;
  }
  
  public int getBetPackage()
  {
    return betPackage;
  }
  
  public String getBetPlayer()
  {
    return betPlayer;
  }
  
  public void setBp(BetPackage bp)
  {
    this.bp = bp;
  }
  
  public BetPackage getBp()
  {
    return bp;
  }
  
  public void setBetPlayer(String betPlayer)
  {
    this.betPlayer = betPlayer;
  }
  

  public String toString()
  {
    return "Nick: " + player.getDisplayName() + "; PlayerType: " + type.toString() + "; Alive = " + alive + "; GG = " + gg + "; Score = " + score + "; Games = " + games + "; PKil = " + perk + "; PDet = " + perd + "; betPlayer = " + betPlayer + "; betPak = " + betPackage;
  }

  public boolean isLoaded()
  {
    return loaded;
  }
  
  public void load()
  {
    loaded = true;
  }
}
