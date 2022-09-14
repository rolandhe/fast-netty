package com.github.rolandhe.file.def.load;

import com.github.rolandhe.file.api.SyncLoadFile;
import com.github.rolandhe.file.api.persist.UploadFileMeta;
import org.apache.commons.io.FilenameUtils;

public class LocalSyncLoadFile implements SyncLoadFile {
    @Override
    public LoadResult load(UploadFileMeta meta) {
        String local = meta.getTargetUrl();

        LoadResult loadResult = new LoadResult();
        loadResult.setLocalFileName(local);
        loadResult.setLocalFileName(FilenameUtils.getExtension(local));
        loadResult.setNeedRelease(false);
        return loadResult;
    }
}
