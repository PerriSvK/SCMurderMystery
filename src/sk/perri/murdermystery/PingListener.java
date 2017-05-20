package sk.perri.murdermystery;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

/**
 * Created by Miroslav. This code is protected by author's copyrights.
 */
public class PingListener implements Listener{

    @EventHandler
    public void onPing(ServerListPingEvent e){
        e.setMotd(Main.get().getHra().getState().getMotd());
    }
}
