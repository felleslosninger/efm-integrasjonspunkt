package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Sertifikat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;

/**
 * Created by kubkaray on 08.12.2014.
 */
public class SignAFile {

    public SignAFile(File file,Avsender avsender) {

        signIt(file,avsender);
    }

    private void signIt(File file,Avsender avsender) {
        PrivateKey privateKey;
        PublicKey publicKey;
        privateKey = avsender.getNoekkelpar().getPrivateKey();
        RSAPrivateCrtKey privk = (RSAPrivateCrtKey)privateKey;
        RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(privk.getPublicExponent(), privk.getModulus());
        KeyFactory keyFactory ;
        PublicKey myPublicKey;
        Signature rsa;
        BufferedInputStream bufin;

        try {
            keyFactory = KeyFactory.getInstance("RSA");
            Sertifikat certificate= avsender.getNoekkelpar().getSertifikat();
            Certificate certificate1=certificate.getX509Certificate();
            myPublicKey=certificate1.getPublicKey();
            rsa = Signature.getInstance("SHA1withRSA");
            rsa.initSign(privateKey);
            FileInputStream fis = new FileInputStream(file);
             bufin = new BufferedInputStream(fis);
            byte[] signedBytes;
            byte[] buffer = new byte[1024];
            int len;
            while (bufin.available() !=0) {
                len = bufin.read(buffer);
                rsa.update(buffer,0,len);
            }
            bufin.close();
            signedBytes = rsa.sign();
            FileOutputStream fileOutputStream = new FileOutputStream(new File(System.getProperty("user.home")+File.separator+"testToRemove"+
            File.separator + "signedXml.xml"));
            fileOutputStream.write(signedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }


}
