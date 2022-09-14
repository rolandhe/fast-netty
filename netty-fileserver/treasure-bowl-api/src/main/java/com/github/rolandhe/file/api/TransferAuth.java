package com.github.rolandhe.file.api;


import com.github.rolandhe.file.api.entiities.UploadFileContext;

public interface TransferAuth {
    boolean preValid(UploadFileContext uploadFileContext);
    boolean nextValid(UploadFileContext uploadFileContext);
}
