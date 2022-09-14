package com.github.rolandhe.file.netty.http;

import com.github.rolandhe.file.api.entiities.DownloadContext;
import com.github.rolandhe.file.netty.http.config.DownloadConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Semaphore;


@Slf4j
public class HttpServerDownloadHandler extends BaseHttpServerHandler {

    private final DownloadConfig downloadConfig;

    private final DownloadContext downloadContext = new DownloadContext();

    public HttpServerDownloadHandler(DownloadConfig downloadConfig, Semaphore concurrentLimit) {
        super(concurrentLimit);
        this.downloadConfig = downloadConfig;
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        reset();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            parseBasicRequestInfo(request, downloadContext);

            String strId = downloadContext.getQueryStringData().get("id");
            if (strId == null || !StringUtils.isNumeric(strId)) {
                writeAbnormalResponse(ctx.channel(), HttpResponseStatus.PRECONDITION_FAILED);
                return;
            }
            downloadContext.setId(Long.parseLong(strId));

            if (!downloadConfig.getDownloadAuth().preValid(downloadContext)) {
                writeAbnormalResponse(ctx.channel(), HttpResponseStatus.UNAUTHORIZED);
                return;
            }

            if (!super.tryLimit()) {
                writeAbnormalResponse(ctx.channel(), HttpResponseStatus.TOO_MANY_REQUESTS);
                return;
            }

            downloadConfig.getAsyncLoadFile().asyncLoad(downloadContext.getId(), (localFile,ext, len, needRelease) -> {
                if (StringUtils.isEmpty(localFile)) {
                    writeAbnormalResponse(ctx.channel(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
                } else {
                    writeResponse(ctx.channel(), localFile, len, needRelease);
                }
                reset();
            });
        }
    }

    private void reset() {
        this.downloadContext.clear();
        MDC.remove(TRACE_ID);
        release();
        if (this.httpRequest != null) {
            this.httpRequest = null;
        }
    }


    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        boolean canAccept = super.acceptInboundMessage(msg);
        if (!canAccept) {
            return canAccept;
        }

        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            if (request.uri().startsWith("/download/") && request.method().equals(HttpMethod.GET)) {
                return true;
            }
        }
        return false;
    }

    private void writeAbnormalResponse(Channel channel, HttpResponseStatus httpResponseStatus) {

        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(this.httpRequest);
//        boolean keepAlive = false;

        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, httpResponseStatus);
        HttpHeaders httpHeaders = response.headers();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        if (!keepAlive) {
            httpHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (httpRequest.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            httpHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        Set<Cookie> cookies;
        String value = httpRequest.headers().get(HttpHeaderNames.COOKIE);
        if (value == null) {
            cookies = Collections.emptySet();
        } else {
            cookies = ServerCookieDecoder.STRICT.decode(value);
        }
        if (!cookies.isEmpty()) {
            // Reset the cookies if necessary.
            for (Cookie cookie : cookies) {
                httpHeaders.add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
            }
        }
        // Write the response.
        channel.write(response);
        ChannelFuture future = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        // Close the connection after the write operation is done if necessary.
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("met error:{}", this.httpRequest.uri(), cause);
        writeAbnormalResponse(ctx.channel(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
        reset();
    }

    private String getContentType(String ext) {
        String isShow = downloadContext.getQueryStringData().get("show");
        if ("true".equals(isShow)) {
            if ("bmp".equalsIgnoreCase(ext)) {
                return "image/bmp";
            }
            if ("gif".equalsIgnoreCase(ext)) {
                return "image/gif";
            }
            if ("jpeg".equalsIgnoreCase(ext) || "jpg".equalsIgnoreCase(ext)) {
                return "image/jpeg";
            }
            if ("png".equalsIgnoreCase(ext)) {
                return "image/png";
            }
            if ("tiff".equalsIgnoreCase(ext) || "tif".equalsIgnoreCase(ext)) {
                return "image/tiff";
            }
            if ("webp".equalsIgnoreCase(ext)) {
                return "image/webp";
            }
            if ("svg".equalsIgnoreCase(ext)) {
                return "image/svg+xml";
            }
            if ("pdf".equalsIgnoreCase(ext)) {
                return "application/pdf";
            }
        }
        return "application/octet-stream";
    }

    private void writeResponse(Channel channel, String localFile, long len, boolean needRelease) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpUtil.setContentLength(response, len);
//        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        File file = new File(localFile);
        HttpHeaders httpHeaders = response.headers();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, getContentType(FilenameUtils.getExtension(localFile)));

        boolean keepAlive = HttpUtil.isKeepAlive(httpRequest);

        if (!keepAlive) {
            httpHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (httpRequest.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            httpHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        Set<Cookie> cookies;
        String value = httpRequest.headers().get(HttpHeaderNames.COOKIE);
        if (value == null) {
            cookies = Collections.emptySet();
        } else {
            cookies = ServerCookieDecoder.STRICT.decode(value);
        }
        if (!cookies.isEmpty()) {
            // Reset the cookies if necessary.
            for (Cookie cookie : cookies) {
                httpHeaders.add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
            }
        }


        channel.write(response);

        // Write the content.
        channel.write(new DefaultFileRegion(file, 0, len));
        // Write the end marker.
        ChannelFuture lastContentFuture = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (needRelease) {
            lastContentFuture.addListener((ChannelFutureListener) future -> FileUtils.forceDelete(file));
        }
        if (!keepAlive) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
