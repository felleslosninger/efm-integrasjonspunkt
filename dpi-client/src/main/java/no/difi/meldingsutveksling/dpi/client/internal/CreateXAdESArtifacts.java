package no.difi.meldingsutveksling.dpi.client.internal;

import no.difi.commons.asic.jaxb.xades.*;
import no.difi.commons.asic.jaxb.xmldsig.DigestMethodType;
import no.difi.commons.asic.jaxb.xmldsig.X509IssuerSerialType;
import no.difi.meldingsutveksling.dpi.client.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dpi.client.domain.BusinessCertificate;
import no.difi.meldingsutveksling.dpi.client.internal.domain.XAdESArtifacts;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.dom.DOMResult;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import static java.util.stream.IntStream.range;
import static org.apache.commons.codec.digest.DigestUtils.sha1;

public class CreateXAdESArtifacts {

    private final Clock clock;
    private final Jaxb2Marshaller marshaller;
    private final DigestMethodType digestMethod;

    public CreateXAdESArtifacts(Clock clock) {
        this.clock = clock;
        this.marshaller = new Jaxb2Marshaller();
        this.marshaller.setClassesToBeBound(QualifyingPropertiesType.class);
        this.digestMethod = getDigestMethod();
    }

    private DigestMethodType getDigestMethod() {
        DigestMethodType type = new DigestMethodType();
        type.getContent();
        type.setAlgorithm(javax.xml.crypto.dsig.DigestMethod.SHA1);
        return type;
    }

    XAdESArtifacts createArtifactsToSign(List<AsicEAttachable> files, BusinessCertificate sertifikat) {
        byte[] certificateDigestValue = sha1(sertifikat.getEncoded());
        X509Certificate certificate = sertifikat.getX509Certificate();

        DigestAlgAndValueType certificateDigest = new DigestAlgAndValueType();
        certificateDigest.setDigestMethod(digestMethod);
        certificateDigest.setDigestValue(certificateDigestValue);

        X509IssuerSerialType certificateIssuer = new X509IssuerSerialType();
        certificateIssuer.setX509IssuerName(certificate.getIssuerDN().getName());
        certificateIssuer.setX509SerialNumber(certificate.getSerialNumber());

        CertIDType certID = new CertIDType();
        certID.setCertDigest(certificateDigest);
        certID.setIssuerSerial(certificateIssuer);

        CertIDListType signingCertificate = new CertIDListType();
        signingCertificate.getCert().add(certID);

        SignedSignaturePropertiesType signedSignatureProperties = new SignedSignaturePropertiesType();
        signedSignatureProperties.setSigningTime(getSigningTime());
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

        return from(qualifyingProperties);
    }

    private XMLGregorianCalendar getSigningTime() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        GregorianCalendar gregorianCalendar = GregorianCalendar.from(now);
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            throw new Exception("Could not get signing time", e);
        }
    }

    private List<DataObjectFormatType> dataObjectFormats(List<AsicEAttachable> files) {
        List<DataObjectFormatType> result = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            String signatureElementIdReference = "#ID_" + i;
            DataObjectFormatType dataObjectFormatType = new DataObjectFormatType();
            dataObjectFormatType.setMimeType(files.get(i).getMimeType());
            dataObjectFormatType.setObjectReference(signatureElementIdReference);
            result.add(dataObjectFormatType);
        }
        return result;
    }

    private XAdESArtifacts from(QualifyingPropertiesType qualifyingProperties) {
        DOMResult domResult = new DOMResult();
        marshaller.marshal(new ObjectFactory().createQualifyingProperties(qualifyingProperties), domResult);
        return from((Document) domResult.getNode());
    }

    private XAdESArtifacts from(Document qualifyingPropertiesDocument) {
        Element qualifyingProperties = qualifyingPropertiesDocument.getDocumentElement();
        NodeList qualifyingPropertiesContents = qualifyingProperties.getChildNodes();
        Element signedProperties = range(0, qualifyingPropertiesContents.getLength()).mapToObj(qualifyingPropertiesContents::item)
                .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
                .map(Element.class::cast)
                .filter(element -> "SignedProperties".equals(element.getLocalName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Didn't find SignedProperties in document."));
        String signerPropertiesReferenceUri = signedProperties.getAttribute("Id");
        return new XAdESArtifacts(qualifyingPropertiesDocument, signedProperties, "#" + signerPropertiesReferenceUri);
    }

    private static class Exception extends RuntimeException {

        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }
}