package com.github.rolandhe.file.api.entiities;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class BasicContext {
    private final Map<String, String> headerData = new HashMap<>();
    private final Map<String, String> cookieData = new HashMap<>();
    private final Map<String, String> queryStringData = new HashMap<>();



    private String uri;
    private long startTime;

    private String urlPath;

    private String traceId;

    private Long userId;

    public void clear(){
        headerData.clear();
        cookieData.clear();
        queryStringData.clear();
        uri = null;
        startTime = -1;
        traceId = null;
        userId = null;
        urlPath = null;
    }
}
