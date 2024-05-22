package io.github.kale_ko.srd.common.http.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.SocketException;
import org.jetbrains.annotations.NotNull;

public class HttpExceptionHandler extends ChannelInboundHandlerAdapter {
    protected final @NotNull HttpServer parent;

    public HttpExceptionHandler(@NotNull HttpServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpServer getParent() {
        return this.parent;
    }

    @Override
    public void exceptionCaught(@NotNull ChannelHandlerContext ctx, @NotNull Throwable exception) {
        if (exception instanceof SocketException && exception.getMessage().equalsIgnoreCase("Connection reset")) {
            ctx.close();
        } else {
            parent.getLogger().throwing(exception);

            ctx.close();
        }
    }
}