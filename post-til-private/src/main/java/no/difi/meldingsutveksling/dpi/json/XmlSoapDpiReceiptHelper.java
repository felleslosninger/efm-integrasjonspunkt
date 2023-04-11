package no.difi.meldingsutveksling.dpi.json;

import lombok.SneakyThrows;
import no.difi.begrep.sdp.schema_v10.*;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.*;
import no.digipost.org.w3.xmldsig.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

class XmlSoapDpiReceiptHelper {

    static String getType(DpiMessageType messageType) {
        return messageType == DpiMessageType.FEIL ? "feil" : "kvittering";
    }

    static String getAuthority(PartnerIdentification p) {
        Iso6523 iso6523 = Iso6523.parse(p.getValue());
        return String.format("urn:oasis:names:tc:ebcore:partyid-type:iso6523:%s", iso6523.getIcd().getCode());
    }

    @NotNull
    static SDPFeil getSdpFeil(StandardBusinessDocument sbd) {
        SDPFeil feil = new SDPFeil()
                .withSignature(getSignature())
                .withTidspunkt(getTidspunkt(sbd));

        sbd.getBusinessMessage(Feil.class)
                .ifPresent(p -> feil.withFeiltype(p.getFeiltype() == Feil.Type.KLIENT ? SDPFeiltype.KLIENT : SDPFeiltype.SERVER)
                        .withDetaljer(p.getDetaljer())
                );

        return feil;
    }

    @NotNull
    static SDPKvittering getSdpKvittering(StandardBusinessDocument sbd, DpiMessageType messageType) {
        SDPKvittering kvittering = new SDPKvittering()
                .withSignature(getSignature())
                .withTidspunkt(getTidspunkt(sbd));

        switch (messageType) {
            case LEVERINGSKVITTERING:
                kvittering.withLevering(new SDPLevering());
                break;
            case AAPNINGSKVITTERING:
                kvittering.withAapning(new SDPAapning());
                break;
            case VARSLINGFEILETKVITTERING:
                sbd.getBusinessMessage(Varslingfeiletkvittering.class)
                        .ifPresent(p -> kvittering.withVarslingfeilet(new SDPVarslingfeilet()
                                .withVarslingskanal(p.getVarslingskanal() == Varslingskanal.SMS ? SDPVarslingskanal.SMS : SDPVarslingskanal.EPOST)
                                .withBeskrivelse(p.getBeskrivelse())
                        ));
                break;
            case MOTTAKSKVITTERING:
                kvittering.withMottak(new SDPMottak());
                break;
            case RETURPOSTKVITTERING:
                kvittering.withReturpost(new SDPReturpost());
                break;
            default:
        }

        return kvittering;
    }

    static Signature getSignature() {
        return new Signature()
                .withSignedInfo(new SignedInfo()
                        .withCanonicalizationMethod(new CanonicalizationMethod()
                                .withAlgorithm("http://www.w3.org/2001/10/xml-exc-c14n#"))
                        .withSignatureMethod(new SignatureMethod()
                                .withAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"))
                        .withReferences(new Reference()
                                .withURI("")
                                .withTransforms(new Transforms()
                                        .withTransforms(new Transform()
                                                .withAlgorithm("http://www.w3.org/2000/09/xmldsig#enveloped-signature")))
                                .withDigestMethod(new DigestMethod()
                                        .withAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256"))
                                .withDigestValue("".getBytes())
                        )
                )
                .withSignatureValue(new SignatureValue())
                .withKeyInfo(new KeyInfo()
                        .withContent(new X509Data()
                                .withX509IssuerSerialsAndX509SKISAndX509SubjectNames(new org.w3._2000._09.xmldsig_.ObjectFactory().createX509DataX509Certificate("".getBytes()))
                        )
                );
    }

    @Nullable
    private static ZonedDateTime getTidspunkt(StandardBusinessDocument sbd) {
        return sbd.getBusinessMessage(TidspunktHolder.class).map(TidspunktHolder::getTidspunkt)
                .map(OffsetDateTime::toZonedDateTime)
                .orElse(null);
    }

    @NotNull
    @SneakyThrows
    private static Marshaller getMarshaller(JAXBContext context) {
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
        return marshaller;
    }

    @SneakyThrows
    static String serialize(JAXBContext context, XMLOutputFactory xmlOutputFactory, ObjectFactory objectFactory,
                            StandardBusinessDocument sbd) {
        Marshaller marshaller = getMarshaller(context);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(baos, (String) marshaller.getProperty(Marshaller.JAXB_ENCODING));
        xmlStreamWriter.writeStartDocument((String) marshaller.getProperty(Marshaller.JAXB_ENCODING), "1.0");
        marshaller.marshal(objectFactory.createStandardBusinessDocument(sbd), xmlStreamWriter);
        xmlStreamWriter.writeEndDocument();
        xmlStreamWriter.close();
        return baos.toString();
    }
}
