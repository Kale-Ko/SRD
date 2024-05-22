package io.github.kale_ko.srd.common.http.server;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import org.jetbrains.annotations.NotNull;

public interface WsServerListener {
    public default void onHandshake(@NotNull HttpServer server, @NotNull FullHttpRequest request, @NotNull FullHttpResponse response) {
    }

    public void onTextMessage(@NotNull WsChannel channel, @NotNull TextWebSocketFrame frame);

    public void onBinaryMessage(@NotNull WsChannel channel, @NotNull BinaryWebSocketFrame frame);

    public default void onPing(@NotNull WsChannel channel, @NotNull PingWebSocketFrame frame) {
        channel.send(new PongWebSocketFrame(frame.content().copy()));
    }

    public default void onPong(@NotNull WsChannel channel, @NotNull PongWebSocketFrame frame) {
    }

    public default void onClose(@NotNull CloseWebSocketFrame frame) {
    }
}