package com.github.rolandhe.file.netty.http;

import com.github.rolandhe.file.api.entiities.OutputTransferResult;
import com.github.rolandhe.file.api.entiities.UploadFileContext;
import com.github.rolandhe.file.netty.http.config.UploadConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

@Slf4j
public class HttpServerUpFileHandler extends BaseHttpServerHandler {
    private HttpData partialContent;
    private HttpPostRequestDecoder decoder;

    private final UploadFileContext uploadFileContext = new UploadFileContext();

    private final HttpDataFactory factory;
    private final UploadConfig uploadConfig;


    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
        // on exit (in normal
        // exit)
        DiskFileUpload.baseDirectory = null; // system temp directory
        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
        // exit (in normal exit)
        DiskAttribute.baseDirectory = null; // system temp directory
    }

    public HttpServerUpFileHandler(UploadConfig uploadConfig, Semaphore concurrentLimit) {
        super(concurrentLimit);
        this.uploadConfig = uploadConfig;
        this.factory = new DefaultHttpDataFactory(uploadConfig.getMaxInMemory()); // Disk if size exceed
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        reset(false);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            // if GET Method: should not try to create an HttpPostRequestDecoder
            if (!HttpMethod.POST.equals(request.method())) {
                writeResponse(ctx.channel(), uploadConfig.getResultResponseGenerator().invalidRequest(), false);
                return;
            }

            parseBasicRequestInfo(request, uploadFileContext);

            if (!uploadConfig.getTransferAuth().preValid(uploadFileContext)) {
                writeResponse(ctx.channel(), uploadConfig.getResultResponseGenerator().genNotAuthing(), true);
                return;
            }


            if (!tryLimit()) {
                writeResponse(ctx.channel(), uploadConfig.getResultResponseGenerator().exceedConcurrent(), true);
                return;
            }
            long maxFileContentLimit = uploadConfig.getBizConfig().getConfigByUrl(uploadFileContext.getUrlPath()).getMaxLength();
            factory.setMaxLimit(maxFileContentLimit);
            decoder = new HttpPostRequestDecoder(factory, request);
            if (!decoder.isMultipart()) {
                throw new RuntimeException("not multipart");
            }
        }

        // check if the decoder was constructed before
        // if not it handles the form get
        if (decoder != null) {
            if (msg instanceof HttpContent) {
                // New chunk is received
                HttpContent chunk = (HttpContent) msg;
                decoder.offer(chunk);
                readHttpDataChunkByChunk();
                // example of reading only if at the end
                if (chunk instanceof LastHttpContent) {
                    processLastFile(ctx, uploadFileContext.getStartTime());
                }
            }
        }
    }


    private void processLastFile(ChannelHandlerContext ctx, long start) {
        uploadConfig.getAsyncTransferFile().transfer(uploadFileContext, transferResult -> {
            try {
                OutputTransferResult outputTransferResult = new OutputTransferResult();
                outputTransferResult.setId(transferResult.getId());
                outputTransferResult.setStatus(transferResult.getStatus());
                if (outputTransferResult.getStatus()) {
                    writeResponse(ctx.channel(), uploadConfig.getResultResponseGenerator().forResult(outputTransferResult), false);
                } else {
                    writeResponse(ctx.channel(), uploadConfig.getResultResponseGenerator().internalError(), false);
                }
            } finally {
                long cost = System.currentTimeMillis() - start;
                log.info("url is {},cost {} ms", uploadFileContext.getUri(), cost);
                reset(false);
            }
        });
        MDC.remove(TRACE_ID);
    }


    private void reset(boolean notClean) {
        this.uploadFileContext.clear();
        MDC.remove(TRACE_ID);
        if (decoder == null) {
            release();
            partialContent = null;
            super.httpRequest = null;
            return;
        }
        try {
            if (!notClean) {
                try {
                    List<InterfaceHttpData> datas = decoder.getBodyHttpDatas();
                    for (InterfaceHttpData httpData : datas) {
                        httpData.release();
                        factory.removeHttpDataFromClean(super.httpRequest, httpData);
                    }
                } catch (NotEnoughDataDecoderException e) {
                    log.info(null, e);
                }
            }
            factory.cleanAllHttpData();
            // destroy the decoder to release all resources
            decoder.destroy();

        } finally {
            release();
            partialContent = null;
            super.httpRequest = null;
            decoder = null;
        }
    }


    /**
     * Example of reading request by chunk and getting values from chunk to chunk
     */
    private void readHttpDataChunkByChunk() {
        while (decoder.hasNext()) {
            InterfaceHttpData data = decoder.next();
            if (data != null) {
                // check if current HttpData is a FileUpload and previously set as partial
                if (partialContent == data) {
                    partialContent = null;
                }
                // new value
                acceptHttpData(data);
            }
        }
        // Check partial decoding for a FileUpload
        InterfaceHttpData data = decoder.currentPartialHttpData();
        if (data != null) {
            if (partialContent == null) {
                partialContent = (HttpData) data;
            }
        }
    }

    private void acceptHttpData(InterfaceHttpData data) {
        if (data.getHttpDataType() == HttpDataType.Attribute) {
            Attribute attribute = (Attribute) data;
            try {
                uploadFileContext.getFormData().put(attribute.getName(), attribute.getValue());
            } catch (IOException e1) {
                log.error(null, e1);
                throw new RuntimeException(e1);
            }
        } else {
            if (data.getHttpDataType() == HttpDataType.FileUpload) {
                FileUpload fileUpload = (FileUpload) data;
                if (fileUpload.isCompleted()) {
                    uploadFileContext.getAllFiles().add(fileUpload);
                }
            }
        }
    }

    private void writeResponse(Channel channel, String responseString, boolean forceClose) {
        ByteBuf buf = Unpooled.copiedBuffer(responseString, CharsetUtil.UTF_8);

        // Decide whether to close the connection or not.
        boolean keepAlive = !forceClose && HttpUtil.isKeepAlive(super.httpRequest);

        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
//        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, uploadConfig.getResultResponseGenerator().contextType());
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());


        if (!keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (super.httpRequest.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        Set<Cookie> cookies;
        String value = super.httpRequest.headers().get(HttpHeaderNames.COOKIE);
        if (value == null) {
            cookies = Collections.emptySet();
        } else {
            cookies = ServerCookieDecoder.STRICT.decode(value);
        }
        if (!cookies.isEmpty()) {
            // Reset the cookies if necessary.
            for (Cookie cookie : cookies) {
                response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
            }
        }
        // Write the response.
        ChannelFuture future = channel.writeAndFlush(response);
        // Close the connection after the write operation is done if necessary.
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("met error:{}", super.httpRequest.uri(), cause);
        if (cause.getMessage().contains("Size exceed allowed maximum capacity")) {
            reset(true);
            writeResponse500(ctx.channel(), uploadConfig.getResultResponseGenerator().tooLarge());
        } else {
            reset(false);
            writeResponse500(ctx.channel(), uploadConfig.getResultResponseGenerator().internalError());
        }
    }

    private void writeResponse500(Channel channel, String responseString) {
        ByteBuf buf = Unpooled.copiedBuffer(responseString, CharsetUtil.UTF_8);


        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, uploadConfig.getResultResponseGenerator().contextType());
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());


        // Write the response.
        ChannelFuture future = channel.writeAndFlush(response);
        // Close the connection after the write operation is done if necessary.
        future.addListener(ChannelFutureListener.CLOSE);
    }
}
