package io.github.kale_ko.srd.common.https.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.jetbrains.annotations.NotNull;

public class HttpOverHttpsHandler extends ChannelInboundHandlerAdapter {
    protected final @NotNull HttpServer parent;

    public HttpOverHttpsHandler(@NotNull HttpServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpServer getParent() {
        return this.parent;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
        if (msg instanceof ByteBuf buffer) {
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < buffer.readableBytes(); i++) {
                int read = buffer.getByte(i);

                if (read == '\r' || read == '\n') {
                    break;
                }

                if (Character.isLetterOrDigit(read) || Character.isWhitespace(read) || "-._~:/?#[]@!$&'()*+,;=".indexOf(read) != -1) {
                    stringBuilder.appendCodePoint(read);
                } else {
                    break;
                }
            }

            String string = stringBuilder.toString();
            if (string.contains("HTTP")) {
                DefaultFullHttpRequest response = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/bad-request", Unpooled.buffer(0));
                response.setDecoderResult(DecoderResult.failure(new RuntimeException("HTTP over HTTPS")));

                ctx.pipeline().remove("SslHandler");

                ctx.fireChannelRead(response);

                buffer.release();
            } else {
                ctx.fireChannelRead(buffer.copy());

                buffer.release();
            }
        } else {
            parent.getLogger().warn("[{}] Unknown type passed to {}, {}!", parent.getName(), this.getClass().getSimpleName(), msg.getClass().getSimpleName());

            ctx.fireChannelRead(msg);
        }
    }
}