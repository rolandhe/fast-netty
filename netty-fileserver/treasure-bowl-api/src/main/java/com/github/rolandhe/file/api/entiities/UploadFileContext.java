package com.github.rolandhe.file.api.entiities;

import io.netty.handler.codec.http.multipart.FileUpload;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class UploadFileContext extends BasicContext {
    private final List<FileUpload> allFiles = new ArrayList<>();
    private final Map<String, String> formData = new HashMap<>();





    public void clear() {
        super.clear();
        allFiles.clear();
        formData.clear();
    }
}
