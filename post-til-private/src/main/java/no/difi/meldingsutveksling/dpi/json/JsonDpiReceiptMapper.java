package no.difi.meldingsutveksling.dpi.json;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DpiMessageType;
import no.difi.meldingsutveksling.dpi.client.internal.DpiMapper;
import no.difi.meldingsutveksling.status.MessageStatus;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class JsonDpiReceiptMapper {

    private final MessageStatusMapper messageStatusMapper;
    private DpiMapper dpiMapper = new DpiMapper();

    public MessageStatus from(StandardBusinessDocument standardBusinessDocument) {

        String type = standardBusinessDocument.getType();
        MessageStatus ms = messageStatusMapper.getMessageStatus(
                DpiMessageType.fromType(type));

        var map = dpiMapper.convertToJsonObject(standardBusinessDocument);

//        var sbd = (Map<Object, Object>) map.get("standardBusinessDocument");
//        var sfm = (Map<Object, Object>) sbd.get(type);
//        var tidspunkt = sfm.get("tidspunkt");

        Optional.ofNullable((Map<Object, Object>) map.get("standardBusinessDocument"))
            .map(sbd -> (Map<Object, Object>) sbd.get(type))
            .map(sfm -> sfm.get("tidspunkt"))
            .map(t -> OffsetDateTime.parse(t.toString()))
            .ifPresent(ms::setLastUpdate);

//        standardBusinessDocument.getBusinessMessage(Kvittering.class)
//                .filter(kvittering -> kvittering.getTidspunkt() != null)
//                .ifPresent(kvittering -> ms.setLastUpdate(kvittering.getTidspunkt()));

        return ms;
    }

}
