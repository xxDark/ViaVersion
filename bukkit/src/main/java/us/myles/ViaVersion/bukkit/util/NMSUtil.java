package us.myles.ViaVersion.bukkit.util;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.Bukkit;

@UtilityClass
public class NMSUtil {
    private final String BASE;
    private final  String NMS;

    public Class<?> nms(String className) throws ClassNotFoundException {
        return Class.forName(NMS + "." + className);
    }

    public Class<?> obc(String className) throws ClassNotFoundException {
        return Class.forName(BASE + "." + className);
    }

    public String getVersion() {
        return BASE.substring(BASE.lastIndexOf('.') + 1);
    }

    public int getVersionInt() {
        return Integer.parseInt(getVersion().split("_")[1], 10);
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
