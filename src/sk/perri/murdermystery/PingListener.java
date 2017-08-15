package sk.perri.murdermystery;

import me.mirek.devtools.serverstatus.ServerStatusUpdateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

/**
 * Created by Miroslav. This code is protected by author's copyrights.
 */
public class PingListener implements Listener
{

    @EventHandler
    public void onPing(ServerListPingEvent e)
    {
        //e.setMotd(Main.get().getHra().getState().getMotd());
        e.setMotd("§a§lLobby");
        /*e.setMap(Main.get().getMap().getName());
        e.setPlayers(Main.get().getServer().getOnlinePlayers().size());
        e.setMaxPlayers(Main.get().getServer().getMaxPlayers());
        e.setInfo(1, "§a§lNove mapy!");*/
    }
}
