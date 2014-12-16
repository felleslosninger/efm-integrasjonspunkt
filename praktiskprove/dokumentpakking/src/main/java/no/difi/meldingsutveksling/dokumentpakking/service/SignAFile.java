package no.difi.meldingsutveksling.dokumentpakking.service;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import no.difi.meldingsutveksling.dokumentpakking.kvit.Aapning;
import no.difi.meldingsutveksling.dokumentpakking.kvit.CanonicalizationMethodType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.DigestMethodType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.KeyInfoType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.Kvittering;
import no.difi.meldingsutveksling.dokumentpakking.kvit.Levering;
import no.difi.meldingsutveksling.dokumentpakking.kvit.ObjectFactory;
import no.difi.meldingsutveksling.dokumentpakking.kvit.ReferenceType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureValueType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.SignedInfoType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.TransformType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.TransformsType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.X509DataType;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Sertifikat;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by kubkaray on 08.12.2014.
 */
public class SignAFile {


    public Kvittering signIt(Object obj, Avsender avsender,KvitteringType kvitType) {
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
        Sertifikat certificate;
        X509Certificate x509Certificate;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            certificate= avsender.getNoekkelpar().getSertifikat();
             x509Certificate=certificate.getX509Certificate();
            myPublicKey=x509Certificate.getPublicKey();
            rsa = Signature.getInstance("SHA256withRSA");
            rsa.initSign(privateKey);

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(obj);
            rsa.update(b.toByteArray());
            signedBytes = rsa.sign();

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
        if (kvitType.equals(KvitteringType.LEVERING)) {
            kvittering.setLevering(new Levering());
        }else {
            kvittering.setAapning(new Aapning());
        }

        //************************SIGNATURE ELEMENT************************************
        SignatureType signature = new SignatureType();
        //************************CHILDS OF SIGNATURE*********************************
        ///1. child
        SignedInfoType signedInfoType= new SignedInfoType();
            CanonicalizationMethodType canonicalizationMethodType = new CanonicalizationMethodType();
            canonicalizationMethodType.setAlgorithm("http://www.w3.org/2001/10/xml-exc-c14n#");
            no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureMethodType signatureMethodType = new no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureMethodType();
            signatureMethodType.setAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
            signedInfoType.setCanonicalizationMethod(canonicalizationMethodType);
            signedInfoType.setSignatureMethod(signatureMethodType);
            ReferenceType referenceType = new ReferenceType();
            TransformsType transformsType= new TransformsType();
            TransformType transformType =new TransformType();
            transformType.setAlgorithm("http://www.w3.org/2000/09/xmldsig#enveloped-signature");
            transformsType.getTransform().add(transformType);
            referenceType.setTransforms(transformsType);
            DigestMethodType digestMethodType = new DigestMethodType();
            digestMethodType.setAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
            referenceType.setDigestMethod(digestMethodType);
            referenceType.setDigestValue(signedBytes);
            signedInfoType.getReference().add(referenceType);




        ///2.child
        SignatureValueType signatureValueType = new SignatureValueType();
            String signatureValue=DatatypeConverter.printBase64Binary(signedBytes);
            signatureValueType.setValue(signatureValue.getBytes());
        ///3.child
        KeyInfoType keyInfoType = new KeyInfoType();

        X509DataType x509DataType = new ObjectFactory().createX509DataType();
        JAXBElement<byte[]> resultCert = new ObjectFactory().createX509DataTypeX509Certificate(certificate.getEncoded());

        JAXBElement<X509DataType> x509Data = new ObjectFactory().createX509Data(x509DataType);

         x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(resultCert);
         keyInfoType.getContent().add(x509Data);
        signature.setSignedInfo(signedInfoType);
        signature.setKeyInfo(keyInfoType);
        signature.setSignatureValue(signatureValueType);
        kvittering.setSignature(signature);




        //************************TIDSPUNKT ELEMENT************************************
        GregorianCalendar gCal = new GregorianCalendar();
        gCal.setTime(new Date());
        XMLGregorianCalendar xmlGeorgianCalendar = new XMLGregorianCalendarImpl(gCal);
        kvittering.setTidspunkt(xmlGeorgianCalendar);

        //turn on while debugging
        JAXBContext jaxbContext ;
       /* try {
            jaxbContext = JAXBContext.newInstance(Kvittering.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(new ObjectFactory().createKvittering(kvittering) , System.out);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
*/

         return kvittering;
    }

}
