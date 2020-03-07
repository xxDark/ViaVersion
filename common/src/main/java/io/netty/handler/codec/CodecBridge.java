package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class CodecBridge {
	public void decode(ByteToMessageDecoder decoder, ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		decoder.decode(ctx, in, out);
	}

	public <I> void decode(MessageToMessageDecoder<I> decoder, ChannelHandlerContext ctx, I in, List<Object> out) throws Exception {
		decoder.decode(ctx, in, out);
	}

	public <I> void encode(MessageToByteEncoder<I> encoder, ChannelHandlerContext ctx, I in, ByteBuf out) throws Exception {
		encoder.encode(ctx, in, out);
	}
}
