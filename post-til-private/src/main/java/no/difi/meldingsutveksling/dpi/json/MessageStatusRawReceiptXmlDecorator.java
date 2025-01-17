package no.difi.meldingsutveksling.dpi.json;

import lombok.SneakyThrows;
import no.difi.begrep.sdp.schema_v10.SDPFeil;
import no.difi.begrep.sdp.schema_v10.SDPFeiltype;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLOutputFactory;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;

import static no.difi.meldingsutveksling.dpi.json.XmlSoapDpiReceiptHelper.*;

public class MessageStatusRawReceiptXmlDecorator implements MessageStatusDecorator {

    private final JAXBContext context;
    private final XMLOutputFactory xmlOutputFactory;
    private final ObjectFactory objectFactory;

    public MessageStatusRawReceiptXmlDecorator() throws JAXBException {
        this.context = JAXBContext.newInstance(StandardBusinessDocument.class, SDPFeil.class);
        this.xmlOutputFactory = XMLOutputFactory.newFactory();
        this.objectFactory = new ObjectFactory();
    }

    @Override
    @SneakyThrows
    public MessageStatus apply(Conversation conversation, MessageStatus messageStatus) {
        if (messageStatus.getRawReceipt() != null) {
            return messageStatus;
        }
        if (! ReceiptStatus.FEIL.toString().equals(messageStatus.getStatus())) {
            return messageStatus;
        }
        StandardBusinessDocument sbd = new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setHeaderVersion("1.0")
                        .setSender(Collections.singleton(new Partner()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908")
                                        .setValue("9908:984661185"))))
                        .setReceiver(Collections.singleton(new Partner()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908")
                                        .setValue("9908:984661185"))))
                        .setDocumentIdentification(new DocumentIdentification()
                                .setStandard("urn:no:difi:sdp:1.0")
                                .setType("feil")
                                .setTypeVersion("1.0")
                                .setInstanceIdentifier(conversation.getMessageId())
                                .setCreationDateAndTime(OffsetDateTime.now()))
                        .setBusinessScope(new BusinessScope()
                                .setScope(Collections.singleton(new Scope()
                                        .setType("ConversationId")
                                        .setIdentifier("urn:no:difi:sdp:1.0")
                                        .setInstanceIdentifier(conversation.getConversationId())))))
                .setAny(new SDPFeil()
                        .withSignature(getSignature())
                        .withTidspunkt(ZonedDateTime.now())
                        .withFeiltype(SDPFeiltype.SERVER)
                        .withDetaljer(messageStatus.getDescription()));

        String rawReceipt = serialize(context, xmlOutputFactory, objectFactory, sbd);
        messageStatus.setRawReceipt(rawReceipt);
        return messageStatus;
    }

}
