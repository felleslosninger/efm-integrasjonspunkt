package no.difi.meldingsutveksling.dpi.json;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DpiMessageType;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Kvittering;
import no.difi.meldingsutveksling.status.MessageStatus;

@Slf4j
@RequiredArgsConstructor
public class JsonDpiReceiptMapper {

    private final MessageStatusMapper messageStatusMapper;

    public MessageStatus from(StandardBusinessDocument standardBusinessDocument) {

        String type = standardBusinessDocument.getType();
        MessageStatus ms = messageStatusMapper.getMessageStatus(DpiMessageType.fromType(type));

        // jackson will try to parse what's in the "xs:any field" to an instance of Kvittering
        standardBusinessDocument.getBusinessMessage(Kvittering.class)
                .filter(kvittering -> kvittering.getTidspunkt() != null)
                .ifPresentOrElse(
                        kvittering -> ms.setLastUpdate(kvittering.getTidspunkt()),
                        () -> log.warn("No tidspunkt found in kvittering with conversationId={} and type={}", standardBusinessDocument.getConversationId(), type)
                );

        return ms;
    }

}
