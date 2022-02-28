package no.difi.meldingsutveksling.dpi.json;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Kvittering;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DpiMessageType;
import no.difi.meldingsutveksling.status.MessageStatus;

@RequiredArgsConstructor
public class JsonDpiReceiptMapper {

    private final MessageStatusMapper messageStatusMapper;

    public MessageStatus from(StandardBusinessDocument standardBusinessDocument) {
        MessageStatus ms = standardBusinessDocument.getType()
                .map(DpiMessageType::fromType)
                .map(messageStatusMapper::getMessageStatus)
                .orElseGet(messageStatusMapper::getDefaultMessageStatus);

        standardBusinessDocument.getBusinessMessage(Kvittering.class)
                .filter(kvittering -> kvittering.getTidspunkt() != null)
                .ifPresent(kvittering -> ms.setLastUpdate(kvittering.getTidspunkt()));

        return ms;
    }
}
