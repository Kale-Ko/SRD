package io.github.kale_ko.srd.manager;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.github.kale_ko.srd.common.http.server.HttpServerListener;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManagerHttpListener implements HttpServerListener {
    protected final @NotNull ManagerServer parent;

    public ManagerHttpListener(@NotNull ManagerServer parent) {
        this.parent = parent;
    }

    public @NotNull ManagerServer getParent() {
        return this.parent;
    }

    @Override
    public void onRequest(@NotNull HttpServer server, @NotNull FullHttpRequest request, @NotNull FullHttpResponse response) throws Exception {
        // Never called because we always return NOT_FOUND in #onPreRequest
    }

    @Override
    public @Nullable HttpResponseStatus onPreRequest(@NotNull HttpServer server, @NotNull FullHttpRequest request) throws Exception {
        return HttpResponseStatus.NOT_FOUND;
    }
}