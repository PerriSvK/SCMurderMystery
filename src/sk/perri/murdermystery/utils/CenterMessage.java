package sk.perri.murdermystery.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class CenterMessage
{
    public static void sendCenteredMessage(List<Player> players, String message){
        for(Player player : players){
            sendCenteredMessage(player, message);
        }
    }
    public static void sendCenteredMessage(Player player, String message){
        if(message == null || message.equals("")) player.sendMessage("");
        //message = color(message);
        int CENTER_PX = 154;
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for(char c : message.toCharArray())
        {
            if(c == 'ง')
            {
                previousCode = true;
                continue;
            }
            else if(previousCode)
            {
                previousCode = false;
                if(c == 'l' || c == 'L')
                {
                    isBold = true;
                }
                else
                    isBold = false;
            }
            else
            {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while(compensated < toCompensate){
            sb.append(" ");
            compensated += spaceLength;
        }
        player.sendMessage(sb.toString() + message);
    }
}
