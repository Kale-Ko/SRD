package io.github.kale_ko.srd.common.http.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import org.jetbrains.annotations.NotNull;

public class WsChannel implements AttributeMap {
    protected final @NotNull Channel channel;

    public WsChannel(@NotNull Channel channel) {
        this.channel = channel;
    }

    public void send(@NotNull WebSocketFrame frame) {
        channel.writeAndFlush(frame).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return channel.hasAttr(key);
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return channel.attr(key);
    }
}