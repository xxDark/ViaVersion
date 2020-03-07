package protocolsupport.api;

import org.bukkit.entity.Player;

/**
 * Facade classes for ProtocolSupport
 */
public class ProtocolSupportAPI {

	public static ProtocolVersion getProtocolVersion(Player player) {
		return ProtocolVersion.FACADE;
	}
}
