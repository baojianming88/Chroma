package com.zt.chroma_v2.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Tanlang
 * @create 2022-05-14-12:09
 */
public class AES {

    public static String iv = "iviviviviviviviv";
    public static String password = "zhangtan";

    public AES(){};

    /**
     * GCM 模式
     *
     * */
    public static Cipher initAESCipherGCM(String passsword, int cipherMode) {
        Cipher cipher = null;
        try {
            SecretKey key = getKey(passsword);
            cipher = Cipher.getInstance("AES/GCM/NoPadding"); // AES/GCM/PKCS5Padding 失效
            cipher.init(cipherMode, key,new GCMParameterSpec(128, iv.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InvalidKeyException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return cipher;
    }

    public static void encryptionStreamGCM(String plainTxt,String cipherTxt) {
        File inputFile = new File(plainTxt);
        File outputFile = new File(cipherTxt);
        CipherOutputStream cipherOutputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            if (!inputFile.exists()){
                System.out.println("The file is not exit!");;
            }
            Cipher cipher = initAESCipherGCM(password,Cipher.ENCRYPT_MODE);
            cipherOutputStream = new CipherOutputStream(new FileOutputStream(outputFile), cipher);
            bufferedInputStream = new BufferedInputStream(new FileInputStream(inputFile));
            byte[] buffer = new byte[1024];
            int bufferLength;
            while ((bufferLength = bufferedInputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bufferLength);
            }
            bufferedInputStream.close();
            cipherOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
     }

    public static String decryptionStreamGCM(InputStream inputStream){
        CipherInputStream cipherInputStream = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            Cipher cipher = initAESCipherGCM(password,Cipher.DECRYPT_MODE);
            cipherInputStream = new CipherInputStream(inputStream, cipher);
            int bufferLength;
            byte[] buffer = new byte[1024];
            while ((bufferLength = cipherInputStream.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer,0,bufferLength));
            }
            cipherInputStream.close();
            return new String(stringBuilder);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 通过password获取 key
     * @param password
     * @return
     */
    private static SecretKey getKey(String password) {
        int keyLength = 128;
        byte[] keyBytes = new byte[keyLength / 8];
        SecretKeySpec key = null;
        try {
            Arrays.fill(keyBytes, (byte) 0x0);
            byte[] passwordBytes = password.getBytes("UTF-8");
            int length = passwordBytes.length < keyBytes.length ? passwordBytes.length : keyBytes.length;
            System.arraycopy(passwordBytes, 0, keyBytes, 0, length);
            key = new SecretKeySpec(keyBytes, "AES");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return key;
    }

}
