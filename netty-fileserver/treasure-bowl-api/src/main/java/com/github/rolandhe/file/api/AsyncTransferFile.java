package com.github.rolandhe.file.api;

import com.github.rolandhe.file.api.entiities.TransferResult;
import com.github.rolandhe.file.api.entiities.UploadFileContext;

public interface AsyncTransferFile {
    interface Callback{
        void callback(TransferResult result);
    }
    void transfer(UploadFileContext uploadFileContext, Callback callback);
}
