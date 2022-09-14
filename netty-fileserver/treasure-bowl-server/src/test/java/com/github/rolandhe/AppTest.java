package com.github.rolandhe;

import static org.junit.Assert.assertTrue;

import com.github.rolandhe.file.def.auth.RsaAuth;
import com.github.rolandhe.file.def.auth.SecurityUtils;
import com.github.rolandhe.file.json.JsonHelper;
import org.junit.Test;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;

/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void test() {
        PrivateKey key = SecurityUtils.readPemPrivateKey(new File("/Users/hexiufeng/github/netty-fileserver/treasure-bowl-server/src/main/resources/rsa/private_key.pem"));
        RsaAuth auth = new RsaAuth();
        auth.setBizLine("data");
        auth.setUserId(101L);
        auth.setExpiredAt(LocalDateTime.MAX);

        String json = JsonHelper.toJson(auth);

        String enData = SecurityUtils.rsaEncryptString(key, json);

        PublicKey publicKey = SecurityUtils.readPemPublicKey(new File("/Users/hexiufeng/github/netty-fileserver/treasure-bowl-server/src/main/resources/rsa/public_key.pem"));

        String deData = SecurityUtils.rsaDecryptString(publicKey,enData);
        System.out.println(enData);
    }
}
