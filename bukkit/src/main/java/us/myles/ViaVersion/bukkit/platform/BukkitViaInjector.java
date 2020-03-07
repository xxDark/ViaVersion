package us.myles.ViaVersion.bukkit.platform;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.bukkit.plugin.PluginDescriptionFile;
import us.myles.ViaVersion.api.Pair;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.platform.ViaInjector;
import us.myles.ViaVersion.bukkit.handlers.BukkitChannelInitializer;
import us.myles.ViaVersion.bukkit.util.NMSUtil;
import us.myles.ViaVersion.util.SynhronizedArrayList;
import us.myles.ViaVersion.util.ListWrapper;
import us.myles.ViaVersion.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BukkitViaInjector implements ViaInjector {
    private final List<ChannelFuture> injectedFutures = Collections.synchronizedList(new ArrayList<ChannelFuture>());
    private final List<Pair<Field, Object>> injectedLists = Collections.synchronizedList(new ArrayList<Pair<Field, Object>>());

    @Override
    public void inject() throws Exception {
        try {
            Object connection = getServerConnection();
            if (connection == null) {
                throw new Exception("We failed to find the core component 'ServerConnection', please file an issue on our GitHub.");
            }
            for (Field field : connection.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                final Object value = field.get(connection);
                if (value instanceof List) {
                    // Inject the list
                    List wrapper = new ListWrapper((List) value) {
                        @Override
                        public void handleAdd(Object o) {
                            if (o instanceof ChannelFuture) {
                                try {
                                    injectChannelFuture((ChannelFuture) o);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    injectedLists.add(new Pair<>(field, connection));
                    field.set(connection, wrapper);
                    // Iterate through current list
                    synchronized (wrapper) {
                        for (Object o : (List) value) {
                            if (o instanceof ChannelFuture) {
                                injectChannelFuture((ChannelFuture) o);
                            } else {
                                break; // not the right list.
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            Via.getPlatform().getLogger().severe("Unable to inject ViaVersion, please post these details on our GitHub and ensure you're using a compatible server version.");
            throw e;
        }
    }

    private void injectChannelFuture(ChannelFuture future) throws Exception {
        try {
            List<String> names = future.channel().pipeline().names();
            ChannelHandler bootstrapAcceptor = null;
            // Pick best
            for (String name : names) {
                ChannelHandler handler = future.channel().pipeline().get(name);
                try {
                    ReflectionUtil.get(handler, "childHandler", ChannelInitializer.class);
                    bootstrapAcceptor = handler;
                } catch (Exception e) {
                    // Not this one
                }
            }
            // Default to first (Also allows blame to work)
            if (bootstrapAcceptor == null) {
                bootstrapAcceptor = future.channel().pipeline().first();
            }
            try {
                ChannelInitializer<SocketChannel> oldInit = ReflectionUtil.get(bootstrapAcceptor, "childHandler", ChannelInitializer.class);
                ChannelInitializer newInit = new BukkitChannelInitializer(oldInit);

                ReflectionUtil.set(bootstrapAcceptor, "childHandler", newInit);
                injectedFutures.add(future);
            } catch (NoSuchFieldException e) {
                // let's find who to blame!
                ClassLoader cl = bootstrapAcceptor.getClass().getClassLoader();
                if (cl.getClass().getName().equals("org.bukkit.plugin.java.PluginClassLoader")) {
                    PluginDescriptionFile yaml = ReflectionUtil.get(cl, "description", PluginDescriptionFile.class);
                    throw new Exception("Unable to inject, due to " + bootstrapAcceptor.getClass().getName() + ", try without the plugin " + yaml.getName() + "?");
                } else {
                    throw new Exception("Unable to find core component 'childHandler', please check your plugins. issue: " + bootstrapAcceptor.getClass().getName());
                }

            }
        } catch (Exception e) {
            Via.getPlatform().getLogger().severe("We failed to inject ViaVersion, have you got late-bind enabled with something else?");
            throw e;
        }
    }

    @Override
    public void uninject() {
        // TODO: Uninject from players currently online to prevent protocol lib issues.
        for (ChannelFuture future : injectedFutures) {
            List<String> names = future.channel().pipeline().names();
            ChannelHandler bootstrapAcceptor = null;
            // Pick best
            for (String name : names) {
                ChannelHandler handler = future.channel().pipeline().get(name);
                try {
                    ChannelInitializer<SocketChannel> oldInit = ReflectionUtil.get(handler, "childHandler", ChannelInitializer.class);
                    if (oldInit instanceof BukkitChannelInitializer) {
                        bootstrapAcceptor = handler;
                    }
                } catch (Exception e) {
                    // Not this one
                }
            }
            // Default to first
            if (bootstrapAcceptor == null) {
                bootstrapAcceptor = future.channel().pipeline().first();
            }

            try {
                ChannelInitializer<SocketChannel> oldInit = ReflectionUtil.get(bootstrapAcceptor, "childHandler", ChannelInitializer.class);
                if (oldInit instanceof BukkitChannelInitializer) {
                    ReflectionUtil.set(bootstrapAcceptor, "childHandler", ((BukkitChannelInitializer) oldInit).getOriginal());
                }
            } catch (Exception e) {
                Via.getPlatform().getLogger().severe("Failed to remove injection handler, reload won't work with connections, please reboot!");
            }
        }
        injectedFutures.clear();

        for (Pair<Field, Object> pair : injectedLists) {
            try {
                Object o = pair.getKey().get(pair.getValue());
                if (o instanceof ListWrapper) {
                    pair.getKey().set(pair.getValue(), ((ListWrapper) o).getOriginalList());
                }
            } catch (IllegalAccessException e) {
                Via.getPlatform().getLogger().severe("Failed to remove injection, reload won't work with connections, please reboot!");
            }
        }

        injectedLists.clear();
    }

    @Override
    public int getServerProtocolVersion() throws Exception {
        try {
            Class<?> serverClazz = NMSUtil.nms("MinecraftServer");
            Object server = ReflectionUtil.invokeStatic(serverClazz, "getServer");
            Class<?> pingClazz = NMSUtil.nms("ServerPing");
            Object ping = null;
            // Search for ping method
            for (Field f : serverClazz.getDeclaredFields()) {
                if (f.getType() != null) {
                    if (f.getType().getSimpleName().equals("ServerPing")) {
                        f.setAccessible(true);
                        ping = f.get(server);
                    }
                }
            }
            if (ping != null) {
                Object serverData = null;
                for (Field f : pingClazz.getDeclaredFields()) {
                    if (f.getType() != null) {
                        if (f.getType().getSimpleName().endsWith("ServerData")) {
                            f.setAccessible(true);
                            serverData = f.get(ping);
                        }
                    }
                }
                if (serverData != null) {
                    int protocolVersion = -1;
                    for (Field f : serverData.getClass().getDeclaredFields()) {
                        if (f.getType() != null) {
                            if (f.getType() == int.class) {
                                f.setAccessible(true);
                                protocolVersion = (int) f.get(serverData);
                            }
                        }
                    }
                    if (protocolVersion != -1) {
                        return protocolVersion;
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception("Failed to get server", e);
        }
        throw new Exception("Failed to get server");
    }

    @Override
    public String getEncoderName() {
        return "encoder";
    }

    @Override
    public String getDecoderName() {
        return "decoder";
    }

    public static Object getServerConnection() throws Exception {
        Class<?> serverClazz = NMSUtil.nms("MinecraftServer");
        Object server = ReflectionUtil.invokeStatic(serverClazz, "getServer");
        Object connection = null;
        for (Method m : serverClazz.getDeclaredMethods()) {
            if (m.getReturnType() != null) {
                if (m.getReturnType().getSimpleName().equals("ServerConnection")) {
                    if (m.getParameterTypes().length == 0) {
                        connection = m.invoke(server);
                    }
                }
            }
        }
        return connection;
    }

    public static void patchLists() throws Exception {
        Object connection = getServerConnection();
        if (connection == null) {
            Via.getPlatform().getLogger().warning("We failed to find the core component 'ServerConnection', please file an issue on our GitHub.");
            return;
        }
        for (Field field : connection.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            final Object value = field.get(connection);
            if (value instanceof List) {
                if (!(value instanceof SynhronizedArrayList)) {
                    SynhronizedArrayList list = new SynhronizedArrayList((List)value);
                    field.set(connection, list);
                }
            }
        }
    }

    public static boolean isBound() {
        try {
            Object connection = getServerConnection();
            if (connection == null) {
                return false;
            }
            for (Field field : connection.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                final Object value = field.get(connection);
                if (value instanceof List) {
                    // Inject the list
                    synchronized (value) {
                        for (Object o : (List) value) {
                            if (o instanceof ChannelFuture) {
                                return true;
                            } else {
                                break; // not the right list.
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public JsonObject getDump() {
        JsonObject data = new JsonObject();

        // Generate information about current injections
        JsonArray injectedChannelInitializers = new JsonArray();
        for (ChannelFuture cf : injectedFutures) {
            JsonObject info = new JsonObject();
            info.addProperty("futureClass", cf.getClass().getName());
            info.addProperty("channelClass", cf.channel().getClass().getName());

            // Get information about the pipes for this channel future
            JsonArray pipeline = new JsonArray();
            for (String pipeName : cf.channel().pipeline().names()) {
                JsonObject pipe = new JsonObject();
                pipe.addProperty("name", pipeName);
                if (cf.channel().pipeline().get(pipeName) != null) {
                    pipe.addProperty("class", cf.channel().pipeline().get(pipeName).getClass().getName());
                    try {
                        Object child = ReflectionUtil.get(cf.channel().pipeline().get(pipeName), "childHandler", ChannelInitializer.class);
                        pipe.addProperty("childClass", child.getClass().getName());
                        if (child instanceof BukkitChannelInitializer) {
                            pipe.addProperty("oldInit", ((BukkitChannelInitializer) child).getOriginal().getClass().getName());
                        }
                    } catch (Exception e) {
                        // Don't display
                    }
                }
                // Add to the pipeline array
                pipeline.add(pipe);
            }
            info.add("pipeline", pipeline);

            // Add to the list
            injectedChannelInitializers.add(info);
        }
        data.add("injectedChannelInitializers", injectedChannelInitializers);

        // Generate information about lists we've injected into
        JsonObject wrappedLists = new JsonObject();
        JsonObject currentLists = new JsonObject();
        try {
            for (Pair<Field, Object> pair : injectedLists) {
                Object list = pair.getKey().get(pair.getValue());
                // Note down the current value (could be overridden by another plugin)
                currentLists.addProperty(pair.getKey().getName(), list.getClass().getName());
                // Also if it's not overridden we can display what's inside our list (possibly another plugin)
                if (list instanceof ListWrapper) {
                    wrappedLists.addProperty(pair.getKey().getName(), ((ListWrapper) list).getOriginalList().getClass().getName());
                }
            }
            data.add("wrappedLists", wrappedLists);
            data.add("currentLists", currentLists);
        } catch (Exception e) {
            // Ignored, fields won't be present
        }

        data.addProperty("binded", isBound());
        return data;
    }
}
