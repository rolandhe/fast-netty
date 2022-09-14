package com.github.rolandhe.file.api.persist;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UploadFileMeta {
    private Long uploadUserId;
    private String bizLine;
    private String originalFileName;
    private String targetUrl;
    private long fileLength;
    private String storeName;
}
