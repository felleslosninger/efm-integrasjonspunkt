package no.difi.meldingsutveksling.dpi.json;

import com.nimbusds.jose.Payload;
import lombok.SneakyThrows;
import no.difi.begrep.sdp.schema_v10.*;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.*;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackJWT;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackStandardBusinessDocument;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLOutputFactory;

import static no.difi.meldingsutveksling.dpi.json.XmlSoapDpiReceiptHelper.*;

public class JWT2XmlSoapDpiReceiptConverter implements DpiReceiptConverter {

    private final JAXBContext context;
    private final XMLOutputFactory xmlOutputFactory;
    private final ObjectFactory objectFactory;
    private final UnpackJWT unpackJWT;
    private final UnpackStandardBusinessDocument unpackStandardBusinessDocument;

    public JWT2XmlSoapDpiReceiptConverter(UnpackJWT unpackJWT, UnpackStandardBusinessDocument unpackStandardBusinessDocument) throws JAXBException {
        this.context = JAXBContext.newInstance(StandardBusinessDocument.class, SDPKvittering.class, SDPFeil.class);
        this.xmlOutputFactory = XMLOutputFactory.newFactory();
        this.objectFactory = new ObjectFactory();
        this.unpackJWT = unpackJWT;
        this.unpackStandardBusinessDocument = unpackStandardBusinessDocument;
    }

    @Override
    @SneakyThrows
    public String apply(String jwt) {
        Payload payload = unpackJWT.getPayload(jwt);
        StandardBusinessDocument sbd = unpackStandardBusinessDocument.unpackStandardBusinessDocument(payload);
        StandardBusinessDocumentHeader sbdh = sbd.getStandardBusinessDocumentHeader();

        DpiMessageType messageType = DpiMessageType.fromType(sbd.getType());

        sbdh.getSender().stream().map(Partner::getIdentifier).forEach(p -> p.setAuthority(getAuthority(p)));
        sbdh.getReceiver().stream().map(Partner::getIdentifier).forEach(p -> p.setAuthority(getAuthority(p)));
        sbdh.getDocumentIdentification()
                .setStandard("urn:no:difi:sdp:1.0")
                .setTypeVersion("1.0")
                .setType(getType(messageType));
        sbd.getScope(ScopeType.CONVERSATION_ID).ifPresent(p -> p.setIdentifier("urn:no:difi:sdp:1.0"));
        sbd.setAny(messageType == DpiMessageType.FEIL ? getSdpFeil(sbd) : getSdpKvittering(sbd, messageType));

        return serialize(context, xmlOutputFactory, objectFactory, sbd);
    }

}
