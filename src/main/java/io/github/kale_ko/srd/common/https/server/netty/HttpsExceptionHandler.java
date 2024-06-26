package io.github.kale_ko.srd.common.https.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.NotSslRecordException;
import javax.net.ssl.SSLHandshakeException;
import org.jetbrains.annotations.NotNull;

public class HttpsExceptionHandler extends ChannelInboundHandlerAdapter {
    protected final @NotNull HttpServer parent;

    public HttpsExceptionHandler(@NotNull HttpServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpServer getParent() {
        return this.parent;
    }

    @Override
    public void exceptionCaught(@NotNull ChannelHandlerContext ctx, @NotNull Throwable exception) {
        if (exception instanceof DecoderException && exception.getCause() != null && (exception.getCause() instanceof SSLHandshakeException || exception.getCause() instanceof NotSslRecordException)) {
            ctx.close();
        } else {
            ctx.fireExceptionCaught(exception);
        }
    }
}