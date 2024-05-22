package io.github.kale_ko.srd.common.http.server;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

public interface HttpServerListener {
    public void onRequest(@NotNull HttpServer server, @NotNull FullHttpRequest request, @NotNull FullHttpResponse response);

    public default HttpResponseStatus onPreRequest(@NotNull HttpServer server, @NotNull FullHttpRequest request) {
        return null;
    }
}