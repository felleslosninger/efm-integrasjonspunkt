package no.difi.meldingsutveksling.dpi.json;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.dpi.client.DpiClient;
import no.difi.meldingsutveksling.dpi.client.DpiException;
import no.difi.meldingsutveksling.dpi.client.domain.GetMessagesInput;
import no.difi.meldingsutveksling.dpi.client.domain.ReceivedMessage;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.meldingsutveksling.status.ExternalReceipt;
import no.difi.meldingsutveksling.status.MessageStatus;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.logging.MarkerFactory.conversationIdMarker;

@Slf4j
@RequiredArgsConstructor
public class JsonMeldingsformidlerClient implements MeldingsformidlerClient {

    private final DpiClient dpiClient;
    private final ShipmentFactory shipmentFactory;
    private final JsonDpiReceiptMapper dpiReceiptMapper;
    private final MessageStatusMapper messageStatusMapper;
    private final ChannelNormalizer channelNormalizer;

    @Override
    public boolean skalPolleMeldingStatus() {
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
    public void sjekkEtterKvitteringer(String avsenderidentifikator, String mpcId, Consumer<ExternalReceipt> callback) {
        dpiClient.getMessages(new GetMessagesInput()
                        .setSenderId(avsenderidentifikator)
                        .setChannel(channelNormalizer.normaiize(mpcId))
                )
                .map(JsonExternalReceipt::new)
                .toStream()
                .forEach(callback);
    }

    @Override
    public Stream<MessageStatus> hentMeldingStatusListe(String messageId) {
        return dpiClient.getMessageStatuses(UUID.fromString(messageId))
                .map(messageStatusMapper::getMessageStatus)
                .toStream();
    }


    @RequiredArgsConstructor
    public class JsonExternalReceipt implements ExternalReceipt {

        private final ReceivedMessage receivedMessage;

        @Override
        public void confirmReceipt() {
            dpiClient.markAsRead(UUID.fromString(receivedMessage.getStandardBusinessDocument().getMessageId()));
        }

        @Override
        public String getConversationId() {
            return receivedMessage.getStandardBusinessDocument().getConversationId();
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
