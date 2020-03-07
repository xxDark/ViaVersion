package us.myles.ViaVersion.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CodecUtil;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.internal.RecyclableArrayList;
import lombok.experimental.UtilityClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PipelineUtil {

    /**
     * Call the decode method on a netty ByteToMessageDecoder
     *
     * @param decoder The decoder
     * @param ctx     The current context
     * @param input   The packet to decode
     * @return A list of the decoders output
     * @throws InvocationTargetException If an exception happens while executing
     */
    public static RecyclableArrayList callDecode(ByteToMessageDecoder decoder, ChannelHandlerContext ctx, ByteBuf input) throws Exception {
        RecyclableArrayList output = RecyclableArrayList.newInstance(1);
        CodecUtil.decode(decoder, ctx, input, output);
        return output;
    }

    /**
     * Call the encode method on a netty MessageToByteEncoder
     *
     * @param encoder The encoder
     * @param ctx     The current context
     * @param msg     The packet to encode
     * @param output  The bytebuf to write the output to
     * @throws InvocationTargetException If an exception happens while executing
     */
    public void callEncode(MessageToByteEncoder encoder, ChannelHandlerContext ctx, Object msg, ByteBuf output) throws Exception {
        CodecUtil.encode(encoder, ctx, msg, output);
    }

    public RecyclableArrayList callDecode(MessageToMessageDecoder decoder, ChannelHandlerContext ctx, Object msg) throws Exception {
        RecyclableArrayList output = RecyclableArrayList.newInstance(1);
        CodecUtil.decode(decoder,ctx, msg, output);
        return output;
    }

    /**
     * Check if a stack trace contains a certain exception
     *
     * @param t The throwable
     * @param c The exception to look for
     * @return True if the stack trace contained it as its cause or if t is an instance of c.
     */
    public static boolean containsCause(Throwable t, Class<? extends Throwable> c) {
        do {
            if (t != null) {
                if (c.isAssignableFrom(t.getClass())) return true;
                t = t.getCause();
            }
        } while (t != null);
        return false;
    }

    /**
     * Get the context for a the channel handler before a certain name.
     *
     * @param name     The name of the channel handler
     * @param pipeline The pipeline to target
     * @return The ChannelHandler before the one requested.
     */
    public ChannelHandlerContext getContextBefore(String name, ChannelPipeline pipeline) {
        boolean mark = false;
        for (String s : pipeline.names()) {
            if (mark) {
                return pipeline.context(pipeline.get(s));
            }
            if (s.equalsIgnoreCase(name))
                mark = true;
        }
        return null;
    }

    public ChannelHandlerContext getPreviousContext(String name, ChannelPipeline pipeline) {
        String previous = null;
        for (String entry : pipeline.toMap().keySet()) {
            if (entry.equals(name)) {
                return pipeline.context(previous);
            }
            previous = entry;
        }
        return null;
    }
}
