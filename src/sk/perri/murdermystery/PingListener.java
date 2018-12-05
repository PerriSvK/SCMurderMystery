package sk.perri.murdermystery;

import me.mirek.devtools.serverstatus.ServerStatusUpdateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PingListener implements Listener
{
  public PingListener() {}
  
  @EventHandler
  public void onPing(ServerStatusUpdateEvent e)
  {
    e.setMotd(MainMurder.get().getHra().getState().getMotd());
    e.setMap(MainMurder.get().getMap().getName());
    e.setPlayers(MainMurder.get().getServer().getOnlinePlayers().size());
    e.setMaxPlayers(MainMurder.get().getServer().getMaxPlayers());
    e.setInfo(1, "§a§lNove mapy!");
  }
}
