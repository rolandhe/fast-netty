package com.github.rolandhe.file.conf;

import com.github.rolandhe.file.api.AsyncTransferFile;
import com.github.rolandhe.file.api.BizConfig;
import com.github.rolandhe.file.api.DownloadAuth;
import com.github.rolandhe.file.api.ResultResponseGenerator;
import com.github.rolandhe.file.api.SyncLoadFile;
import com.github.rolandhe.file.api.SyncTransferFile;
import com.github.rolandhe.file.api.TransferAuth;
import com.github.rolandhe.file.api.entiities.BasicContext;
import com.github.rolandhe.file.api.persist.MetaPersist;
import com.github.rolandhe.file.def.auth.RsaDownloadAuth;
import com.github.rolandhe.file.def.auth.RsaTransferAuth;
import com.github.rolandhe.file.def.bizconfig.DefaultYamlBizConfig;
import com.github.rolandhe.file.def.load.LocalNginxLoadFile;
import com.github.rolandhe.file.def.load.LocalSyncLoadFile;
import com.github.rolandhe.file.def.load.MockS3NginxLoadFile;
import com.github.rolandhe.file.def.result.DefaultResultResponseGenerator;
import com.github.rolandhe.file.def.transfer.LocalSyncTransferFile;
import com.github.rolandhe.file.def.ThreadsAsyncFile;
import com.github.rolandhe.file.persist.mysql.MysqlMetaPersist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfigure {

    @Value("${transfer.worker-threads}")
    private int workerThreads;


    @Bean
    public SyncTransferFile syncTransferFile() {
        return new LocalSyncTransferFile();
    }

    @Bean
    public SyncLoadFile syncLoadFile() {
        return new MockS3NginxLoadFile();
    }


    @Bean
    public AsyncTransferFile asyncTransferFile(SyncTransferFile syncTransferFile, SyncLoadFile syncLoadFile) {
        return new ThreadsAsyncFile(workerThreads, syncTransferFile, syncLoadFile);
    }


    @Bean
    public ResultResponseGenerator resultResponseGenerator() {
        return new DefaultResultResponseGenerator();
    }

    @Bean
    public TransferAuth transferAuth() {
        return new RsaTransferAuth();
    }

    @Bean
    public DownloadAuth downloadAuth() {
        return new RsaDownloadAuth();
    }

    @Bean
    public BizConfig bizConfig() {
        return new DefaultYamlBizConfig();
    }

    @Bean
    public MetaPersist metaPersist() {
        return new MysqlMetaPersist();
    }
}
