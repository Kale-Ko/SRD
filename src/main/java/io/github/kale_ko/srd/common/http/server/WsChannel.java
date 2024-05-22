package io.github.kale_ko.srd.common.http.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jetbrains.annotations.NotNull;

public class WsChannel {
    protected final @NotNull Channel channel;

    public WsChannel(@NotNull Channel channel) {
        this.channel = channel;
    }

    public void send(@NotNull WebSocketFrame frame) {
        channel.writeAndFlush(frame).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}