package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.AvsenderHolder;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.MessageType;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Avsender;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Identifikator;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Virksomhetmottaker;
import no.difi.meldingsutveksling.dpi.client.internal.CreateInstanceIdentifier;
import no.difi.meldingsutveksling.dpi.client.internal.CreateStandardBusinessDocumentJWT;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@RequiredArgsConstructor
public class CreateReceiptJWT {

    private final CreateStandardBusinessDocumentJWT createStandardBusinessDocumentJWT;
    private final CreateInstanceIdentifier createInstanceIdentifier;
    private final Clock clock;

    String createReceiptJWT(StandardBusinessDocument sbd, no.difi.meldingsutveksling.dpi.client.ReceiptFactory receiptFactory) {
        return createStandardBusinessDocumentJWT.createStandardBusinessDocumentJWT(
                createReceiptStandardBusinessDocument(sbd, receiptFactory), null, null);
    }

    private StandardBusinessDocument createReceiptStandardBusinessDocument(StandardBusinessDocument sbd, no.difi.meldingsutveksling.dpi.client.ReceiptFactory receiptFactory) {
        MessageType receiptType = receiptFactory.getMessageType();

        PartnerIdentification receiver = StandardBusinessDocumentUtils.getFirstReceiverIdentifier(sbd)
                .orElseThrow(() -> new IllegalArgumentException("Missing receiver!"));

        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setHeaderVersion("1.0")
                        .setBusinessScope(sbd.getStandardBusinessDocumentHeader().getBusinessScope())
                        .setDocumentIdentification(new DocumentIdentification()
                                .setInstanceIdentifier(createInstanceIdentifier.createInstanceIdentifier())
                                .setStandard(receiptType.getStandard())
                                .setType(receiptType.getType())
                                .setTypeVersion("1.0")
                                .setCreationDateAndTime(OffsetDateTime.now(clock)))
                        .addSender(sbd.getStandardBusinessDocumentHeader().getFirstReceiver()
                                .orElseThrow(() -> new IllegalArgumentException("Missing receiver")))
                        .addReceiver(sbd.getStandardBusinessDocumentHeader().getFirstSender()
                                .orElseThrow(() -> new IllegalArgumentException("Missing sender")))
                        .setBusinessScope(new BusinessScope()
                                .addScope(StandardBusinessDocumentUtils.getScope(sbd, ScopeType.CONVERSATION_ID)
                                        .orElseThrow(() -> new IllegalArgumentException("Missing conversationId")))))
                .setAny(receiptFactory.getReceipt(new no.difi.meldingsutveksling.dpi.client.ReceiptInput()
                        .setMottaker(new Virksomhetmottaker()
                                .setVirksomhetsidentifikator(getAvsender(sbd).getVirksomhetsidentifikator()))
                        .setAvsender(new Avsender()
                                .setVirksomhetsidentifikator(new Identifikator()
                                        .setAuthority(receiver.getAuthority())
                                        .setValue(receiver.getValue())
                                )
                        )));
    }

    private Avsender getAvsender(StandardBusinessDocument sbd) {
        return sbd.getBusinessMessage(AvsenderHolder.class)
                .flatMap(p -> Optional.ofNullable(p.getAvsender()))
                .orElseThrow(() -> new IllegalArgumentException("Missing Avsender!"));
    }
}
