package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Direction;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.MessageType;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CreateStandardBusinessDocument {

    private final Clock clock;

    public StandardBusinessDocument createStandardBusinessDocument(Shipment shipment) {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(getStandardBusinessDocumentHeader(shipment))
                .setAny(shipment.getBusinessMessage());
    }

    private StandardBusinessDocumentHeader getStandardBusinessDocumentHeader(Shipment shipment) {
        MessageType outgoingMessageType = MessageType.fromClass(shipment.getBusinessMessage(), Direction.OUTGOING);

        return new StandardBusinessDocumentHeader()
                .setHeaderVersion("1.0")
                .addSender(new Partner()
                        .setIdentifier(new PartnerIdentification()
                                .setAuthority(shipment.getSender().getAuthority())
                                .setValue(shipment.getSender().getIdentifier())
                        )
                )
                .addReceiver(new Partner()
                        .setIdentifier(new PartnerIdentification()
                                .setAuthority(shipment.getReceiver().getAuthority())
                                .setValue(shipment.getReceiver().getIdentifier())
                        )
                )
                .setDocumentIdentification(new DocumentIdentification()
                        .setInstanceIdentifier(shipment.getMessageId())
                        .setStandard(outgoingMessageType.getStandard())
                        .setType(outgoingMessageType.getType())
                        .setTypeVersion("1.0")
                        .setCreationDateAndTime(OffsetDateTime.now(clock))
                )
                .setBusinessScope(new BusinessScope()
                        .addScope(new Scope()
                                .setType(ScopeType.CONVERSATION_ID.getFullname())
                                .setInstanceIdentifier(shipment.getConversationId())
                                .setIdentifier(outgoingMessageType.getProcess())
                                .addScopeInformation(new CorrelationInformation()
                                        .setExpectedResponseDateTime(shipment.getExpectedResponseDateTime())
                                )
                        )
                );
    }
}
