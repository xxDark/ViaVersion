package us.myles.ViaVersion.bukkit.util;

import lombok.val;
import org.bukkit.Bukkit;

public class NMSUtil {
    private static final String BASE;
    private static  final String NMS;

    public static Class<?> nms(String className) throws ClassNotFoundException {
        return Class.forName(NMS + "." + className);
    }

    public static Class<?> obc(String className) throws ClassNotFoundException {
        return Class.forName(BASE + "." + className);
    }

    public static String getVersion() {
        return BASE.substring(BASE.lastIndexOf('.') + 1);
    }

    static {
        val serverClass = Bukkit.getServer().getClass();
        val packet = serverClass.getPackage();
        String packageName;
        if (packet != null) {
            packageName = packet.getName();
        } else {
            val cn = serverClass.getName();
            val dot = cn.lastIndexOf('.');
            packageName = cn.substring(0, dot);
        }
        BASE = packageName;
        NMS = packageName.replace("org.bukkit.craftbukkit", "net.minecraft.server");
    }
}
