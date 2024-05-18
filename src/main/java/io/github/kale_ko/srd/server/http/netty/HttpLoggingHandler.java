package io.github.kale_ko.srd.server.http.netty;

import io.github.kale_ko.srd.server.http.HttpServer;
import io.github.kale_ko.srd.server.https.netty.HttpOverHttpsHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.jetbrains.annotations.NotNull;

public class HttpLoggingHandler extends ChannelDuplexHandler {
    protected final @NotNull HttpServer parent;

    public HttpLoggingHandler(@NotNull HttpServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpServer getParent() {
        return this.parent;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            parent.getLogger().trace("[{}] Read message: {}", parent.getName(), msg);
        } else if (msg instanceof HttpOverHttpsHandler.Message) {
            parent.getLogger().trace("[{}] Read HttpOverHttps message", parent.getName());
        } else if (!(msg instanceof HttpContent)) {
            parent.getLogger().warn("[{}] Unknown type passed, {}!", parent.getName(), msg.getClass().getSimpleName());
        }

        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof HttpResponse) {
            parent.getLogger().trace("[{}] Wrote message: {}", parent.getName(), msg);
        } else if (!(msg instanceof HttpContent)) {
            parent.getLogger().warn("[{}] Unknown type passed, {}!", parent.getName(), msg.getClass().getSimpleName());
        }

        ctx.write(msg, promise);
    }
}