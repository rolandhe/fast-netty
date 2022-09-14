package com.github.rolandhe.file.def.bizconfig;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class YamlConfig {
    private long global;

    private List<BizConf> configs;

    @Getter
    @Setter
    public static class BizConf extends BizLineConf {
        private List<BizLineConf> subs;
    }

    @Getter
    @Setter
    public static class BizLineConf {
        private String bizLine;
        private long maxLength;
    }
}
