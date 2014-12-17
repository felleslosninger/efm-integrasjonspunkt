package no.difi.meldingsutveksling.dokumentpakking;

import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBElement;

import no.difi.meldingsutveksling.dokumentpakking.kvit.CanonicalizationMethodType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.DigestMethodType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.KeyInfoType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.ObjectFactory;
import no.difi.meldingsutveksling.dokumentpakking.kvit.ReferenceType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureMethodType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.SignatureValueType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.SignedInfoType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.TransformType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.TransformsType;
import no.difi.meldingsutveksling.dokumentpakking.kvit.X509DataType;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateManifest;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSignature;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateZip;
import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

public class Dokumentpakker {

	private CmsUtil encryptPayload;
	private CreateSBD createSBD;
	private CreateAsice createAsice;

	public Dokumentpakker() {
		createSBD = new CreateSBD();
		encryptPayload = new CmsUtil();
		createAsice = new CreateAsice(new CreateSignature(), new CreateZip(), new CreateManifest());
	}

	public byte[] pakkDokumentISbd(ByteArrayFile document, Avsender avsender, Mottaker mottaker, String conversationId, String type) {
		StandardBusinessDocument doc = pakkDokumentIStandardBusinessDocument(document, avsender, mottaker, conversationId, type);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalSBD.marshal(doc, os);
		return os.toByteArray();
	}

	public StandardBusinessDocument pakkDokumentIStandardBusinessDocument(ByteArrayFile document, Avsender avsender, Mottaker mottaker, String conversationId,
			String type) {
		Payload payload = new Payload(encryptPayload.createCMS(createAsice.createAsice(document, avsender, mottaker).getBytes(), mottaker.getSertifikat()));
		payload.setSignature(createXmlSignature(createRsaSignature(avsender.getNoekkelpar().getPrivateKey(), payload.getContent().getBytes()), avsender
				.getNoekkelpar().getSertifikat().getEncoded()));
		return createSBD.createSBD(avsender.getOrgNummer(), mottaker.getOrgNummer(), payload, conversationId, type);
	}

	private byte[] createRsaSignature(PrivateKey privateKey, byte[] contentToBeSigned) {
		try {
			Signature rsaSignature;
			rsaSignature = Signature.getInstance("SHA256withRSA");
			rsaSignature.initSign(privateKey);

			rsaSignature.update(contentToBeSigned);
			return rsaSignature.sign();

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		} catch (SignatureException e) {
			throw new RuntimeException(e);
		}
	}

	private SignatureType createXmlSignature(byte[] signedBytes, byte[] x509EncodedCert) {
		SignatureType signature = new SignatureType();
		SignedInfoType signedInfoType = new SignedInfoType();
		CanonicalizationMethodType canonicalizationMethodType = new CanonicalizationMethodType();
		canonicalizationMethodType.setAlgorithm("http://www.w3.org/2001/10/xml-exc-c14n#");
		SignatureMethodType signatureMethodType = new SignatureMethodType();
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

		SignatureValueType signatureValueType = new SignatureValueType();
		String signatureValue = DatatypeConverter.printBase64Binary(signedBytes);
		signatureValueType.setValue(signatureValue.getBytes());
		KeyInfoType keyInfoType = new KeyInfoType();

		X509DataType x509DataType = new ObjectFactory().createX509DataType();
		JAXBElement<byte[]> resultCert = new ObjectFactory().createX509DataTypeX509Certificate(x509EncodedCert);

		JAXBElement<X509DataType> x509Data = new ObjectFactory().createX509Data(x509DataType);

		x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(resultCert);
		keyInfoType.getContent().add(x509Data);
		signature.setSignedInfo(signedInfoType);
		signature.setKeyInfo(keyInfoType);
		signature.setSignatureValue(signatureValueType);
		return signature;
	}
}
