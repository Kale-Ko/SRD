package io.github.kale_ko.srd.common.http.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import org.jetbrains.annotations.NotNull;

public class HttpResponseMetaHandler extends ChannelOutboundHandlerAdapter {
    protected final @NotNull HttpServer parent;

    public HttpResponseMetaHandler(@NotNull HttpServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpServer getParent() {
        return this.parent;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof HttpResponse response) {
            response.headers().remove("X-Request-Id");
            response.headers().remove("X-Request-Address");
            response.headers().remove("X-Request-Ip");
        } else if (!(msg instanceof HttpContent)) {
            parent.getLogger().warn("[{}] Unknown type passed to {}, {}!", parent.getName(), this.getClass().getSimpleName(), msg.getClass().getSimpleName());
        }

        ctx.write(msg, promise);
    }
}