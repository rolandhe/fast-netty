package com.github.rolandhe.file.netty.http.config;

import com.github.rolandhe.file.api.AsyncTransferFile;
import com.github.rolandhe.file.api.BizConfig;
import com.github.rolandhe.file.api.ResultResponseGenerator;
import com.github.rolandhe.file.api.TransferAuth;
import lombok.Getter;

@Getter
public class UploadConfig {
    private final AsyncTransferFile asyncTransferFile;
    private final TransferAuth transferAuth;
    private final ResultResponseGenerator resultResponseGenerator;
    private final BizConfig bizConfig;

    private final long maxInMemory;


    public UploadConfig(AsyncTransferFile asyncTransferFile, TransferAuth transferAuth, ResultResponseGenerator resultResponseGenerator, BizConfig bizConfig, long maxInMemory) {
        this.asyncTransferFile = asyncTransferFile;
        this.transferAuth = transferAuth;
        this.resultResponseGenerator = resultResponseGenerator;
        this.bizConfig = bizConfig;
        this.maxInMemory = maxInMemory;
    }
}
