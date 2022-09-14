package com.github.rolandhe.file.api;

import com.github.rolandhe.file.api.entiities.TransferResult;
import com.github.rolandhe.file.api.entiities.UploadFileContext;

public interface SyncTransferFile {
    TransferResult transferFile(UploadFileContext uploadFileContext);
    String getStoreName();
}
