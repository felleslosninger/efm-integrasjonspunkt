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
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.codec.digest.DigestUtils.sha256;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;

import no.difi.meldingsutveksling.dokumentpakking.domain.Signature;
import no.difi.meldingsutveksling.dokumentpakking.xml.Constants;
import no.difi.meldingsutveksling.dokumentpakking.xml.Schemas;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Noekkelpar;

import org.springframework.core.io.Resource;
import org.springframework.xml.validation.SchemaLoaderUtils;
import org.springframework.xml.validation.XmlValidatorFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class CreateSignature {

	private static final String ASIC_NAMESPACE = "http://uri.etsi.org/2918/v1.2.1#";
	private static final String SIGNED_PROPERTIES_TYPE = "http://uri.etsi.org/01903#SignedProperties";

	private final DigestMethod sha256DigestMethod;
	private final CanonicalizationMethod canonicalizationMethod;
	private final Transform canonicalXmlTransform;

	private final CreateXAdESProperties createXAdESProperties;
	private final TransformerFactory transformerFactory;
	private final Schema schema;

	public CreateSignature() {
		createXAdESProperties = new CreateXAdESProperties();
		transformerFactory = TransformerFactory.newInstance();
		try {
			XMLSignatureFactory xmlSignatureFactory = getSignatureFactory();
			sha256DigestMethod = xmlSignatureFactory.newDigestMethod(DigestMethod.SHA256, null);
			canonicalizationMethod = xmlSignatureFactory.newCanonicalizationMethod(Constants.C14V1, (C14NMethodParameterSpec) null);
			canonicalXmlTransform = xmlSignatureFactory.newTransform(Constants.C14V1, (TransformParameterSpec) null);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Kunne ikke initialisere xml-signering", e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new RuntimeException("Kunne ikke initialisere xml-signering", e);
		}

		schema = loadSchema();
	}

	private Schema loadSchema() {
		try {
			return SchemaLoaderUtils.loadSchema(new Resource[] { Schemas.ASICE_SCHEMA }, XmlValidatorFactory.SCHEMA_W3C_XML);
		} catch (IOException e) {
			throw new RuntimeException("Kunne ikke laste schema for validering av signatures", e);
		} catch (SAXException e) {
			throw new RuntimeException("Kunne ikke laste schema for validering av signatures", e);
		}
	}

	public Signature createSignature(final Noekkelpar noekkelpar, final List<ByteArrayFile> attachedFiles) {
		XMLSignatureFactory xmlSignatureFactory = getSignatureFactory();
		SignatureMethod signatureMethod = getSignatureMethod(xmlSignatureFactory);

		// Lag signatur-referanse for alle filer
		List<Reference> references = references(xmlSignatureFactory, attachedFiles);

		// Lag signatur-referanse for XaDES properties
		references.add(xmlSignatureFactory.newReference("#SignedProperties", sha256DigestMethod, singletonList(canonicalXmlTransform), SIGNED_PROPERTIES_TYPE,
				null));

		// Generer XAdES-dokument som skal signeres, informasjon om nøkkel
		// brukt til signering og informasjon om hva som er signert
		Document document = createXAdESProperties.createPropertiesToSign(attachedFiles, noekkelpar.getSertifikat());

		KeyInfo keyInfo = keyInfo(xmlSignatureFactory, noekkelpar.getCertificateChain());
		SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(canonicalizationMethod, signatureMethod, references);

		// Definer signatur over XAdES-dokument
		XMLObject xmlObject = xmlSignatureFactory.newXMLObject(singletonList(new DOMStructure(document.getDocumentElement())), null, null, null);
		XMLSignature xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo, singletonList(xmlObject), "Signature", null);

		try {
			xmlSignature.sign(new DOMSignContext(noekkelpar.getPrivateKey(), document));
		} catch (MarshalException e) {
			throw new RuntimeException("Klarte ikke å lese ASiC-E XML for signering", e);
		} catch (XMLSignatureException e) {
			throw new RuntimeException("Klarte ikke å signere ASiC-E element.", e);
		}

		// Pakk Signatur inn i XAdES-konvolutt
		wrapSignatureInXADeSEnvelope(document);

		ByteArrayOutputStream outputStream;
		try {
			outputStream = new ByteArrayOutputStream();
			Transformer transformer = transformerFactory.newTransformer();
			schema.newValidator().validate(new DOMSource(document));
			transformer.transform(new DOMSource(document), new StreamResult(outputStream));
		} catch (TransformerException e) {
			throw new RuntimeException("Klarte ikke å serialisere XML", e);
		} catch (SAXException e) {
			throw new RuntimeException("Kunne ikke validere generert signatures.xml. Sjekk at input er gyldig og at det ikke er ugyldige tegn i filnavn o.l.",
					e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new Signature(outputStream.toByteArray());
	}

	private SignatureMethod getSignatureMethod(final XMLSignatureFactory xmlSignatureFactory) {
		try {
			return xmlSignatureFactory.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Kunne ikke initialisere xml-signering", e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new RuntimeException("Kunne ikke initialisere xml-signering", e);
		}
	}

	private List<Reference> references(final XMLSignatureFactory xmlSignatureFactory, final List<ByteArrayFile> files) {
		List<Reference> result = new ArrayList<Reference>();
		for (int i = 0; i < files.size(); i++) {
			try {
				String signatureElementId = format("ID_%s", i);
				String uri = URLEncoder.encode(files.get(i).getFileName(), "UTF-8");
				Reference reference = xmlSignatureFactory
						.newReference(uri, sha256DigestMethod, null, null, signatureElementId, sha256(files.get(i).getBytes()));
				result.add(reference);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}

		}
		return result;
	}

	private KeyInfo keyInfo(final XMLSignatureFactory xmlSignatureFactory, final Certificate[] sertifikater) {
		KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
		X509Data x509Data = keyInfoFactory.newX509Data(asList(sertifikater));
		return keyInfoFactory.newKeyInfo(singletonList(x509Data));
	}

	private void wrapSignatureInXADeSEnvelope(final Document document) {
		Node signatureElement = document.removeChild(document.getDocumentElement());
		Element xadesElement = document.createElementNS(ASIC_NAMESPACE, "XAdESSignatures");
		xadesElement.appendChild(signatureElement);
		document.appendChild(xadesElement);
	}

	private XMLSignatureFactory getSignatureFactory() {
		try {
			return XMLSignatureFactory.getInstance("DOM", "XMLDSig");
		} catch (NoSuchProviderException e) {
			throw new RuntimeException("Fant ikke XML Digital Signature-provider. Biblioteket avhenger av default Java-provider.");
		}
	}

}
