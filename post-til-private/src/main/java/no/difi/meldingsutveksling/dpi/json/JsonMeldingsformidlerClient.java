package no.difi.meldingsutveksling.dpi.json;

import lombok.RequiredArgsConstructor;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.status.ExternalReceipt;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.digdir.dpi.client.DpiClient;
import no.digdir.dpi.client.DpiException;
import no.digdir.dpi.client.domain.ReceivedMessage;
import no.digdir.dpi.client.domain.Shipment;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static no.difi.meldingsutveksling.logging.MarkerFactory.conversationIdMarker;

@RequiredArgsConstructor
public class JsonMeldingsformidlerClient implements MeldingsformidlerClient {

    private final DpiClient dpiClient;
    private final ShipmentFactory shipmentFactory;
    private final JsonDpiReceiptMapper dpiReceiptMapper;

    @Override
    public void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        Shipment shipment = shipmentFactory.getShipment(request);

        try {
            dpiClient.sendMessage(shipment);
        } catch (DpiException e) {
            throw new MeldingsformidlerException("Send DPI message failed!", e);
        }
    }

    @Override
    public Flux<ExternalReceipt> sjekkEtterKvitteringer(String orgnr, String mpcId) {
        return dpiClient.getMessages()
                .map(JsonExternalReceipt::new);
    }

    @RequiredArgsConstructor
    public class JsonExternalReceipt implements ExternalReceipt {

        private final ReceivedMessage receivedMessage;

        @Override
        public void confirmReceipt() {            dpiClient.markAsRead(UUID.fromString(getId()));
        }

        @Override
        public String getId() {
            return SBDUtil.getMessageId(receivedMessage.getStandardBusinessDocument());
        }

        @Override
        public LogstashMarker logMarkers() {
            return conversationIdMarker(getId());
        }

        @Override
        public MessageStatus toMessageStatus() {
            MessageStatus ms = dpiReceiptMapper.from(receivedMessage.getStandardBusinessDocument());
            ms.setRawReceipt(receivedMessage.getMessage().getForettningsmelding());
            return ms;
        }
    }
}
