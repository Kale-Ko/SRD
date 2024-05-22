package io.github.kale_ko.srd.common.https.server.netty;

import io.github.kale_ko.srd.common.https.server.HttpsServer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jetbrains.annotations.NotNull;

public class HttpOverHttpsHandler extends ChannelInboundHandlerAdapter {
    protected final @NotNull HttpsServer parent;

    public HttpOverHttpsHandler(@NotNull HttpsServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpsServer getParent() {
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
                ctx.fireChannelRead(new HttpOverHttpsMessage());

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

    public static class HttpOverHttpsMessage {
    }
}