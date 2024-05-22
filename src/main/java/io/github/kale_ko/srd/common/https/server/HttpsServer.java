package io.github.kale_ko.srd.common.https.server;

import io.github.kale_ko.srd.common.http.server.HttpServer;
import io.github.kale_ko.srd.common.https.server.netty.HttpOverHttpsHandler;
import io.github.kale_ko.srd.common.https.server.netty.HttpsExceptionHandler;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class HttpsServer extends HttpServer {
    private final @NotNull Path certificatePath;
    private final @NotNull Path privateKeyPath;

    private SslContext sslContext;

    public HttpsServer(@NotNull Logger logger, @NotNull InetSocketAddress address, @NotNull Path certificatePath, @NotNull Path privateKeyPath) {
        super(logger, address);

        this.certificatePath = certificatePath;
        this.privateKeyPath = privateKeyPath;
    }

    @Override
    public @NotNull String getName() {
        return "Https";
    }

    @Override
    public @NotNull URI getUrl() {
        try {
            return new URI("https", null, this.getAddress().getHostString(), this.getAddress().getPort(), "/", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull URI getWebsocketUrl() {
        if (!wsEnabled) {
            throw new RuntimeException("Websocket not enabled!");
        }

        try {
            return new URI("wss", null, this.getAddress().getHostString(), this.getAddress().getPort(), this.wsPath, null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void run() {
        try {
            if (!Files.exists(certificatePath) && !Files.exists(privateKeyPath)) {
                this.logger.debug("[{}] Creating new self signed certificate for host {}.", this.getName(), address.getHostName());

                SelfSignedCertificate ssc = new SelfSignedCertificate(address.getHostName());
                Files.copy(ssc.certificate().toPath(), certificatePath);
                Files.copy(ssc.privateKey().toPath(), privateKeyPath);
                ssc.delete();
            }

            this.logger.debug("[{}] Loading certificate from {}.", this.getName(), certificatePath.toFile());

            this.sslContext = SslContextBuilder.forServer(certificatePath.toFile(), privateKeyPath.toFile()).startTls(false).build();
        } catch (IOException | CertificateException e) {
            this.logger.throwing(e);

            throw new RuntimeException(e);
        }

        super.run();
    }

    @Override
    protected void getChannelHandlers(@NotNull Channel channel) {
        super.getChannelHandlers(channel);

        channel.pipeline().addFirst("SslHandler", this.sslContext.newHandler(channel.alloc()));
        channel.pipeline().addBefore("SslHandler", "HttpOverHttpsHandler", new HttpOverHttpsHandler(this));
        channel.pipeline().addAfter("SslHandler", "HttpsExceptionHandler", new HttpsExceptionHandler(this));
    }
}