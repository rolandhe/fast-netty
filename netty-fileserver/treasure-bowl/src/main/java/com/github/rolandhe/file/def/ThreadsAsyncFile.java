package com.github.rolandhe.file.def;

import com.github.rolandhe.file.api.AsyncLoadFile;
import com.github.rolandhe.file.api.AsyncTransferFile;
import com.github.rolandhe.file.api.SyncLoadFile;
import com.github.rolandhe.file.api.SyncLoadFile.LoadResult;
import com.github.rolandhe.file.api.SyncTransferFile;
import com.github.rolandhe.file.api.entiities.TransferResult;
import com.github.rolandhe.file.api.entiities.UploadFileContext;
import com.github.rolandhe.file.api.persist.MetaPersist;
import com.github.rolandhe.file.api.persist.UploadFileMeta;
import io.netty.handler.codec.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ThreadsAsyncFile implements AsyncTransferFile, AsyncLoadFile {
    private final ExecutorService executorService;

    private final int maxThread;
    private final SyncTransferFile syncTransferFile;
    private final SyncLoadFile syncLoadFile;

    @Resource
    private MetaPersist metaPersist;

    public ThreadsAsyncFile(int maxThread, SyncTransferFile syncTransferFile, SyncLoadFile syncLoadFile) {
        this.maxThread = maxThread;
        this.executorService = buildExecutors();
        this.syncTransferFile = syncTransferFile;
        this.syncLoadFile = syncLoadFile;
    }

    private ExecutorService buildExecutors() {
        return Executors.newFixedThreadPool(maxThread, r -> {
            Thread t = new Thread(r);
            t.setName("AsyncTransferFile-" + t.getId());
            return t;
        });
    }

    @Override
    public void transfer(UploadFileContext uploadFileContext, Callback callback) {
        String traceId = MDC.get("trace-id");
        UploadFileMeta meta = fromUploadFileContext(uploadFileContext);
        CompletableFuture.runAsync(() -> {
            try {
                MDC.put("trace-id", traceId);

                Long preId = metaPersist.prePersist(meta);
                if (preId == null) {
                    failedCallback(callback);
                    return;
                }

                TransferResult transferResult = syncTransferFile.transferFile(uploadFileContext);

                if (transferResult.getStatus()) {
                    Long id = metaPersist.persist(meta, preId, transferResult.getTargetFileName());
                    if (id != null) {
                        transferResult.setId(id);
                        callback.callback(transferResult);
                        return;
                    }
                }
                failedCallback(callback);
            } catch (RuntimeException e) {
                log.error("transfer file error.userid={},file={}", meta.getUploadUserId(), meta.getOriginalFileName(), e);
                failedCallback(callback);
            } finally {
                MDC.remove("trace-id");
            }
        }, executorService);
    }

    private void failedCallback(Callback callback) {
        TransferResult result = new TransferResult();
        callback.callback(result);
    }

    private UploadFileMeta fromUploadFileContext(UploadFileContext uploadFileContext) {
        UploadFileMeta meta = new UploadFileMeta();
        meta.setStoreName(syncTransferFile.getStoreName());
        meta.setUploadUserId(uploadFileContext.getUserId());
        meta.setBizLine(uploadFileContext.getUrlPath());
        meta.setOriginalFileName(uploadFileContext.getAllFiles().get(0).getFilename());
        meta.setTargetUrl("unknown");
        meta.setFileLength(uploadFileContext.getAllFiles().get(0).length());
        return meta;
    }

    @Override
    public void asyncLoad(Long id, GenResponseCallback responseCallback) {

        String traceId = MDC.get("trace-id");

        CompletableFuture.runAsync(() -> {
            UploadFileMeta meta = null;
            try {
                MDC.put("trace-id", traceId);
                meta = metaPersist.getMetaByPersistId(id);
                if (meta == null) {
                    responseCallback.callback(null, null,0, false);
                    return;
                }
                LoadResult loadResult = syncLoadFile.load(meta);
                responseCallback.callback(loadResult.getLocalFileName(),loadResult.getExt(), meta.getFileLength(), loadResult.isNeedRelease());
            } catch (RuntimeException e) {
                log.error("transfer file error.userid={},file={}", meta == null ? -1 : meta.getUploadUserId(), meta == null ? "" : meta.getOriginalFileName(), e);
                responseCallback.callback(null, null,0, false);
            } finally {
                MDC.remove("trace-id");
            }
        }, executorService);
    }
}
