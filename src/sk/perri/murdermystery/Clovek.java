package sk.perri.murdermystery;

import org.bukkit.entity.Player;
import sk.perri.murdermystery.enums.PlayerType;

public class Clovek
{
    private Player player;
    private PlayerType type;
    private SBManager board;
    private int score;

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
}
