package sk.perri.murdermystery.enums;

public enum GameState
{
    Setup("§c§lSETUP"), Lobby("§a§lLobby"), Starting("§a§lLobby"), Start("§c§lVe hre"), Ingame("§c§lVe hre"), End("§c§lVe hre");

    GameState(String motd){
        this.motd = motd;
    }

    String motd;

    public String getMotd(){
        return motd;
    }
}
