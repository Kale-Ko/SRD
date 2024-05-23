package io.github.kale_ko.srd.manager;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.github.kale_ko.srd.common.https.server.HttpsServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

public class ManagerServer {
    public record Config(@NotNull InetAddress host, int port, boolean ssl, int sslPort, Path sslCertificate, Path sslPrivateKey) {
    }

    protected final @NotNull Logger logger;

    protected @NotNull Config config;

    protected final @NotNull Object statusLock = new Object();
    protected boolean running = false;

    protected HttpServer httpServer;
    protected HttpsServer httpsServer;

    public ManagerServer(@NotNull Logger logger, @NotNull Config config) {
        this.logger = logger;

        this.config = config;
    }

    public @NonBlocking void start() {
        synchronized (this.statusLock) {
            if (this.running) {
                throw new RuntimeException(this.getClass().getSimpleName() + " is already running!");
            }
            this.running = true;

            {
                this.httpServer = new HttpServer(this.logger, new InetSocketAddress(this.config.host(), this.config.port()));

                this.httpServer.setListener(new ManagerHttpListener(this));
                this.httpServer.setWebsocketListener(new ManagerWsListener(this));

                this.httpServer.enableWebsocket("/");
                this.httpServer.start();
            }

            if (this.config.ssl()) {
                this.httpsServer = new HttpsServer(this.logger, new InetSocketAddress(this.config.host(), this.config.sslPort()), this.config.sslCertificate(), this.config.sslPrivateKey());

                this.httpsServer.setListener(new ManagerHttpListener(this));
                this.httpsServer.setWebsocketListener(new ManagerWsListener(this));

                this.httpsServer.enableWebsocket("/");
                this.httpsServer.start();
            }
        }
    }

    public @Blocking void stop() {
        synchronized (this.statusLock) {
            if (!this.running) {
                throw new RuntimeException(this.getClass().getSimpleName() + " is already running!");
            }
            this.running = false;

            if (this.httpsServer != null) {
                this.httpsServer.stop();
            }
            if (this.httpServer != null) {
                this.httpServer.stop();
            }
        }
    }
}