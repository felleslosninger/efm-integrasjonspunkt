package no.difi.meldingsutveksling.dpi.json;

import io.micrometer.core.annotation.Timed;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
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

import java.util.*;
import java.util.function.Consumer;

import static no.difi.meldingsutveksling.logging.MarkerFactory.conversationIdMarker;

@Slf4j
@RequiredArgsConstructor
public class JsonMeldingsformidlerClient implements MeldingsformidlerClient {

    private final IntegrasjonspunktProperties properties;
    private final DpiClient dpiClient;
    private final ShipmentFactory shipmentFactory;
    private final JsonDpiReceiptMapper dpiReceiptMapper;
    @Getter(lazy = true) private final Set<String> partitionIds = createParticipantsIds();

    public Set<String> createParticipantsIds() {
        List<String> ids = properties.getDpi().getPartitionIds();
        return ids == null || ids.isEmpty()
                ? Collections.singleton(null)
                : Collections.unmodifiableSet(new HashSet<>(ids));
    }

    @Override
    public boolean shouldValidatePartitionId() {
        return true;
    }

    @Override
    public void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        Shipment shipment = shipmentFactory.getShipment(request);

        try {
            dpiClient.sendMessage(shipment);
        } catch (DpiException e) {
            throw new MeldingsformidlerException("Send DPI message failed!", e);
        }
    }

    @Timed
    @Override
    public void sjekkEtterKvitteringer(String avsenderindikator, Consumer<ExternalReceipt> callback) {
        dpiClient.getMessages(avsenderindikator)
                .map(JsonExternalReceipt::new)
                .toStream()
                .forEach(callback);
    }

    @RequiredArgsConstructor
    public class JsonExternalReceipt implements ExternalReceipt {

        private final ReceivedMessage receivedMessage;

        @Override
        public void confirmReceipt() {
            dpiClient.markAsRead(UUID.fromString(SBDUtil.getMessageId(receivedMessage.getStandardBusinessDocument())));
        }

        @Override
        public String getConversationId() {
            return SBDUtil.getConversationId(receivedMessage.getStandardBusinessDocument());
        }

        @Override
        public LogstashMarker logMarkers() {
            return conversationIdMarker(getConversationId());
        }

        @Override
        public MessageStatus toMessageStatus() {
            MessageStatus ms = dpiReceiptMapper.from(receivedMessage.getStandardBusinessDocument());
            ms.setRawReceipt(receivedMessage.getMessage().getForretningsmelding());
            return ms;
        }
    }
}
