package com.github.rolandhe.file.api;

public interface AsyncLoadFile {
    interface GenResponseCallback{
        void callback(String localFile,String ext,long len,boolean needRelease);
    }
    void asyncLoad(Long id,GenResponseCallback responseCallback);
}
