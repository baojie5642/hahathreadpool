package com.baojie.zk.example.security;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

public class TestAes {

    private static final String DEFAULT_CODING = "utf-8";
    private static final String MD5 = "MD5";
    private static final String AES = "AES";
    private static final String BLANK = "";

    public static String decrypt(String encrypted, String seed) {
        if (null == encrypted || null == seed) {
            return BLANK;
        }
        byte[] keyb = seed2Byte(seed);
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
        Cipher dcipher = getAndInit(AES, skey);
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

    private static byte[] seed2Byte(String seed) {
        try {
            return seed.getBytes(DEFAULT_CODING);
        } catch (Throwable t) {

        }
        return null;
    }

    private static MessageDigest getDigest(String tp) {
        try {
            return MessageDigest.getInstance(tp);
        } catch (Throwable t) {

        }
        return null;
    }

    private static Cipher getAndInit(String tp, SecretKeySpec skey) {
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

    private static Cipher getCipher(String tp) {
        try {
            return Cipher.getInstance(tp);
        } catch (Throwable t) {

        }
        return null;
    }

    private static byte[] doFinal(String encrypted, Cipher dcipher) {
        try {
            return dcipher.doFinal(str2Byte(encrypted));
        } catch (Throwable t) {

        }
        return null;
    }

    public static String encrypt(String content, String key) throws Exception {
        byte[] input = content.getBytes(DEFAULT_CODING);

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] thedigest = md.digest(key.getBytes(DEFAULT_CODING));
        SecretKeySpec skc = new SecretKeySpec(thedigest, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skc);

        byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
        int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
        ctLength += cipher.doFinal(cipherText, ctLength);
        return byte2Str(cipherText);
    }

    public static void main(String[] args)  {

    }


    private static byte[] str2Byte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
        }
        return result;
    }

    private static String byte2Str(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex);
        }
        return sb.toString();
    }

//	public static void main(String[] args) throws Exception {
//		System.out.println(AESForNodejs.encrypt("18918912192", "alipay")); //b101d8d30aaa8dcbf9b57ee173cf8d3c
//
//	}

}
