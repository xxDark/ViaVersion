package us.myles.ViaVersion.sponge.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.RecyclableArrayList;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.exception.CancelException;
import us.myles.ViaVersion.packets.Direction;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;
import us.myles.ViaVersion.util.PipelineUtil;

import java.util.List;

public class SpongeDecodeHandler extends ByteToMessageDecoder {

    private final ByteToMessageDecoder minecraftDecoder;
    private final UserConnection info;

    public SpongeDecodeHandler(UserConnection info, ByteToMessageDecoder minecraftDecoder) {
        this.info = info;
        this.minecraftDecoder = minecraftDecoder;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> list) throws Exception {
        // use transformers
        if (bytebuf.isReadable()) {
            // Ignore if pending disconnect
            if (info.isPendingDisconnect()) {
                return;
            }
            // Increment received
            boolean second = info.incrementReceived();
            // Check PPS
            if (second) {
                if (info.handlePPS())
                    return;
            }

            if (info.isActive()) {
                // Handle ID
                int id = Type.VAR_INT.read(bytebuf);
                // Transform
                ByteBuf newPacket = ctx.alloc().buffer();
                try {
                    if (id == PacketWrapper.PASSTHROUGH_ID) {
                        newPacket.writeBytes(bytebuf);
                    } else {
                        PacketWrapper wrapper = new PacketWrapper(id, bytebuf, info);
                        ProtocolInfo protInfo = info.get(ProtocolInfo.class);
                        protInfo.getPipeline().transform(Direction.INCOMING, protInfo.getState(), wrapper);
                        wrapper.writeToBuffer(newPacket);
                    }

                    bytebuf.clear();
                    bytebuf = newPacket;
                } catch (Throwable e) {
                    // Clear Buffer
                    bytebuf.clear();
                    // Release Packet, be free!
                    newPacket.release();
                    throw e;
                }
            }

            // call minecraft decoder
            try {
                RecyclableArrayList decoded = PipelineUtil.callDecode(this.minecraftDecoder, ctx, bytebuf);
                list.addAll(decoded);
                decoded.recycle();
            } finally {
                if (info.isActive()) {
                    bytebuf.release();
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PipelineUtil.containsCause(cause, CancelException.class)) return;
        super.exceptionCaught(ctx, cause);
    }
}
