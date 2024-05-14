package io.github.kale_ko.srd.server.https;

import io.github.kale_ko.srd.server.http.HttpServer;
import io.github.kale_ko.srd.server.https.netty.HttpOverHttpsHandler;
import io.github.kale_ko.srd.server.https.netty.HttpsExceptionHandler;
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
    private final @NotNull SslContext sslContext;

    public HttpsServer(@NotNull Logger logger, @NotNull InetSocketAddress address, @NotNull Path certificatePath, @NotNull Path privateKeyPath) {
        super(logger, address);

        try {
            if (!Files.exists(certificatePath) && !Files.exists(privateKeyPath)) {
                this.logger.debug("Creating new self signed certificate for host {}.", address.getHostName());

                SelfSignedCertificate ssc = new SelfSignedCertificate(address.getHostName());
                Files.copy(ssc.certificate().toPath(), certificatePath);
                Files.copy(ssc.privateKey().toPath(), privateKeyPath);
                ssc.delete();
            }

            this.sslContext = SslContextBuilder.forServer(certificatePath.toFile(), privateKeyPath.toFile()).startTls(false).build();
        } catch (IOException | CertificateException e) {
            throw new RuntimeException(e);
        }
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
    protected void getChannelHandlers(@NotNull Channel channel) {
        super.getChannelHandlers(channel);

        channel.pipeline().addFirst("SslHandler", this.sslContext.newHandler(channel.alloc()));
        channel.pipeline().addBefore("SslHandler", "HttpOverHttpsHandler", new HttpOverHttpsHandler(this));
        channel.pipeline().addAfter("SslHandler", "HttpsExceptionHandler", new HttpsExceptionHandler(this));
    }
}