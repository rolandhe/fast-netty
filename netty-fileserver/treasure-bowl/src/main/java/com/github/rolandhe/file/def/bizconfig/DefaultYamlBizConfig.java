package com.github.rolandhe.file.def.bizconfig;

import com.github.rolandhe.file.api.BizConfig;
import com.github.rolandhe.file.api.entiities.BizConfigMeta;
import com.github.rolandhe.file.def.bizconfig.YamlConfig.BizConf;
import com.github.rolandhe.file.def.bizconfig.YamlConfig.BizLineConf;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class DefaultYamlBizConfig implements BizConfig {
    private Map<String, BizConfigMeta> configMetaMap = new HashMap<>();
    private final String GLOBAL = "global";

    @PostConstruct
    public void init() {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("biz-conf/config.yml")) {
            Yaml yaml = new Yaml(new Constructor(YamlConfig.class));
            YamlConfig yamlConfig = yaml.load(inputStream);
            BizConfigMeta global = new BizConfigMeta();
            global.setBizName(GLOBAL);
            global.setMaxLength(yamlConfig.getGlobal());
            configMetaMap.put(GLOBAL, global);

            for(BizConf bizConf : yamlConfig.getConfigs()){
                BizConfigMeta meta = bizConf2Meta(bizConf);
                configMetaMap.put(meta.getUrl(),meta);
                for(BizLineConf bizLineConf : bizConf.getSubs()){
                    BizConfigMeta subMeta = bizConf2Meta(bizLineConf);
                    configMetaMap.put(subMeta.getUrl(),subMeta);
                }
            }

        } catch (IOException e) {
            log.error("load config error", e);
            throw new RuntimeException(e);
        }
    }
    private BizConfigMeta bizConf2Meta(BizLineConf bizLineConf){
        BizConfigMeta meta = new BizConfigMeta();
        meta.setBizName(getBizName(bizLineConf.getBizLine()));
        meta.setUrl(bizLineConf.getBizLine());
        meta.setMaxLength(bizLineConf.getMaxLength());
        return meta;
    }

    @Override
    public BizConfigMeta getConfigByUrl(String url) {
        BizConfigMeta meta = configMetaMap.get(url);
        if(meta != null){
            return meta;
        }

        getBizName(url);
        meta = configMetaMap.get(getBizName(url));
        if(meta != null){
            return meta;
        }
        return configMetaMap.get(GLOBAL);
    }

    private String getBizName(String url){
        List<String> itemList = Arrays.stream(StringUtils.split(url, "/")).filter(s -> StringUtils.isNotEmpty(s)).collect(Collectors.toList());
        return itemList.get(0);
    }
}
