package io.github.kale_ko.srd.common.http.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.github.kale_ko.srd.common.https.server.netty.HttpOverHttpsHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpStatusClass;
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
        if (msg instanceof HttpRequest request) {
            parent.getLogger().trace("[{}] Read message #{} from {}: {}", parent.getName(), request.headers().get("X-Request-Id"), request.headers().get("X-Request-Address"), request);

            parent.getLogger().info("[{}] [{} #{}] {} {} {}", parent.getName(), request.headers().get("X-Request-Ip"), request.headers().get("X-Request-Id").split("-")[0], request.protocolVersion().text(), request.method().name(), request.uri());
        } else if (msg instanceof HttpOverHttpsHandler.Message) {
            parent.getLogger().trace("[{}] Read HttpOverHttps message", parent.getName());
        } else if (!(msg instanceof HttpContent)) {
            parent.getLogger().warn("[{}] Unknown type passed to {}, {}!", parent.getName(), this.getClass().getSimpleName(), msg.getClass().getSimpleName());
        }

        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof HttpResponse response) {
            parent.getLogger().trace("[{}] Wrote message #{} to {}: {}", parent.getName(), response.headers().get("X-Request-Id"), response.headers().get("X-Request-Address"), response);

            if (response.status().codeClass() == HttpStatusClass.SERVER_ERROR) {
                parent.getLogger().warn("[{}] [{} #{}] {} {} {}", parent.getName(), response.headers().get("X-Request-Ip"), response.headers().get("X-Request-Id").split("-")[0], response.protocolVersion().text(), response.status().code(), response.status().reasonPhrase());
            } else {
                parent.getLogger().info("[{}] [{} #{}] {} {} {}", parent.getName(), response.headers().get("X-Request-Ip"), response.headers().get("X-Request-Id").split("-")[0], response.protocolVersion().text(), response.status().code(), response.status().reasonPhrase());
            }
        } else if (!(msg instanceof HttpContent)) {
            parent.getLogger().warn("[{}] Unknown type passed to {}, {}!", parent.getName(), this.getClass().getSimpleName(), msg.getClass().getSimpleName());
        }

        ctx.write(msg, promise);
    }
}