package io.github.kale_ko.srd.common.http.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class HttpResponseHandler extends ChannelOutboundHandlerAdapter {
    protected final @NotNull HttpServer parent;

    public HttpResponseHandler(@NotNull HttpServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpServer getParent() {
        return this.parent;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof HttpResponse response) {
            HttpHeaders headers = response.headers().copy();
            response.headers().clear();

            Stream<String> nameStream = headers.names().stream().map((a) -> {
                String[] split = a.split("-");
                for (int i = 0; i < split.length; i++) {
                    split[i] = split[i].substring(0, 1).toUpperCase() + split[i].substring(1).toLowerCase();
                }
                return String.join("-", split);
            }).sorted((a, b) -> a.compareToIgnoreCase(b));

            for (String name : nameStream.toList()) {
                response.headers().add(name, headers.getAll(name));
            }
        } else if (!(msg instanceof HttpContent)) {
            parent.getLogger().warn("[{}] Unknown type passed to {}, {}!", parent.getName(), this.getClass().getSimpleName(), msg.getClass().getSimpleName());
        }

        ctx.write(msg, promise);
    }
}