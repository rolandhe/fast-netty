package com.github.rolandhe.file.def.load;

import com.github.rolandhe.file.api.SyncLoadFile;
import com.github.rolandhe.file.api.persist.UploadFileMeta;
import com.github.rolandhe.file.netty.http.NginxDownload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

public class MockS3NginxLoadFile implements SyncLoadFile, NginxDownload {
    @Override
    public LoadResult load(UploadFileMeta meta) {
        String[] items = StringUtils.split(meta.getTargetUrl(),"/");
        LoadResult result = new LoadResult();


        result.setLocalFileName("http://127.0.0.1:8000/s3/"+ items[items.length - 1]);
        result.setExt(FilenameUtils.getExtension(meta.getOriginalFileName()));
        result.setNeedRelease(false);
        return result;
    }
}
