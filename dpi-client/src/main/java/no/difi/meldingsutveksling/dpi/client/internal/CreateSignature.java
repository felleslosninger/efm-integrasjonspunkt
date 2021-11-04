package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.dpi.client.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dpi.client.domain.KeyPair;
import no.difi.meldingsutveksling.dpi.client.internal.domain.Signature;
import no.difi.meldingsutveksling.dpi.client.internal.domain.XAdESArtifacts;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.codec.digest.DigestUtils.sha256;

@Slf4j
public class CreateSignature {

    private static final String C14V1 = CanonicalizationMethod.INCLUSIVE;
    private static final String ASIC_NAMESPACE = "http://uri.etsi.org/2918/v1.2.1#";
    private static final String SIGNED_PROPERTIES_TYPE = "http://uri.etsi.org/01903#SignedProperties";

    private final DigestMethod sha256DigestMethod;
    private final CanonicalizationMethod canonicalizationMethod;
    private final Transform canonicalXmlTransform;

    private final DomUtils domUtils;
    private final CreateXAdESArtifacts createXAdESArtifacts;
    private final KeyPair keyPair;

    public CreateSignature(DomUtils domUtils, CreateXAdESArtifacts createXAdESArtifacts, KeyPair keyPair) {
        this.domUtils = domUtils;
        this.createXAdESArtifacts = createXAdESArtifacts;
        this.keyPair = keyPair;

        try {
            XMLSignatureFactory xmlSignatureFactory = getSignatureFactory();
            this.sha256DigestMethod = xmlSignatureFactory.newDigestMethod(DigestMethod.SHA256, null);
            this.canonicalizationMethod = xmlSignatureFactory.newCanonicalizationMethod(C14V1, (C14NMethodParameterSpec) null);
            this.canonicalXmlTransform = xmlSignatureFactory.newTransform(C14V1, (TransformParameterSpec) null);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new Exception("Kunne ikke initialisere xml-signering", e);
        }
    }

    public Signature createSignature(final List<AsicEAttachable> attachedFiles) {
        log.info("Signing ASiC-E documents using private key with alias " + keyPair.getAlias());

        XMLSignatureFactory xmlSignatureFactory = getSignatureFactory();
        SignatureMethod signatureMethod = getSignatureMethod(xmlSignatureFactory);

        // Generer XAdES-dokument som skal signeres, informasjon om nøkkel brukt til signering og informasjon om hva som er signert
        XAdESArtifacts xadesArtifacts = createXAdESArtifacts.createArtifactsToSign(attachedFiles, keyPair.getBusinessCertificate());

        // Lag signatur-referanse for alle filer
        List<Reference> references = references(xmlSignatureFactory, attachedFiles);

        // Lag signatur-referanse for XaDES properties
        references.add(xmlSignatureFactory.newReference(
                xadesArtifacts.getSignablePropertiesReferenceUri(),
                sha256DigestMethod,
                singletonList(canonicalXmlTransform),
                SIGNED_PROPERTIES_TYPE,
                null
        ));


        KeyInfo keyInfo = keyInfo(xmlSignatureFactory, keyPair.getBusinessCertificateChain());
        SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(canonicalizationMethod, signatureMethod, references);

        // Definer signatur over XAdES-dokument
        XMLObject xmlObject = xmlSignatureFactory.newXMLObject(singletonList(new DOMStructure(xadesArtifacts.getDocument().getDocumentElement())), null, null, null);
        XMLSignature xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo, singletonList(xmlObject), "Signature", null);

        Document signedDocument = domUtils.newEmptyXmlDocument();
        DOMSignContext signContext = new DOMSignContext(keyPair.getBusinessCertificatePrivateKey(), addXAdESSignaturesElement(signedDocument));
        signContext.setURIDereferencer(signedPropertiesURIDereferencer(xadesArtifacts, xmlSignatureFactory));

        try {
            xmlSignature.sign(signContext);
        } catch (MarshalException e) {
            throw new Exception("Klarte ikke å lese ASiC-E XML for signering", e);
        } catch (XMLSignatureException e) {
            throw new Exception("Klarte ikke å signere ASiC-E element.", e);
        }

        return new Signature(domUtils.serializeToXml(signedDocument));
    }

    private URIDereferencer signedPropertiesURIDereferencer(XAdESArtifacts xadesArtifacts, XMLSignatureFactory signatureFactory) {
        return (uriReference, context) -> {
            if (xadesArtifacts.getSignablePropertiesReferenceUri().equals(uriReference.getURI())) {
                return (NodeSetData) domUtils.allNodesBelow(xadesArtifacts.getSignableProperties())::iterator;
            }
            return signatureFactory.getURIDereferencer().dereference(uriReference, context);
        };
    }

    private static Element addXAdESSignaturesElement(Document doc) {
        return (Element) doc.appendChild(doc.createElementNS(ASIC_NAMESPACE, "XAdESSignatures"));
    }

    private static SignatureMethod getSignatureMethod(final XMLSignatureFactory xmlSignatureFactory) {
        try {
            return xmlSignatureFactory.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new Exception("Kunne ikke initialisere xml-signering", e);
        }
    }

    private List<Reference> references(final XMLSignatureFactory xmlSignatureFactory, final List<AsicEAttachable> files) {
        List<Reference> result = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            try {
                result.add(xmlSignatureFactory.newReference(
                        URLEncoder.encode(files.get(i).getFilename(), "UTF-8"),
                        sha256DigestMethod,
                        null,
                        null,
                        "ID_" + i,
                        sha256(files.get(i).getResource().getInputStream())));
            } catch (IOException e) {
                throw new Exception("Failed to get references", e);
            }

        }
        return result;
    }

    private static KeyInfo keyInfo(final XMLSignatureFactory xmlSignatureFactory, final Certificate[] sertifikater) {
        KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
        X509Data x509Data = keyInfoFactory.newX509Data(asList(sertifikater));
        return keyInfoFactory.newKeyInfo(singletonList(x509Data));
    }

    private static XMLSignatureFactory getSignatureFactory() {
        try {
            return XMLSignatureFactory.getInstance("DOM", "XMLDSig");
        } catch (NoSuchProviderException e) {
            throw new Exception("Fant ikke XML Digital Signature-provider. Biblioteket avhenger av default Java-provider.");
        }
    }

    private static class Exception extends RuntimeException {
        public Exception(String message) {
            super(message);
        }

        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
