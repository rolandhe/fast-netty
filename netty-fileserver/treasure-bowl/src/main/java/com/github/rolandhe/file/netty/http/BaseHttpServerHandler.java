package com.github.rolandhe.file.netty.http;

import com.github.rolandhe.file.api.entiities.BasicContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Semaphore;

@Slf4j
public abstract class BaseHttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    public static final String TRACE_ID = "trace-id";
    private final Semaphore concurrentLimit;
    protected HttpRequest httpRequest;

    private boolean needRelease;

    protected BaseHttpServerHandler(Semaphore concurrentLimit) {
        this.concurrentLimit = concurrentLimit;
    }

    protected void parseBasicRequestInfo(HttpRequest request, BasicContext basicContext) {
        this.httpRequest = request;
        basicContext.setStartTime(System.currentTimeMillis());

        // new getMethod
        outputHeaders(request, basicContext);
        MDC.put(TRACE_ID, basicContext.getTraceId());
        log.info("start to process {}", request.uri());
        // new getMethod
        outputCookies(request, basicContext);
        outputQueryString(request, basicContext);
    }

    protected void outputQueryString(HttpRequest request, BasicContext basicContext) {
        QueryStringDecoder decoderQuery = new QueryStringDecoder(request.uri());
        basicContext.setUrlPath(decoderQuery.path());
        basicContext.setUri(request.uri());
        Map<String, List<String>> uriAttributes = decoderQuery.parameters();
        for (Entry<String, List<String>> attr : uriAttributes.entrySet()) {
            for (String attrVal : attr.getValue()) {
                basicContext.getQueryStringData().put(attr.getKey(), attrVal);
            }
        }
    }

    protected void outputCookies(HttpRequest request, BasicContext basicContext) {
        Set<Cookie> cookies;
        String value = request.headers().get(HttpHeaderNames.COOKIE);
        if (value == null) {
            cookies = Collections.emptySet();
        } else {
            cookies = ServerCookieDecoder.STRICT.decode(value);
        }
        for (Cookie cookie : cookies) {
            basicContext.getCookieData().put(cookie.name(), cookie.value());
        }
    }

    protected void outputHeaders(HttpRequest request, BasicContext basicContext) {
        for (Entry<String, String> entry : request.headers()) {
            if (entry.getKey().equals(TRACE_ID)) {
                basicContext.setTraceId(entry.getValue());
            }
            basicContext.getHeaderData().put(entry.getKey(), entry.getValue());
        }
    }

    protected boolean tryLimit(){
        if (!concurrentLimit.tryAcquire()) {
            return false;
        }
        needRelease = true;
        return true;
    }

    protected void release() {
        if (needRelease) {
            this.concurrentLimit.release();
            needRelease = false;
        }
    }
}
