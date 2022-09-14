package com.github.rolandhe.file.api;

import com.github.rolandhe.file.api.persist.UploadFileMeta;
import lombok.Getter;
import lombok.Setter;

public interface SyncLoadFile {
    @Getter
    @Setter
    class LoadResult{
        private String localFileName;
        private String ext;
        private boolean needRelease;
    }
    LoadResult load(UploadFileMeta meta);
}
