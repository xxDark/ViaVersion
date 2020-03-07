package us.myles.ViaVersion.bukkit.util;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import protocolsupport.api.ProtocolSupportAPI;

@UtilityClass
public class ProtocolSupportUtil {
    public int getProtocolVersion(Player player) {
        return ProtocolSupportAPI.getProtocolVersion(player).getId();
    }
}
