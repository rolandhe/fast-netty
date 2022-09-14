package com.github.rolandhe.file.api.entiities;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DownloadContext extends BasicContext{
    private Long id;

    @Override
    public void clear(){
        super.clear();
        this.id = null;
    }
}
