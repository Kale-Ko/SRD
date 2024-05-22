package io.github.kale_ko.srd.common.http.server;

import io.github.kale_ko.srd.common.http.server.netty.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

public class HttpServer {
    protected final @NotNull Logger logger;

    protected final @NotNull InetSocketAddress address;

    protected final @NotNull Object statusLock = new Object();
    protected boolean running = false;

    protected Thread thread;
    protected EventLoopGroup serverWorker;
    protected EventLoopGroup connectionWorker;

    public HttpServer(@NotNull Logger logger, @NotNull InetSocketAddress address) {
        this.logger = logger;

        this.address = address;
    }

    public @NotNull Logger getLogger() {
        return this.logger;
    }

    public @NotNull InetSocketAddress getAddress() {
        return this.address;
    }

    public @NotNull String getName() {
        return "Http";
    }

    public @NotNull URI getUrl() {
        try {
            return new URI("http", null, this.getAddress().getHostString(), this.getAddress().getPort(), "/", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    public @NonBlocking void start() {
        synchronized (this.statusLock) {
            if (this.running) {
                throw new RuntimeException(this.getClass().getSimpleName() + " is already running!");
            }
            this.running = true;

            this.logger.info("[{}] Starting...", this.getName());

            this.thread = new Thread(this::run, this.getClass().getSimpleName() + "[address=" + this.address + "]");
            this.thread.setDaemon(true);
            this.thread.start();
        }
    }

    public @Blocking void stop() {
        synchronized (this.statusLock) {
            if (!this.running) {
                throw new RuntimeException(this.getClass().getSimpleName() + " is already running!");
            }
            this.running = false;

            this.logger.info("[{}] Stopping...", this.getName());

            Future<?> serverFuture = null;
            Future<?> connectionFuture = null;
            if (this.serverWorker != null) {
                serverFuture = this.serverWorker.shutdownGracefully(500, 5000, TimeUnit.MILLISECONDS);
            }
            if (this.connectionWorker != null) {
                connectionFuture = this.connectionWorker.shutdownGracefully(500, 5000, TimeUnit.MILLISECONDS);
            }

            if (serverFuture != null) {
                serverFuture.syncUninterruptibly();
            }
            if (connectionFuture != null) {
                connectionFuture.syncUninterruptibly();
            }
        }
    }

    protected void run() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            this.serverWorker = new NioEventLoopGroup(8);
            this.connectionWorker = new NioEventLoopGroup(512);

            bootstrap.channel(NioServerSocketChannel.class).group(this.serverWorker, this.connectionWorker);

            bootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                public void initChannel(NioSocketChannel channel) {
                    HttpServer.this.getChannelHandlers(channel);
                }
            });

            bootstrap.childOption(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_BACKLOG, 16).childOption(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.validate();

            {
                Channel channel = bootstrap.bind(this.address).syncUninterruptibly().channel();

                this.logger.info("[{}] Successfully started at {}", this.getName(), this.getUrl());

                {
                    channel.closeFuture().syncUninterruptibly();

                    this.logger.info("[{}] Successfully stopped.", this.getName());
                }
            }
        } catch (Exception e) {
            this.logger.throwing(e);

            throw e;
        }
    }

    protected void getChannelHandlers(@NotNull Channel channel) {
        channel.pipeline().addLast("HttpResponseEncoder", new HttpResponseEncoder());
        channel.pipeline().addLast("HttpRequestDecoder", new HttpRequestDecoder(512, 8192, 8192));
        channel.pipeline().addLast("HttpResponseMetaHandler", new HttpResponseMetaHandler(this));
        channel.pipeline().addLast("HttpRequestMetaHandler", new HttpRequestMetaHandler(this));
        channel.pipeline().addLast("HttpLoggingHandler", new HttpLoggingHandler(this));
        channel.pipeline().addLast("HttpResponseHandler", new HttpResponseHandler(this));
        channel.pipeline().addLast("HttpContentCompressor[deflate]", new HttpContentCompressor(StandardCompressionOptions.deflate(9, 15, 8)));
        channel.pipeline().addLast("HttpContentCompressor[gzip]", new HttpContentCompressor(StandardCompressionOptions.gzip(9, 15, 8)));
        channel.pipeline().addLast("HttpContentCompressor[brotli]", new HttpContentCompressor(StandardCompressionOptions.brotli()));
        channel.pipeline().addLast("HttpContentCompressor[zstd]", new HttpContentCompressor(StandardCompressionOptions.zstd(9, 15, 1024 * 32)));
        channel.pipeline().addLast("HttpContentDecompressor", new HttpContentDecompressor());
        channel.pipeline().addLast("HttpKeepAliveHandler", new HttpKeepAliveHandler(this));
        channel.pipeline().addLast("HttpObjectAggregator", new HttpObjectAggregator(20971520));
        channel.pipeline().addAfter("HttpObjectAggregator", "HttpRequestHandler", new HttpRequestHandler(this));
        channel.pipeline().addLast("HttpExceptionHandler", new HttpExceptionHandler(this));
    }
}