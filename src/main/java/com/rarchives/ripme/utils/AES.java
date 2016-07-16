package com.rarchives.ripme.utils;

import org.apache.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.util.Arrays;

public class AES {

    protected static final Logger LOGGER = Logger.getLogger(AES.class);

    /**
     * Hack to get JCE Unlimited Strenght so we can use weird AES encryption stuff.
     * From http://stackoverflow.com/a/20286961
     */
    static {
        try {
            Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
            if (!field.isAccessible()) {
                field.setAccessible(true);
                field.set(null, java.lang.Boolean.FALSE);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private AES() {
    }

    public static String decrypt(String cipherText, String key, int nBits) throws Exception {
        String res;
        nBits = nBits / 8;

        byte[] data = Base64.decode(cipherText);
        byte[] k = Arrays.copyOf(key.getBytes(), nBits);

        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        SecretKey secretKey = generateSecretKey(k, nBits);

        byte[] nonceBytes = Arrays.copyOf(Arrays.copyOf(data, 8), nBits / 2);
        IvParameterSpec nonce = new IvParameterSpec(nonceBytes);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, nonce);
        res = new String(cipher.doFinal(data, 8, data.length - 8));
        return res;
    }

    private static SecretKey generateSecretKey(byte[] keyBytes, int nBits) throws Exception {
        try {
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            keyBytes = cipher.doFinal(keyBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        System.arraycopy(keyBytes, 0, keyBytes, nBits / 2, nBits / 2);
        return new SecretKeySpec(keyBytes, "AES");
    }
}