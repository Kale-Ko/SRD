package io.github.kale_ko.srd.common.http.server.netty;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.github.kale_ko.srd.common.http.server.WsChannel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;

public class WsFrameHandler extends ChannelDuplexHandler {
    protected final @NotNull HttpServer parent;

    protected enum CloseState {
        OPEN,
        SERVER_INIT,
        CLIENT_INIT,
        CLOSED
    }

    protected static final @NotNull AttributeKey<CloseState> CLOSESTATE_ATTRIBUTE = AttributeKey.newInstance("closeState");

    public WsFrameHandler(@NotNull HttpServer parent) {
        this.parent = parent;
    }

    public @NotNull HttpServer getParent() {
        return this.parent;
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) {
        ctx.channel().attr(CLOSESTATE_ATTRIBUTE).set(CloseState.OPEN);

        ctx.fireChannelActive();
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
        if (msg instanceof WebSocketFrame frame) {
            if (frame instanceof TextWebSocketFrame textFrame) {
                parent.getWebsocketListener().onTextMessage(new WsChannel(ctx.channel()), textFrame);
            } else if (frame instanceof BinaryWebSocketFrame binaryFrame) {
                parent.getWebsocketListener().onBinaryMessage(new WsChannel(ctx.channel()), binaryFrame);
            } else if (frame instanceof PingWebSocketFrame pingFrame) {
                parent.getWebsocketListener().onPing(new WsChannel(ctx.channel()), pingFrame);
            } else if (frame instanceof PongWebSocketFrame pongFrame) {
                parent.getWebsocketListener().onPong(new WsChannel(ctx.channel()), pongFrame);
            } else if (frame instanceof CloseWebSocketFrame closeFrame) {
                CloseState closeState = ctx.channel().attr(CLOSESTATE_ATTRIBUTE).get();

                if (closeState == CloseState.OPEN) {
                    ctx.channel().attr(CLOSESTATE_ATTRIBUTE).set(CloseState.CLIENT_INIT);

                    ctx.writeAndFlush(new CloseWebSocketFrame(WebSocketCloseStatus.NORMAL_CLOSURE));

                    ctx.channel().attr(CLOSESTATE_ATTRIBUTE).set(CloseState.CLOSED);
                    ctx.close();
                } else if (closeState == CloseState.SERVER_INIT) {
                    ctx.channel().attr(CLOSESTATE_ATTRIBUTE).set(CloseState.CLOSED);
                    ctx.close();

                    parent.getWebsocketListener().onClose(closeFrame);
                } else {
                    parent.getLogger().warn("[{}] Invalid close state during close frame receive, {}!", parent.getName(), closeState);
                }
            }
        } else {
            parent.getLogger().warn("[{}] Unknown type passed to {}, {}!", parent.getName(), this.getClass().getSimpleName(), msg.getClass().getSimpleName());

            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(@NotNull ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void write(@NotNull ChannelHandlerContext ctx, @NotNull Object msg, @NotNull ChannelPromise promise) {
        if (msg instanceof CloseWebSocketFrame) {
            CloseState closeState = ctx.channel().attr(CLOSESTATE_ATTRIBUTE).get();

            if (closeState == CloseState.OPEN) {
                ctx.channel().attr(CLOSESTATE_ATTRIBUTE).set(CloseState.SERVER_INIT);
            } else if (closeState != CloseState.CLIENT_INIT) {
                parent.getLogger().warn("[{}] Invalid close state during close frame receive, {}!", parent.getName(), closeState);
            }
        }

        ctx.write(msg);
    }
}