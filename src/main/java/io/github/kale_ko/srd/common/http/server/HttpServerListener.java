package io.github.kale_ko.srd.common.http.server;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HttpServerListener {
    public void onRequest(@NotNull HttpServer server, @NotNull FullHttpRequest request, @NotNull FullHttpResponse response) throws Exception;

    public default @Nullable HttpResponseStatus onPreRequest(@NotNull HttpServer server, @NotNull FullHttpRequest request) throws Exception {
        return null;
    }

    public default void onError(@NotNull HttpServer server, @NotNull FullHttpRequest request, @NotNull FullHttpResponse response, @NotNull HttpResponseStatus status, @Nullable String message) {
        response.setStatus(status);

        byte[] statusContent = String.format("<b>%s %s</b>", status.code(), status.reasonPhrase()).getBytes(StandardCharsets.UTF_8);
        response.headers().set("Content-Length", statusContent.length);
        response.headers().set("Content-Type", "text/html");
        response.content().clear();
        response.content().writeBytes(statusContent);
    }
}