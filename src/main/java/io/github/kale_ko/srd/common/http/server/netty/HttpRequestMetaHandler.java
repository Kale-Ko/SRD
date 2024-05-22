package io.github.kale_ko.srd.common.http.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.github.kale_ko.srd.common.https.server.netty.HttpOverHttpsHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class HttpRequestMetaHandler extends ChannelInboundHandlerAdapter {
    protected final @NotNull HttpServer parent;

    public HttpRequestMetaHandler(@NotNull HttpServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpServer getParent() {
        return this.parent;
    }

    protected final @NotNull SecureRandom random = new SecureRandom();

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
        if (msg instanceof HttpRequest request) {
            UUID uuid = new UUID(random.nextLong(), random.nextLong());

            request.headers().set("X-Request-Id", uuid);

            if (ctx.channel().remoteAddress() instanceof InetSocketAddress address) {
                request.headers().set("X-Request-Address", address.getAddress().getHostAddress() + ":" + address.getPort());
                request.headers().set("X-Request-Ip", address.getAddress().getHostAddress());
            } else {
                parent.getLogger().error("[{}] Unknown socket address type, {}!", parent.getName(), ctx.channel().remoteAddress().getClass().getSimpleName());

                request.headers().set("X-Request-Address", ctx.channel().remoteAddress().toString());
            }
        } else if (!(msg instanceof HttpContent || msg instanceof HttpOverHttpsHandler.HttpOverHttpsMessage)) {
            parent.getLogger().warn("[{}] Unknown type passed to {}, {}!", parent.getName(), this.getClass().getSimpleName(), msg.getClass().getSimpleName());
        }

        ctx.fireChannelRead(msg);
    }
}