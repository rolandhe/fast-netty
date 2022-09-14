package com.github.rolandhe.file.api;

import com.github.rolandhe.file.api.entiities.BasicContext;
import com.github.rolandhe.file.api.entiities.DownloadContext;

public interface DownloadAuth {
    boolean preValid(DownloadContext context);
}
