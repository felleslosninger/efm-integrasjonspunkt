package no.difi.meldingsutveksling.dokumentpakking.service;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import no.difi.meldingsutveksling.dokumentpakking.asice.CanonicalizationMethodType;
import no.difi.meldingsutveksling.dokumentpakking.asice.SignedInfoType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.DigestMethodType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.KeyInfoType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.Kvittering;
import no.difi.meldingsutveksling.dokumentpakking.kvit.Levering;
import no.difi.meldingsutveksling.dokumentpakking.kvit.ReferenceType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureValueType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.TransformsType;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Sertifikat;
import org.apache.commons.io.FileUtils;

import javax.xml.datatype.XMLGregorianCalendar;
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
import java.util.Date;
import java.util.GregorianCalendar;

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
        byte[] signedBytes;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            Sertifikat certificate= avsender.getNoekkelpar().getSertifikat();
            Certificate certificate1=certificate.getX509Certificate();
            myPublicKey=certificate1.getPublicKey();
            rsa = Signature.getInstance("SHA1withRSA");
            rsa.initSign(privateKey);
            FileInputStream fis = new FileInputStream(file);
             bufin = new BufferedInputStream(fis);

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
            FileUtils.writeByteArrayToFile(file, signedBytes);
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

        Kvittering kvittering = new Kvittering();
        Levering levering = new Levering();

        kvittering.setLevering(new Levering());

        //************************SIGNATURE ELEMENT************************************
        SignatureType signature = new SignatureType();
        //************************CHILDS OF SIGNATURE*********************************
        SignedInfoType signedInfoType= new SignedInfoType();
        SignatureValueType signatureValueType = new SignatureValueType();
        KeyInfoType keyInfoType = new KeyInfoType();

        CanonicalizationMethodType canonicalizationMethodType = new CanonicalizationMethodType();
        canonicalizationMethodType.setAlgorithm("http://www.w3.org/2001/10/xml-exc-c14n#");
        no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureMethodType signatureMethodType = new no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureMethodType();
        signatureMethodType.setAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        signedInfoType.setCanonicalizationMethod(canonicalizationMethodType);
        ReferenceType referenceType = new ReferenceType();
        referenceType.setTransforms(new TransformsType());
        DigestMethodType digestMethodType = new DigestMethodType();
        digestMethodType.setAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
        System.out.println();


        //************************TIDSPUNKT ELEMENT************************************
        GregorianCalendar gCal = new GregorianCalendar();
        gCal.setTime(new Date());
        XMLGregorianCalendar xmlGeorgianCalendar = new XMLGregorianCalendarImpl(gCal);
        kvittering.setTidspunkt(xmlGeorgianCalendar);

    }



}
