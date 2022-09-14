package com.github.rolandhe.file.def.auth;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

@Slf4j
public class RsaSupport {
    protected static final PublicKey publicKey;
    protected static final String AUTH_TOKEN_NAME = "auth-token";

    static {
        try (InputStream inputStream = RsaTransferAuth.class.getClassLoader().getResourceAsStream("rsa/public_key.pem")) {
            publicKey = SecurityUtils.readPemPublicKey(inputStream);
        } catch (IOException e) {
            log.error("load public key error.", e);
            throw new RuntimeException(e);
        }
    }
}
