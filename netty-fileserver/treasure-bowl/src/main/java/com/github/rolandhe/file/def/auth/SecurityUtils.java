package com.github.rolandhe.file.def.auth;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

/**
 * 基于BouncyCastle封装的加解密工具，支持AES和RSA
 *
 * @author roland
 */
public class SecurityUtils {
    private SecurityUtils() {

    }

    static {
        Security.setProperty("crypto.policy", "unlimited");
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String genAESKey() {
        String key = UUID.randomUUID().toString().replace("-", "");
        return key;
    }

    public static String aesEncryptString(String key, String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");

            cipher.init(Cipher.ENCRYPT_MODE, defineAESKey(key));
            byte[] iv = cipher.getIV();
            byte[] encryptData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            String iv64 = Base64.encodeBase64String(iv);
            String data64 = Base64.encodeBase64String(encryptData);


            return iv64 + "@" + data64;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static String aesDecryptString(String key, String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");

            String[] array = StringUtils.split(cipherText, "@");

            cipher.init(Cipher.DECRYPT_MODE, defineAESKey(key), new IvParameterSpec(Base64.decodeBase64(array[0])));

            byte[] decryptedData = cipher.doFinal(Base64.decodeBase64(array[1]));

            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }


    public static String rsaEncryptString(Key key, String data) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA-256AndMGF1Padding", "BC");

            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encrypt = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64String(encrypt);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static String rsaDecryptString(Key key, String data) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA-256AndMGF1Padding", "BC");

            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypt = cipher.doFinal(Base64.decodeBase64(data));
            return new String(decrypt, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 读取rsa的pem格式公钥文件，通过
     * openssl rsa -in 1.private_key.pem -out 1.public_key.pem -pubout 由私钥文件生成公钥文件
     *
     * @param file
     * @return
     */
    public static RSAPublicKey readPemPublicKey(File file) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");

            try (FileReader keyReader = new FileReader(file);
                 PemReader pemReader = new PemReader(keyReader)) {

                PemObject pemObject = pemReader.readPemObject();
                byte[] content = pemObject.getContent();
                X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(content);
                return (RSAPublicKey) factory.generatePublic(pubKeySpec);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RSAPublicKey readPemPublicKey(InputStream inputStream) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");

            try (InputStreamReader keyReader = new InputStreamReader(inputStream,StandardCharsets.UTF_8);
                 PemReader pemReader = new PemReader(keyReader)) {

                PemObject pemObject = pemReader.readPemObject();
                byte[] content = pemObject.getContent();
                X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(content);
                return (RSAPublicKey) factory.generatePublic(pubKeySpec);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读取rsa的pem格式私钥文件，通过
     * openssl genrsa -out 1.private_key.pem 3072|2048 可以生成私钥文件
     *
     * @param file
     * @return
     */
    public static RSAPrivateKey readPemPrivateKey(File file) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");

            try (FileReader keyReader = new FileReader(file);
                 PemReader pemReader = new PemReader(keyReader)) {

                PemObject pemObject = pemReader.readPemObject();
                byte[] content = pemObject.getContent();
                PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
                return (RSAPrivateKey) factory.generatePrivate(privKeySpec);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static RSAPrivateKey readPemPrivateKey(InputStream inputStream) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");

            try (InputStreamReader keyReader = new InputStreamReader(inputStream,StandardCharsets.UTF_8);
                 PemReader pemReader = new PemReader(keyReader)) {

                PemObject pemObject = pemReader.readPemObject();
                byte[] content = pemObject.getContent();
                PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
                return (RSAPrivateKey) factory.generatePrivate(privKeySpec);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static SecretKey defineAESKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException("keyBytes wrong length for AES key");
        }

        return new SecretKeySpec(keyBytes, "AES");
    }


}
