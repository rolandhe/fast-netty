package com.github.rolandhe.file.def.auth;

import com.github.rolandhe.file.api.TransferAuth;
import com.github.rolandhe.file.api.entiities.UploadFileContext;
import com.github.rolandhe.file.json.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RsaTransferAuth extends RsaSupport implements TransferAuth {

//    private static final PublicKey publicKey;
//    private static final String AUTH_TOKEN_NAME = "auth-token";
//
//    static {
//        try (InputStream inputStream = RsaTransferAuth.class.getClassLoader().getResourceAsStream("rsa/public_key.pem")) {
//            publicKey = SecurityUtils.readPemPublicKey(inputStream);
//        } catch (IOException e) {
//            log.error("load public key error.", e);
//            throw new RuntimeException(e);
//        }
//    }

    @Override
    public boolean preValid(UploadFileContext uploadFileContext) {
        String token = uploadFileContext.getHeaderData().get(AUTH_TOKEN_NAME);
        if (StringUtils.isEmpty(token)) {
            log.warn("can't find token in header,{}.", uploadFileContext.getUri());
            return false;
        }
        String json = SecurityUtils.rsaDecryptString(publicKey, token);

        RsaAuth rsaAuth = JsonHelper.fromJson(json, RsaAuth.class);
        if (rsaAuth == null) {
            return false;
        }
        if (rsaAuth.getExpiredAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        if (uploadFileContext.getUrlPath().equals(rsaAuth.getUrl())) {
            uploadFileContext.setUserId(rsaAuth.getUserId());
            return true;
        }
        if (StringUtils.isNotEmpty(rsaAuth.getBizLine()) && rsaAuth.getBizLine().equals(getBizName(uploadFileContext.getUrlPath()))) {
            uploadFileContext.setUserId(rsaAuth.getUserId());
            return true;
        }

        return false;
    }

    @Override
    public boolean nextValid(UploadFileContext uploadFileContext) {
        return true;
    }



    private String getBizName(String url) {
        List<String> itemList = Arrays.stream(StringUtils.split(url, "/")).filter(s -> StringUtils.isNotEmpty(s)).collect(Collectors.toList());
        return itemList.get(0);
    }
}
