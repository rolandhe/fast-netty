package com.github.rolandhe.file.netty.http.config;

import com.github.rolandhe.file.api.AsyncLoadFile;
import com.github.rolandhe.file.api.DownloadAuth;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DownloadConfig {
    private final AsyncLoadFile asyncLoadFile;
    private  final DownloadAuth downloadAuth;


    public DownloadConfig(AsyncLoadFile asyncLoadFile, DownloadAuth downloadAuth) {
        this.asyncLoadFile = asyncLoadFile;
        this.downloadAuth = downloadAuth;
    }
}
