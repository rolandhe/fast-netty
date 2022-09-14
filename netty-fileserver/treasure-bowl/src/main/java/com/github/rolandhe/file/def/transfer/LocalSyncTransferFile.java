package com.github.rolandhe.file.def.transfer;

import com.github.rolandhe.file.api.SyncTransferFile;
import com.github.rolandhe.file.api.entiities.TransferResult;
import com.github.rolandhe.file.api.entiities.UploadFileContext;
import io.netty.handler.codec.http.multipart.FileUpload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
public class LocalSyncTransferFile implements SyncTransferFile {
    @Value("${local.target}")
    private String localRoot;

    @Override
    public TransferResult transferFile(UploadFileContext uploadFileContext) {
        FileUpload fl =  uploadFileContext.getAllFiles().get(0);
        log.info("{} file is memory:{}", fl.getFilename(),fl.isInMemory());
        String ext =  fl.getFilename().substring(fl.getFilename().lastIndexOf("."));
        File target =   new File(localRoot + UUID.randomUUID() + ext);
        try {
//                    FileUtils.copyFile(fl.getFile(), target);
            fl.renameTo(target);
        } catch (IOException e) {
            log.error("",e);
            return new TransferResult();
        }
        TransferResult result = new TransferResult();
        result.setStatus(true);
        result.setTargetFileName(target.getAbsolutePath());
        return result;
    }

    @Override
    public String getStoreName() {
        return "local";
    }
}
