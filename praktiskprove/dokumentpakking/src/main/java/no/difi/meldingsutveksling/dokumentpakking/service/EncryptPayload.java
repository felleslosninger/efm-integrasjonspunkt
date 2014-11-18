package no.difi.meldingsutveksling.dokumentpakking.service;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

import no.difi.meldingsutveksling.dokumentpakking.domain.EncryptedContent;
import no.difi.meldingsutveksling.domain.Mottaker;

public class EncryptPayload {

	public EncryptedContent encrypt(byte[] payload, Mottaker mottaker) {
		try {
			Cipher keyCipher = Cipher.getInstance("RSA");
			Cipher contentCipher = Cipher.getInstance("AES");
			Key contentKey = generateKey();
			
			keyCipher.init(Cipher.ENCRYPT_MODE, mottaker.getPublicKey());
			contentCipher.init(Cipher.ENCRYPT_MODE, contentKey);
			
			byte[] encryptedContentKey = keyCipher.doFinal(contentKey.getEncoded());
			byte[] encryptedContent = contentCipher.doFinal(payload);
			
			return new EncryptedContent(encryptedContentKey, encryptedContent);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private Key generateKey() throws NoSuchAlgorithmException{
	      KeyGenerator kg = KeyGenerator.getInstance("AES");
	      SecureRandom random = new SecureRandom();
	      kg.init(random);
	      return kg.generateKey();
	}
}
