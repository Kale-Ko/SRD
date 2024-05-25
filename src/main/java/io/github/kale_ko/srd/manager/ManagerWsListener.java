package io.github.kale_ko.srd.manager;

import io.github.kale_ko.srd.common.http.server.WsChannel;
import io.github.kale_ko.srd.common.http.server.WsServerListener;
import io.github.kale_ko.srd.manager.protocol.Message;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;

public class ManagerWsListener implements WsServerListener {
    protected final @NotNull ManagerServer parent;

    public ManagerWsListener(@NotNull ManagerServer parent) {
        this.parent = parent;
    }

    public @NotNull ManagerServer getParent() {
        return this.parent;
    }

    protected static final @NotNull AttributeKey<String> AUTHENTICATION_ATTRIBUTE = AttributeKey.newInstance("manager:authentication");

    @Override
    public void onTextMessage(@NotNull WsChannel channel, @NotNull TextWebSocketFrame frame) {
        channel.send(new CloseWebSocketFrame(WebSocketCloseStatus.INVALID_MESSAGE_TYPE));
    }

    @Override
    public void onBinaryMessage(@NotNull WsChannel channel, @NotNull BinaryWebSocketFrame frame) {
        ByteBuf data = frame.content().copy();
        data.retain();

        try {
            Message message = Message.read(data);
            if (message == null) {
                channel.send(new CloseWebSocketFrame(WebSocketCloseStatus.INVALID_PAYLOAD_DATA));
                return;
            }

            System.out.println(message);

            switch (message.getType()) {
                case HELLO -> {
                }
                case GOODBYE -> {
                }
            }
        } finally {
            data.release();
        }
    }

    @Override
    public void onOpen(@NotNull WsChannel channel) {
        channel.attr(AUTHENTICATION_ATTRIBUTE).set(null);
    }

    @Override
    public void onClose(@NotNull CloseWebSocketFrame frame) {

    }
}