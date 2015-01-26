package no.difi.meldingsutveksling.dokumentpakking;

import no.difi.meldingsutveksling.dokumentpakking.kvit.*;
import no.difi.meldingsutveksling.dokumentpakking.service.*;
import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBElement;
import java.io.ByteArrayOutputStream;
import java.security.*;

public class Dokumentpakker {

    private CmsUtil encryptPayload;
    private CreateSBD createSBD;
    private CreateAsice createAsice;

    public Dokumentpakker() {
        createSBD = new CreateSBD();
        encryptPayload = new CmsUtil();
        createAsice = new CreateAsice(new CreateSignature(), new CreateZip(), new CreateManifest());
    }

    public byte[] pakkTilByteArray(ByteArrayFile document, Avsender avsender, Mottaker mottaker, String id, String type) {
        StandardBusinessDocument doc = pakk(document, avsender, mottaker, id, type);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MarshalSBD.marshal(doc, os);
        return os.toByteArray();
    }

    public StandardBusinessDocument pakk(ByteArrayFile document, Avsender avsender, Mottaker mottaker,
                                         String id, String type) {
        Payload payload = new Payload(encryptPayload.createCMS(createAsice.createAsice(document, avsender, mottaker).getBytes(), mottaker.getSertifikat()));
        payload.setSignature(createXmlSignature(createRsaSignature(avsender.getNoekkelpar().getPrivateKey(), payload.getContent().getBytes()), avsender
                .getNoekkelpar().getSertifikat().getEncoded()));
        return createSBD.createSBD(avsender.getOrgNummer(), mottaker.getOrgNummer(), payload, id, type);
    }

    private byte[] createRsaSignature(PrivateKey privateKey, byte[] contentToBeSigned) {
        try {
            Signature rsaSignature;
            rsaSignature = Signature.getInstance("SHA256withRSA");
            rsaSignature.initSign(privateKey);

            rsaSignature.update(contentToBeSigned);
            return rsaSignature.sign();

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    private SignatureType createXmlSignature(byte[] signedBytes, byte[] x509EncodedCert) {

        CanonicalizationMethodType canonicalizationMethodType = new CanonicalizationMethodType();
        canonicalizationMethodType.setAlgorithm("http://www.w3.org/2001/10/xml-exc-c14n#");
        SignatureMethodType signatureMethodType = new SignatureMethodType();
        signatureMethodType.setAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        SignedInfoType signedInfoType = new SignedInfoType();
        signedInfoType.setCanonicalizationMethod(canonicalizationMethodType);
        signedInfoType.setSignatureMethod(signatureMethodType);

        TransformsType transformsType = new TransformsType();
        TransformType transformType = new TransformType();
        transformType.setAlgorithm("http://www.w3.org/2000/09/xmldsig#enveloped-signature");
        transformsType.getTransform().add(transformType);

        ReferenceType referenceType = new ReferenceType();
        referenceType.setTransforms(transformsType);
        DigestMethodType digestMethodType = new DigestMethodType();
        digestMethodType.setAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
        referenceType.setDigestMethod(digestMethodType);
        referenceType.setDigestValue(signedBytes);
        signedInfoType.getReference().add(referenceType);

        SignatureValueType signatureValueType = new SignatureValueType();
        String signatureValue = DatatypeConverter.printBase64Binary(signedBytes);
        signatureValueType.setValue(signatureValue.getBytes());
        KeyInfoType keyInfoType = new KeyInfoType();


        X509DataType x509DataType = new ObjectFactory().createX509DataType();
        JAXBElement<X509DataType> x509Data = new ObjectFactory().createX509Data(x509DataType);

        JAXBElement<byte[]> resultCert = new ObjectFactory().createX509DataTypeX509Certificate(x509EncodedCert);
        x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(resultCert);
        keyInfoType.getContent().add(x509Data);

        SignatureType signature = new SignatureType();
        signature.setSignedInfo(signedInfoType);
        signature.setKeyInfo(keyInfoType);
        signature.setSignatureValue(signatureValueType);
        return signature;
    }
}
