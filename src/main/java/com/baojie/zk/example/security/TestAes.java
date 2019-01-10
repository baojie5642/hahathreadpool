package com.baojie.zk.example.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

public class TestAes {

    private static final String DEFAULT_CODING = "utf-8";
    private static final String MD5 = "MD5";
    private static final String AES = "AES";
    private static final String PKCS = "AES/ECB/PKCS5Padding";
    private static final String BLANK = "";

    public String decrypt(String encrypted, String seed) {
        if (null == encrypted || null == seed) {
            return BLANK;
        }
        byte[] keyb = getStrByte(seed);
        if (null == keyb) {
            return BLANK;
        }
        MessageDigest md = getDigest(MD5);
        if (null == md) {
            return BLANK;
        }
        byte[] digest = md.digest(keyb);
        if (null == digest) {
            return BLANK;
        }
        SecretKeySpec skey = new SecretKeySpec(digest, AES);
        Cipher dcipher = getDecpMode(AES, skey);
        if (null == dcipher) {
            return BLANK;
        }
        byte[] clearbyte = doFinal(encrypted, dcipher);
        if (null == clearbyte) {
            return BLANK;
        } else {
            return new String(clearbyte);
        }
    }

    private byte[] getStrByte(String str) {
        try {
            return str.getBytes(DEFAULT_CODING);
        } catch (Throwable t) {

        }
        return null;
    }

    private MessageDigest getDigest(String tp) {
        try {
            return MessageDigest.getInstance(tp);
        } catch (Throwable t) {

        }
        return null;
    }

    private Cipher getDecpMode(String tp, SecretKeySpec skey) {
        Cipher dcipher = getCipher(tp);
        if (null == dcipher) {
            return null;
        } else {
            try {
                dcipher.init(Cipher.DECRYPT_MODE, skey);
            } catch (Throwable t) {
                return null;
            }
            return dcipher;
        }
    }

    private Cipher getEncrMode(String tp, SecretKeySpec skey) {
        Cipher dcipher = getCipher(tp);
        if (null == dcipher) {
            return null;
        } else {
            try {
                dcipher.init(Cipher.ENCRYPT_MODE, skey);
            } catch (Throwable t) {
                return null;
            }
            return dcipher;
        }
    }

    private Cipher getCipher(String tp) {
        try {
            return Cipher.getInstance(tp);
        } catch (Throwable t) {

        }
        return null;
    }

    private byte[] doFinal(String encrypted, Cipher dcipher) {
        try {
            return dcipher.doFinal(str2Byte(encrypted));
        } catch (Throwable t) {

        }
        return null;
    }

    public String encrypt(String content, String key) {
        if (null == content || null == key) {
            return BLANK;
        }
        byte[] input = getStrByte(content);
        if (null == input) {
            return BLANK;
        }
        MessageDigest md = getDigest(MD5);
        byte[] kbs = getStrByte(key);
        if (null == kbs) {
            return BLANK;
        }
        byte[] digest = md.digest(kbs);
        SecretKeySpec skc = new SecretKeySpec(digest, AES);
        Cipher ecipher = getEncrMode(PKCS, skc);
        if (null == ecipher) {
            return BLANK;
        }
        byte[] cipherByte = new byte[ecipher.getOutputSize(input.length)];


        int ctLength = encrUpdate(input, cipherByte, ecipher);
        if (0 > ctLength) {
            return BLANK;
        }
        ctLength += encrDoFinal(cipherByte, ctLength, ecipher);
        //debug
        return byte2Str(cipherByte);
    }

    private int encrUpdate(byte[] input, byte[] cipherByte, Cipher ecipher) {
        try {
            return ecipher.update(input, 0, input.length, cipherByte, 0);
        } catch (Throwable t) {

        }
        return -1;
    }


    private int encrDoFinal(byte[] cipherByte, int ctLength, Cipher ecipher) {
        try {
            return ecipher.doFinal(cipherByte, ctLength);
        } catch (Throwable t) {

        }
        return -1;
    }

    private byte[] str2Byte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
        }
        return result;
    }

    private String byte2Str(byte buf[]) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex);
        }
        return sb.toString();
    }

}
