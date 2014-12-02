/**
 * Copyright (C) Posten Norge AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.difi.meldingsutveksling.dokumentpakking.service;

import static java.lang.String.format;
import static org.apache.commons.codec.digest.DigestUtils.sha1;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.dom.DOMResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import no.difi.meldingsutveksling.dokumentpakking.xades.CertIDListType;
import no.difi.meldingsutveksling.dokumentpakking.xades.CertIDType;
import no.difi.meldingsutveksling.dokumentpakking.xades.DataObjectFormatType;
import no.difi.meldingsutveksling.dokumentpakking.xades.DigestAlgAndValueType;
import no.difi.meldingsutveksling.dokumentpakking.xades.DigestMethodType;
import no.difi.meldingsutveksling.dokumentpakking.xades.ObjectFactory;
import no.difi.meldingsutveksling.dokumentpakking.xades.QualifyingPropertiesType;
import no.difi.meldingsutveksling.dokumentpakking.xades.SignedDataObjectPropertiesType;
import no.difi.meldingsutveksling.dokumentpakking.xades.SignedPropertiesType;
import no.difi.meldingsutveksling.dokumentpakking.xades.SignedSignaturePropertiesType;
import no.difi.meldingsutveksling.dokumentpakking.xades.X509IssuerSerialType;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Sertifikat;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CreateXAdESProperties {

	private final DigestMethodType sha1DigestMethod;
	private static Jaxb2Marshaller marshaller;

	static {
		marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(QualifyingPropertiesType.class);
	}

	public CreateXAdESProperties() {
		sha1DigestMethod = new DigestMethodType();
		sha1DigestMethod.setAlgorithm(DigestMethod.SHA1);
	}

	public Document createPropertiesToSign(List<ByteArrayFile> files, Sertifikat sertifikat) {
		X509Certificate certificate = sertifikat.getX509Certificate();
		byte[] certificateDigestValue = sha1(sertifikat.getEncoded());

		DigestAlgAndValueType certificateDigest = new DigestAlgAndValueType();
		certificateDigest.setDigestMethod(sha1DigestMethod);
		certificateDigest.setDigestValue(certificateDigestValue);

		X509IssuerSerialType certificateIssuer = new X509IssuerSerialType();
		certificateIssuer.setX509IssuerName(certificate.getIssuerDN().getName());
		certificateIssuer.setX509SerialNumber(certificate.getSerialNumber());

		CertIDListType signingCertificate = new CertIDListType();
		CertIDType certIDType = new CertIDType();
		certIDType.setCertDigest(certificateDigest);
		certIDType.setIssuerSerial(certificateIssuer);
		signingCertificate.getCert().add(certIDType);

		SignedSignaturePropertiesType signedSignatureProperties = new SignedSignaturePropertiesType();

		GregorianCalendar gCal = new GregorianCalendar();
		gCal.setTime(new Date());
		XMLGregorianCalendar xmlDate;
		try {
			xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCal);
			signedSignatureProperties.setSigningTime(xmlDate);
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}

		signedSignatureProperties.setSigningCertificate(signingCertificate);

		SignedDataObjectPropertiesType signedDataObjectProperties = new SignedDataObjectPropertiesType();
		signedDataObjectProperties.getDataObjectFormat().addAll(dataObjectFormats(files));
		SignedPropertiesType signedProperties = new SignedPropertiesType();
		signedProperties.setSignedSignatureProperties(signedSignatureProperties);
		signedProperties.setSignedDataObjectProperties(signedDataObjectProperties);
		signedProperties.setId("SignedProperties");
		QualifyingPropertiesType qualifyingProperties = new QualifyingPropertiesType();
		qualifyingProperties.setSignedProperties(signedProperties);
		qualifyingProperties.setTarget("#Signature");

		DOMResult domResult = new DOMResult();
		marshaller.marshal(new ObjectFactory().createQualifyingProperties(qualifyingProperties), domResult);
		Document document = (Document) domResult.getNode();

		// Explicitly mark the SignedProperties Id as an Document ID attribute,
		// so that it will be eligble as a reference for signature.
		// If not, it will not be treated as something to sign.
		markAsIdProperty(document, "SignedProperties", "Id");

		return document;
	}

	private List<DataObjectFormatType> dataObjectFormats(List<ByteArrayFile> files) {
		List<DataObjectFormatType> result = new ArrayList<DataObjectFormatType>();
		for (int i = 0; i < files.size(); i++) {
			String signatureElementIdReference = format("#ID_%s", i);
			DataObjectFormatType obj = new DataObjectFormatType();
			obj.setMimeType(files.get(i).getMimeType());
			obj.setObjectReference(signatureElementIdReference);
			result.add(obj);
		}
		return result;
	}

	private void markAsIdProperty(Document document, final String elementName, String property) {
		XPath xPath = XPathFactory.newInstance().newXPath();
		try {
			Element idElement = (Element) xPath.evaluate("//*[local-name()='" + elementName + "']", document, XPathConstants.NODE);
			idElement.setIdAttribute(property, true);

		} catch (XPathExpressionException e) {
			throw new RuntimeException("XPath på generert XML feilet.", e);
		}
	}
}
