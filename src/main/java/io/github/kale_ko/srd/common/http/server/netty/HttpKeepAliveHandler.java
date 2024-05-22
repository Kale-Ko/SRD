package io.github.kale_ko.srd.common.http.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;

public class HttpKeepAliveHandler extends ChannelDuplexHandler {
    protected final @NotNull HttpServer parent;

    public HttpKeepAliveHandler(@NotNull HttpServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpServer getParent() {
        return this.parent;
    }

    protected static final @NotNull AttributeKey<Boolean> KEEPALIVE_ATTRIBUTE = AttributeKey.newInstance("keepAlive");
    protected static final @NotNull AttributeKey<Integer> REQUEST_COUNT_ATTRIBUTE = AttributeKey.newInstance("requestCount");

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) {
        ctx.channel().attr(KEEPALIVE_ATTRIBUTE).set(true);
        ctx.channel().attr(REQUEST_COUNT_ATTRIBUTE).set(0);

        ctx.fireChannelActive();
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
        if (msg instanceof HttpRequest request) {
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (!keepAlive) {
                ctx.channel().attr(KEEPALIVE_ATTRIBUTE).set(false);
            }

            ctx.channel().attr(REQUEST_COUNT_ATTRIBUTE).set(ctx.channel().attr(REQUEST_COUNT_ATTRIBUTE).get() + 1);
        } else if (!(msg instanceof HttpContent || msg instanceof WebSocketFrame)) {
            parent.getLogger().warn("[{}] Unknown type passed to {}, {}!", parent.getName(), this.getClass().getSimpleName(), msg.getClass().getSimpleName());
        }

        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(@NotNull ChannelHandlerContext ctx, @NotNull Object msg, @NotNull ChannelPromise promise) {
        if (msg instanceof HttpResponse response) {
            if (response.status().codeClass() == HttpStatusClass.INFORMATIONAL) {
                ctx.write(msg, promise);
                return;
            } else {
                ctx.channel().attr(REQUEST_COUNT_ATTRIBUTE).set(ctx.channel().attr(REQUEST_COUNT_ATTRIBUTE).get() - 1);
            }

            if (!response.headers().contains("Connection")) {
                boolean keepAlive = ctx.channel().attr(KEEPALIVE_ATTRIBUTE).get();

                response.headers().set("Connection", keepAlive ? "keep-alive" : "close");
            }
        } else if (msg instanceof LastHttpContent) {
            boolean keepAlive = ctx.channel().attr(KEEPALIVE_ATTRIBUTE).get();

            if (!keepAlive) {
                promise = promise.unvoid().addListener(ChannelFutureListener.CLOSE);
            }
        } else if (!(msg instanceof HttpContent || msg instanceof WebSocketFrame)) {
            parent.getLogger().warn("[{}] Unknown type passed to {}, {}!", parent.getName(), this.getClass().getSimpleName(), msg.getClass().getSimpleName());
        }

        ctx.write(msg, promise);
    }
}