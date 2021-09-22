package no.difi.meldingsutveksling.dpi.json;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentUtils;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.digdir.dpi.client.domain.messagetypes.Kvittering;
import no.digdir.dpi.client.domain.messagetypes.MessageType;

@RequiredArgsConstructor
public class JsonDpiReceiptMapper {

    private final MessageStatusMapper messageStatusMapper;

    public MessageStatus from(StandardBusinessDocument standardBusinessDocument) {
        MessageStatus ms = StandardBusinessDocumentUtils.getType(standardBusinessDocument)
                .map(MessageType::fromType)
                .map(messageStatusMapper::getMessageStatus)
                .orElseGet(messageStatusMapper::getDefaultMessageStatus);

        standardBusinessDocument.getBusinessMessage(Kvittering.class)
                .filter(kvittering -> kvittering.getTidspunkt() != null)
                .ifPresent(kvittering -> ms.setLastUpdate(kvittering.getTidspunkt()));

        return ms;
    }
}
