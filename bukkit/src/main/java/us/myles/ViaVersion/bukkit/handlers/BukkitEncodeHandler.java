package us.myles.ViaVersion.bukkit.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.bukkit.util.NMSUtil;
import us.myles.ViaVersion.exception.CancelException;
import us.myles.ViaVersion.handlers.ChannelHandlerContextWrapper;
import us.myles.ViaVersion.handlers.ViaHandler;
import us.myles.ViaVersion.packets.Direction;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;
import us.myles.ViaVersion.util.PipelineUtil;

import java.lang.reflect.Field;

public class BukkitEncodeHandler extends MessageToByteEncoder implements ViaHandler {
    private static Field versionField = null;

    static {
        try {
            versionField = NMSUtil.nms("PacketEncoder").getDeclaredField("version");
            versionField.setAccessible(true);
        } catch (Exception e) {
            // Not compat version
        }
    }

    private final UserConnection info;
    private final MessageToByteEncoder minecraftEncoder;

    public BukkitEncodeHandler(UserConnection info, MessageToByteEncoder minecraftEncoder) {
        this.info = info;
        this.minecraftEncoder = minecraftEncoder;
    }


    @Override
    protected void encode(final ChannelHandlerContext ctx, Object o, final ByteBuf bytebuf) throws Exception {
        if (versionField != null) {
            versionField.set(minecraftEncoder, versionField.get(this));
        }
        // handle the packet type
        if (!(o instanceof ByteBuf)) {
            // call minecraft encoder
            PipelineUtil.callEncode(this.minecraftEncoder, new ChannelHandlerContextWrapper(ctx, this), o, bytebuf);
        }

        transform(bytebuf);
    }

    public void transform(ByteBuf bytebuf) throws Exception {
        if (bytebuf.readableBytes() == 0) {
            return; // Someone Already Decoded It!
        }
        // Increment sent
        info.incrementSent();
        if (info.isActive()) {
            // Handle ID
            int id = Type.VAR_INT.read(bytebuf);
            // Transform
            ByteBuf oldPacket = bytebuf.copy();
            bytebuf.clear();

            try {
                PacketWrapper wrapper = new PacketWrapper(id, oldPacket, info);
                ProtocolInfo protInfo = info.get(ProtocolInfo.class);
                protInfo.getPipeline().transform(Direction.OUTGOING, protInfo.getState(), wrapper);
                wrapper.writeToBuffer(bytebuf);
            } catch (Exception e) {
                bytebuf.clear();
                throw e;
            } finally {
                oldPacket.release();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PipelineUtil.containsCause(cause, CancelException.class)) return;
        super.exceptionCaught(ctx, cause);
    }
}
