package no.difi.meldingsutveksling.dokumentpakking.service;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import no.difi.meldingsutveksling.dokumentpakking.kvit.*;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Sertifikat;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.*;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by kubkaray on 08.12.2014.
 */
public class SignAFile {


    public Kvittering signIt(Object obj, Avsender avsender, KvitteringType kvitType) {
        PrivateKey privateKey;
        privateKey = avsender.getNoekkelpar().getPrivateKey();

        Signature rsa;

        byte[] signedBytes;
        Sertifikat certificate;

        try {
            certificate = avsender.getNoekkelpar().getSertifikat();
            rsa = Signature.getInstance("SHA256withRSA");
            rsa.initSign(privateKey);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(obj);
            rsa.update(b.toByteArray());
            signedBytes = rsa.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | IOException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }

        Kvittering kvittering = new Kvittering();
        if (kvitType.equals(KvitteringType.LEVERING)) {
            kvittering.setLevering(new Levering());
        } else {
            kvittering.setAapning(new Aapning());
        }

        //************************SIGNATURE ELEMENT************************************
        SignatureType signature = new SignatureType();
        //************************CHILDS OF SIGNATURE*********************************
        ///1. child
        SignedInfoType signedInfoType = new SignedInfoType();
        CanonicalizationMethodType canonicalizationMethodType = new CanonicalizationMethodType();
        canonicalizationMethodType.setAlgorithm("http://www.w3.org/2001/10/xml-exc-c14n#");
        no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureMethodType signatureMethodType = new no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureMethodType();
        signatureMethodType.setAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        signedInfoType.setCanonicalizationMethod(canonicalizationMethodType);
        signedInfoType.setSignatureMethod(signatureMethodType);
        ReferenceType referenceType = new ReferenceType();
        TransformsType transformsType = new TransformsType();
        TransformType transformType = new TransformType();
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
        String signatureValue = DatatypeConverter.printBase64Binary(signedBytes);
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

        return kvittering;
    }

}
