package io.github.kale_ko.srd.server.http.netty;

import io.github.kale_ko.srd.server.http.HttpServer;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

public class HttpRequestHandler extends ChannelInboundHandlerAdapter {
    protected final @NotNull HttpServer parent;

    public HttpRequestHandler(@NotNull HttpServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpServer getParent() {
        return this.parent;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest request) {
            try {
                FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
                response.retain();

                try {
                    response.headers().set("X-Request-Id", request.headers().get("X-Request-Id"));
                    response.headers().set("X-Request-Address", request.headers().get("X-Request-Address"));
                    response.headers().set("X-Request-Ip", request.headers().get("X-Request-Ip"));
                    response.headers().set("Content-Length", 0);

                    // TODO

                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                } finally {
                    response.release();
                }
            } finally {
                request.release();
            }
        } else {
            parent.getLogger().warn("Unknown type passed to {}, {}!", this.getClass().getSimpleName(), msg.getClass().getSimpleName());

            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
}