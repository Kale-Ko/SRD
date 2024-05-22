package io.github.kale_ko.srd.common.http.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.charset.StandardCharsets;
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
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
        if (msg instanceof FullHttpRequest request) {
            try {
                FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
                response.retain();

                try {
                    response.headers().set("X-Request-Id", request.headers().get("X-Request-Id"));
                    response.headers().set("X-Request-Address", request.headers().get("X-Request-Address"));
                    response.headers().set("X-Request-Ip", request.headers().get("X-Request-Ip"));

                    if (parent.getListener() != null) {
                        HttpResponseStatus preStatus;
                        try {
                            preStatus = parent.getListener().onPreRequest(parent, request);
                        } catch (Exception e) {
                            parent.getLogger().error("Unhandled exception occurred in HttpServerListener#onPreRequest", e);

                            preStatus = null;
                        }

                        if (preStatus == null) {
                            try {
                                parent.getListener().onRequest(parent, request, response);
                            } catch (Exception e) {
                                parent.getLogger().error("Unhandled exception occurred in HttpServerListener#onRequest", e);

                                response.release();
                                response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
                                response.retain();

                                byte[] statusContent = String.format("<b>%s %s</b>", HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()).getBytes(StandardCharsets.UTF_8);
                                response.headers().set("Content-Length", statusContent.length);
                                response.headers().set("Content-Type", "text/html");
                                response.content().clear();
                                response.content().writeBytes(statusContent);
                            }
                        } else {
                            response.setStatus(preStatus);

                            byte[] statusContent = String.format("<b>%s %s</b>", preStatus.code(), preStatus.reasonPhrase()).getBytes(StandardCharsets.UTF_8);
                            response.headers().set("Content-Length", statusContent.length);
                            response.headers().set("Content-Type", "text/html");
                            response.content().clear();
                            response.content().writeBytes(statusContent);
                        }
                    } else {
                        byte[] statusContent = String.format("<b>%s %s</b>", HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()).getBytes(StandardCharsets.UTF_8);
                        response.headers().set("Content-Length", statusContent.length);
                        response.headers().set("Content-Type", "text/html");
                        response.content().clear();
                        response.content().writeBytes(statusContent);
                    }

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
    public void channelReadComplete(@NotNull ChannelHandlerContext ctx) {
        ctx.flush();
    }
}