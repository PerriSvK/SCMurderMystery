package sk.perri.murdermystery.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Logger;

import me.mirek.devtools.api.database.DBPool;
import me.mirek.devtools.api.database.Database;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import sk.perri.murdermystery.MainMurder;
import sk.perri.murdermystery.enums.PlayerType;
import sk.perri.murdermystery.game.Clovek;
import sk.perri.murdermystery.game.Ludia;

public class DBExecutor
{
    private Database db, dbs;
    private Connection conn, conns;


    public DBExecutor()
    {
        db = DBPool.STATS;
        db.openConnection();
        conn = db.getConnection();

        dbs = DBPool.STYLECRAFT;
        dbs.openConnection();
        conns = dbs.getConnection();
    }

    public void close()
    {
        db.closeConnection();
    }

    public Vector<String> getTOP()
    {
        Vector<String> top = new Vector();

        try
        {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM murder ORDER BY karma DESC LIMIT 3");
            while (rs.next())
            {
                top.add(rs.getString("name") + " - " + rs.getInt("karma"));
            }

            rs = st.executeQuery("SELECT * FROM murder ORDER BY games DESC LIMIT 3");
            while (rs.next())
            {
                top.add(rs.getString("name") + " - " + rs.getInt("games"));
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            MainMurder.get().getLogger().info("NO DB!");
        }

        return top;
    }

    public void playerDB(Player p, Clovek c)
    {
        try
        {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM murder WHERE name = '" + p.getDisplayName() + "';");
            if (rs.next())
            {
                c.setLdec(rs.getInt("ldet"));
                c.setLkil(rs.getInt("lkil"));
                c.setScore(rs.getInt("karma"));
                c.setGames(rs.getInt("games"));
                c.setWind(rs.getInt("wind"));
                c.setWink(rs.getInt("wink"));
                c.setSword(Material.matchMaterial(rs.getString("sword")));
                c.load();
                if (rs.getString("trail").equalsIgnoreCase("null"))
                {
                    c.setTrail(null);
                }
                else
                {
                    c.setTrail(Particle.valueOf(rs.getString("trail")));
                }
                MainMurder.get().getLogger().info("Hrac " + p.getDisplayName() + " nacitany, karma: " + c.getScore());
            }
            else
            {
                String sql = "INSERT INTO murder(name, uuid, lkil, ldet, karma, games, sword, trail, wink, wind)VALUES('" + p.getDisplayName() + "', '" + p.getUniqueId() + "', 0, 0, 0, 0, 'IRON_SWORD', 'NULL', 0, 0);";


                st.execute(sql);
                MainMurder.get().getLogger().info("Player " + p.getDisplayName() + " has connected for first time, creating row in DB!");

                c.setLdec(0);
                c.setLkil(0);
                c.load();
            }
            st.close();
            MainMurder.get().getHra().calculatePercentage();
        }
        catch (SQLException e)
        {
            MainMurder.get().getLogger().warning("SQLError - onJoin e:" + e.getMessage());
        }
        catch (Exception e)
        {
            MainMurder.get().getLogger().warning("Error with SQL: " + e.toString());
        }
    }

    public void updatePlayersStats()
    {
        try
        {
            Statement st = conn.createStatement();
            for (Map.Entry<Player, Clovek> en : Ludia.getVsetci().entrySet())
            {

                Clovek cl = (Clovek) en.getValue();
                int lk = cl.getLkil() < 1.0D ? 0 : (int) Math.round(cl.getLkil());
                int ld = cl.getLdec() < 1.0D ? 0 : (int) Math.round(cl.getLdec());
                String ggg = "games = " + (cl.getGames() + 1);
                String swo = "sword = '" + cl.getSword().toString() + "'";

                String t = "NULL";
                if (cl.getTrail() != null)
                {
                    t = cl.getTrail().toString();
                }
                String tra = "trail = '" + t + "'";
                String pri = ggg + ", " + swo + ", " + tra;
                String sql;
                if (cl == Ludia.getVrah())
                {
                    sql = "UPDATE murder SET lkil = 0, ldet = " + (ld + 1) + ", " + pri + " WHERE name = '" + Ludia.getVrah().getPlayer().getDisplayName() + "';";
                }
                else
                {
                    if (cl == Ludia.getDetektiv())
                    {
                        sql = "UPDATE murder SET ldet = 0, lkil = " + (lk + 1) + ", " + pri + " WHERE name = '" + Ludia.getDetektiv().getPlayer().getDisplayName() + "';";
                    }
                    else
                    {
                        sql = "UPDATE murder SET ldet = " + (ld + 1) + ", lkil = " + (lk + 1) + ", " + pri + " WHERE name = '" + cl.getPlayer().getDisplayName() + "';";
                    }
                }
                st.execute(sql);
            }

            st.close();
        }
        catch (SQLException e)
        {
            MainMurder.get().getLogger().warning("SQL Error - Game#roleRole - e: " + e.getMessage());
        }
    }

    public void updateScoreInDB(Clovek c, int stat)
    {
        try
        {
            Statement st = conn.createStatement();
            String s = "";

            if ((c.getType() == PlayerType.Killer) && (stat == 1))
            {
                s = ", wink = " + (c.getWink() + 1);
            }
            if ((c.getType() == PlayerType.Detective) && (stat == 2))
            {
                s = ", wind = " + (c.getWind() + 1);
            }
            String sql = "UPDATE murder SET karma = " + c.getScore() + s + " WHERE name = '" + c.getPlayer().getDisplayName() + "';";
            st.execute(sql);
            st.close();
        }
        catch (SQLException e)
        {
            MainMurder.get().getLogger().warning("SQL Error - Game#updateScore - e: " + e.getMessage());
        }
    }

    public void aliveRequest()
    {
        try
        {
            Statement st = conn.createStatement();
            st.executeQuery("SELECT * FROM murder LIMIT 1");
            st.close();
        }
        catch (Exception e)
        {
            MainMurder.get().getLogger().warning("Error - DBAliveRequest - e: " + e.getMessage());
        }
    }

    public void runSql(Player p, String sql)
    {
        if(!sql.contains("LIMIT") && !sql.contains("limit") && (sql.contains("SELECT") || sql.contains("select")))
        {
            sql += " LIMIT 10";
        }

        try
        {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            String res = "";

            while(rs.next())
            {
                String row = "";

                for(int i = 0; i < rs.getMetaData().getColumnCount(); i++)
                {
                    row += rs.getString(i+1)+" ";
                }

                res += row+"\n";
            }

            st.close();
            p.sendMessage(res);
        }
        catch (SQLException e)
        {
            p.sendMessage(e.getMessage());
        }
    }

    public void runSqls(Player p, String sql)
    {
        if(!sql.contains("LIMIT") && !sql.contains("limit") && (sql.contains("SELECT") || sql.contains("select")))
        {
            sql += " LIMIT 10";
        }

        try
        {
            Statement st = conns.createStatement();
            ResultSet rs = st.executeQuery(sql);
            String res = "\n";


            for(int i = 0; i < rs.getMetaData().getColumnCount(); i++)
            {
                res += rs.getMetaData().getColumnName(i+1)+" ";
            }

            res += "\n\n";

            while(rs.next())
            {
                String row = "";

                for(int i = 0; i < rs.getMetaData().getColumnCount(); i++)
                {
                    row += rs.getString(i+1)+" ";
                }

                res += row+"\n";
            }

            st.close();
            p.sendMessage(res);
        }
        catch (SQLException e)
        {
            p.sendMessage(e.getMessage());
        }
    }
}
