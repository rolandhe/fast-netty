package com.github.rolandhe.file.api.persist;

public interface MetaPersist {
    Long prePersist(UploadFileMeta meta);
    Long persist(UploadFileMeta meta, Long preId,String targetFile);

    UploadFileMeta getMetaByPersistId(long persistId);
}
