package com.github.rolandhe.file.netty.http;

import com.github.rolandhe.file.api.AsyncLoadFile;
import com.github.rolandhe.file.api.AsyncTransferFile;
import com.github.rolandhe.file.api.BizConfig;
import com.github.rolandhe.file.api.DownloadAuth;
import com.github.rolandhe.file.api.ResultResponseGenerator;
import com.github.rolandhe.file.api.SyncLoadFile;
import com.github.rolandhe.file.api.TransferAuth;
import com.github.rolandhe.file.netty.http.config.DownloadConfig;
import com.github.rolandhe.file.netty.http.config.UploadConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.Semaphore;

@Service
@Slf4j
public class ServerStarter {
    @Value(("${netty.port}"))
    private int port;
    @Value("${netty.boss-threads}")
    private int bossThreads;

    @Value(("${netty.worker-threads}"))
    private int workerThreads;

    @Value(("${netty.max-current}"))
    private int maxConcurrent;

    @Value(("${netty.max-in-memory:1048576}"))
    private int maxInMemory;


    @Resource
    private AsyncTransferFile asyncTransferFile;

    @Resource
    private TransferAuth transferAuth;

    @Resource
    private ResultResponseGenerator resultResponseGenerator;

    @Resource
    private BizConfig bizConfig;

    @Resource
    private AsyncLoadFile asyncLoadFile;

    @Resource
    private DownloadAuth downloadAuth;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Thread thread;

    @Resource
    private SyncLoadFile syncLoadFile;


    @PostConstruct
    public void init() {
        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);
        thread = new Thread(() -> start());
        thread.start();
    }

    @PreDestroy
    public void destroy() {
        thread.interrupt();
    }

    private void start() {
        UploadConfig uploadConfig = new UploadConfig(asyncTransferFile, transferAuth, resultResponseGenerator, bizConfig, maxInMemory);
        Semaphore semaphore = new Semaphore(maxConcurrent);
        DownloadConfig downloadConfig = new DownloadConfig(asyncLoadFile, downloadAuth);

        boolean nginxDownload = syncLoadFile instanceof NginxDownload;

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 512);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HttpServerFileHandlerInitializer(uploadConfig, semaphore, downloadConfig,nginxDownload));
            Channel ch = b.bind(port).sync().channel();
            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            log.info("file server thread InterruptedException", e);
        } catch (RuntimeException e) {
            log.info("file server thread met error", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
