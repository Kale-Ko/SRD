package io.github.kale_ko.srd.common.http.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
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

                    if (request.decoderResult().isSuccess()) {
                        HttpResponseStatus preStatus;
                        try {
                            preStatus = parent.getListener().onPreRequest(parent, request);
                        } catch (Exception e) {
                            parent.getLogger().error("[{}] Unhandled exception occurred in HttpServerListener#onPreRequest", parent.getName(), e);

                            preStatus = null;
                        }

                        if (preStatus == null) {
                            try {
                                parent.getListener().onRequest(parent, request, response);
                            } catch (Exception e) {
                                parent.getLogger().error("[{}] Unhandled exception occurred in HttpServerListener#onRequest", parent.getName(), e);

                                response.release();
                                response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
                                response.retain();

                                response.headers().set("X-Request-Id", request.headers().get("X-Request-Id"));
                                response.headers().set("X-Request-Address", request.headers().get("X-Request-Address"));
                                response.headers().set("X-Request-Ip", request.headers().get("X-Request-Ip"));

                                parent.getListener().onError(parent, request, response, HttpResponseStatus.INTERNAL_SERVER_ERROR, null);
                            }
                        } else {
                            response.setStatus(preStatus);

                            parent.getListener().onError(parent, request, response, preStatus, null);
                        }
                    } else {
                        response.setStatus(HttpResponseStatus.BAD_REQUEST);

                        if (request.decoderResult().cause() != null && request.decoderResult().cause().getMessage().equalsIgnoreCase("HTTP over HTTPS")) {
                            parent.getListener().onError(parent, request, response, HttpResponseStatus.BAD_REQUEST, "HTTP was sent to an HTTPS port");
                        } else {
                            parent.getListener().onError(parent, request, response, HttpResponseStatus.BAD_REQUEST, null);
                        }
                    }

                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                } finally {
                    response.release();
                }
            } finally {
                request.release();
            }
        } else if (!(msg instanceof WebSocketFrame)) {
            parent.getLogger().warn("[{}] Unknown type passed to {}, {}!", parent.getName(), this.getClass().getSimpleName(), msg.getClass().getSimpleName());

            ctx.fireChannelRead(msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(@NotNull ChannelHandlerContext ctx) {
        ctx.flush();
    }
}