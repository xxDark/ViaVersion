package io.netty.channel;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ChannelBridge {
	public <C extends Channel> void initChannel(C ch, ChannelInitializer<C> initializer) throws Exception {
		initializer.initChannel(ch);
	}
}
