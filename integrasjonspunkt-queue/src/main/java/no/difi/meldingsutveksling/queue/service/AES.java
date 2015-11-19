package no.difi.meldingsutveksling.queue.service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AES {
    public static byte[] encrypt(final byte[] keyBytes, final byte[] ivBytes, final byte[] messageBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        return AES.transform(Cipher.ENCRYPT_MODE, keyBytes, ivBytes, messageBytes);
    }

    public static byte[] decrypt(final byte[] keyBytes, final byte[] ivBytes, final byte[] messageBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        return AES.transform(Cipher.DECRYPT_MODE, keyBytes, ivBytes, messageBytes);
    }

    private static byte[] transform(final int mode, final byte[] keyBytes, final byte[] ivBytes, final byte[] messageBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        final SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        final IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        cipher.init(mode, keySpec, ivSpec);

        return cipher.doFinal(messageBytes);
    }
}