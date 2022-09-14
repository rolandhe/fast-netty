package com.github.rolandhe.file.def.auth;

import com.github.rolandhe.file.api.DownloadAuth;
import com.github.rolandhe.file.api.entiities.DownloadContext;
import org.apache.commons.lang3.StringUtils;

public class RsaDownloadAuth extends RsaSupport implements DownloadAuth {
    @Override
    public boolean preValid(DownloadContext context) {

        return true;

//        String token = context.getQueryStringData().get(AUTH_TOKEN_NAME);
//        if (StringUtils.isEmpty(token)) {
//            return false;
//        }
//
//        String decodeString = SecurityUtils.rsaDecryptString(publicKey, token);
//
//        if ((context.getUserId() + "-" + context.getId()).equals(decodeString)) {
//            return true;
//        }
//
//        return false;
    }
}
