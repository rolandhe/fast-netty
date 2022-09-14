package com.github.rolandhe.file.netty.http;

import com.github.rolandhe.file.netty.http.config.DownloadConfig;
import com.github.rolandhe.file.netty.http.config.UploadConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;

import java.util.concurrent.Semaphore;

public class HttpServerFileHandlerInitializer extends ChannelInitializer<SocketChannel> {
    private final Semaphore maxConcurrent;
    private final UploadConfig uploadConfig;
    private final DownloadConfig downloadConfig;
    private final boolean nginxDownload;

    public HttpServerFileHandlerInitializer(UploadConfig uploadConfig, Semaphore concurrentLimit, DownloadConfig downloadConfig,boolean nginxDownload) {
        this.uploadConfig = uploadConfig;
        this.maxConcurrent = concurrentLimit;
        this.downloadConfig = downloadConfig;
        this.nginxDownload = nginxDownload;
    }


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpServerExpectContinueHandler());
        p.addLast(buildDownload());
        p.addLast(new HttpServerUpFileHandler(uploadConfig,maxConcurrent));
    }

    private ChannelHandler buildDownload(){
        if(nginxDownload) {
            return new NginxHttpServerDownloadHandler(downloadConfig, maxConcurrent);
        }
        return new HttpServerDownloadHandler(downloadConfig, maxConcurrent);
    }

}
