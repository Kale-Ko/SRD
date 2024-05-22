package io.github.kale_ko.srd.common.http.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.jetbrains.annotations.NotNull;

public class WsUpgradeHandler extends ChannelInboundHandlerAdapter {
    protected final @NotNull HttpServer parent;

    public WsUpgradeHandler(@NotNull HttpServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpServer getParent() {
        return this.parent;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
        if (msg instanceof FullHttpRequest request) {
            if (request.uri().startsWith(parent.getWebsocketUrl().getPath())) {
                try {
                    FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    response.retain();

                    try {
                        response.headers().set("X-Request-Id", request.headers().get("X-Request-Id"));
                        response.headers().set("X-Request-Address", request.headers().get("X-Request-Address"));
                        response.headers().set("X-Request-Ip", request.headers().get("X-Request-Ip"));

                        if (request.decoderResult().isSuccess()) {
                            try {
                                if (request.protocolVersion().compareTo(HttpVersion.HTTP_1_1) >= 0) {
                                    if (request.method() == HttpMethod.GET) {
                                        if (!request.headers().contains("Content-Length") || request.headers().getInt("Content-Length") == 0) {
                                            if (request.headers().contains("Connection") && request.headers().containsValue("Connection", "upgrade", true) && request.headers().contains("Upgrade") && request.headers().get("Upgrade").equalsIgnoreCase("websocket")) {
                                                int version = 13;
                                                if (request.headers().contains("Sec-WebSocket-Version")) {
                                                    version = request.headers().getInt("Sec-WebSocket-Version");
                                                }

                                                if (version >= 13 && version <= 17 && request.headers().contains("Sec-WebSocket-Key")) {
                                                    String key = request.headers().get("Sec-WebSocket-Key") + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                                                    String accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(key.getBytes(StandardCharsets.UTF_8)));

                                                    response.setStatus(HttpResponseStatus.SWITCHING_PROTOCOLS);
                                                    response.headers().set("Connection", "upgrade");
                                                    response.headers().set("Upgrade", "websocket");
                                                    response.headers().set("Sec-WebSocket-Version", version);
                                                    response.headers().set("Sec-WebSocket-Accept", accept);

                                                    parent.getWebsocketListener().onHandshake(parent, request, response);

                                                    ctx.pipeline().addAfter("HttpResponseEncoder", "WebSocketFrameEncoder", new WebSocket13FrameEncoder(false));
                                                    ctx.pipeline().addAfter("HttpRequestDecoder", "WebSocketFrameDecoder", new WebSocket13FrameDecoder(WebSocketDecoderConfig.newBuilder().expectMaskedFrames(true).allowMaskMismatch(false).allowExtensions(false).closeOnProtocolViolation(true).build()));
                                                    ctx.pipeline().remove("HttpRequestDecoder");
                                                } else {
                                                    parent.getListener().onError(parent, request, response, HttpResponseStatus.BAD_REQUEST, null);
                                                }
                                            } else {
                                                parent.getListener().onError(parent, request, response, HttpResponseStatus.UPGRADE_REQUIRED, null);
                                            }
                                        } else {
                                            parent.getListener().onError(parent, request, response, HttpResponseStatus.BAD_REQUEST, null);
                                        }
                                    } else {
                                        parent.getListener().onError(parent, request, response, HttpResponseStatus.METHOD_NOT_ALLOWED, null);
                                    }
                                } else {
                                    parent.getListener().onError(parent, request, response, HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED, null);
                                }
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
            } else {
                ctx.fireChannelRead(msg);
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