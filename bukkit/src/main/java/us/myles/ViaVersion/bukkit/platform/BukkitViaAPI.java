package us.myles.ViaVersion.bukkit.platform;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.myles.ViaVersion.ViaVersionPlugin;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;
import us.myles.ViaVersion.api.ViaVersionAPI;
import us.myles.ViaVersion.api.boss.BossBar;
import us.myles.ViaVersion.api.boss.BossColor;
import us.myles.ViaVersion.api.boss.BossStyle;
import us.myles.ViaVersion.api.command.ViaVersionCommand;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.boss.ViaBossBar;
import us.myles.ViaVersion.bukkit.util.ProtocolSupportUtil;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

@AllArgsConstructor
public class BukkitViaAPI implements ViaAPI<Player>, ViaVersionAPI {
    private ViaVersionPlugin plugin;

    @Override
    public int getPlayerVersion(@NonNull Player player) {
        if (!isPorted(player))
            return getExternalVersion(player);
        return getPortedPlayers().get(player.getUniqueId()).get(ProtocolInfo.class).getProtocolVersion();
    }

    @Override
    public int getPlayerVersion(@NonNull UUID uuid) {
        if (!isPorted(uuid))
            return getExternalVersion(Bukkit.getPlayer(uuid));
        return getPortedPlayers().get(uuid).get(ProtocolInfo.class).getProtocolVersion();
    }

    private int getExternalVersion(Player player) {
        if (!isProtocolSupport()) {
            return ProtocolRegistry.SERVER_PROTOCOL;
        } else {
            return ProtocolSupportUtil.getProtocolVersion(player);
        }
    }

    @Override
    public boolean isPorted(Player player) {
        return isPorted(player.getUniqueId());
    }

    @Override
    public boolean isPorted(UUID playerUUID) {
        return getPortedPlayers().containsKey(playerUUID);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public void sendRawPacket(UUID uuid, ByteBuf packet) throws IllegalArgumentException {
        if (!isPorted(uuid)) throw new IllegalArgumentException("This player is not controlled by ViaVersion!");
        UserConnection ci = getPortedPlayers().get(uuid);
        ci.sendRawPacket(packet);
    }

    @Override
    public void sendRawPacket(Player player, ByteBuf packet) throws IllegalArgumentException {
        sendRawPacket(player.getUniqueId(), packet);
    }

    @Override
    public BossBar createBossBar(String title, BossColor color, BossStyle style) {
        return new ViaBossBar(title, 1F, color, style);
    }

    @Override
    public BossBar createBossBar(String title, float health, BossColor color, BossStyle style) {
        return new ViaBossBar(title, health, color, style);
    }

    @Override
    public boolean isDebug() {
        return Via.getManager().isDebug();
    }

    @Override
    public ViaVersionCommand getCommandHandler() {
        return Via.getManager().getCommandHandler();
    }

    @Override
    public SortedSet<Integer> getSupportedVersions() {
        SortedSet<Integer> outputSet = new TreeSet<>(ProtocolRegistry.getSupportedVersions());
        outputSet.removeAll(Via.getPlatform().getConf().getBlockedProtocols());

        return outputSet;
    }

    @Override
    public boolean isCompatSpigotBuild() {
        return plugin.isCompatSpigotBuild();
    }

    @Override
    public boolean isSpigot() {
        return plugin.isSpigot();
    }

    @Override
    public boolean isProtocolSupport() {
        return plugin.isProtocolSupport();
    }

    public Map<UUID, UserConnection> getPortedPlayers() {
        return Via.getManager().getPortedPlayers();
    }
}
