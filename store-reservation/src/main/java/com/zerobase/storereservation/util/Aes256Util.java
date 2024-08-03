package com.zerobase.storereservation.util;

import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class Aes256Util {
    // Claim에 id와 이메일 암호화해 저장
    public static String alg = "AES/CBC/PKCS5Padding";
    private static final String KEY = "ZEROBASEKEYISZEROBASEKEY";
    private static final String IV = KEY.substring(0,16);

    public static String encrypt(String text) {
        try {
            Cipher cipher = Cipher.getInstance(alg);
            SecretKeySpec key = new SecretKeySpec(KEY.getBytes(), "AES");
            IvParameterSpec iv = new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64String(encrypted);
        }catch (Exception e){
            return null;
        }
    }

    public static String decrypt(String cipherText) {
        try{
            Cipher cipher = Cipher.getInstance(alg);
            SecretKeySpec key = new SecretKeySpec(KEY.getBytes(), "AES");
            IvParameterSpec iv = new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, key, iv);

            byte[] decodeBytes = Base64.decodeBase64(cipherText);
            byte[] decrypted = cipher.doFinal(decodeBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        }catch (Exception e){
            return null;
        }
    }
}