package com.github.rolandhe.file.api;

import com.github.rolandhe.file.api.entiities.BizConfigMeta;

public interface BizConfig {
    BizConfigMeta getConfigByUrl(String url);
}
