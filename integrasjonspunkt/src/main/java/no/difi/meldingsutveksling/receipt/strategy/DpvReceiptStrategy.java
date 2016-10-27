package no.difi.meldingsutveksling.receipt.strategy;

import net.logstash.logback.marker.LogstashMarker;
import no.altinn.schemas.services.serviceengine.correspondence._2014._10.StatusChangeV2;
import no.altinn.schemas.services.serviceengine.correspondence._2014._10.StatusV2;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2Response;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;
import no.difi.meldingsutveksling.receipt.ExternalReceipt;
import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.ReceiptStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.difi.meldingsutveksling.receipt.MessageReceiptMarker.markerFrom;

public class DpvReceiptStrategy implements ReceiptStrategy {
    private static final String STATUS_CREATED = "Created";
    private static final String STATUS_READ = "Read";

    @Autowired
    private IntegrasjonspunktProperties properties;
    private String messageId;

    private LogstashMarker markers;

    public DpvReceiptStrategy(IntegrasjonspunktProperties properties, String messageId, LogstashMarker markers) {
        this.properties = properties;
        this.messageId = messageId;
        this.markers = markers;
    }

    @Override
    public ExternalReceipt getReceipt() {
        CorrespondenceAgencyConfiguration config = new CorrespondenceAgencyConfiguration.Builder()
                .withEndpointURL(properties.getAltinnPTV().getEndpointUrl())
                .withExternalServiceCode(properties.getAltinnPTV().getExternalServiceCode())
                .withExternalServiceEditionCode(properties.getAltinnPTV().getExternalServiceEditionCode())
                .withPassword(properties.getAltinnPTV().getPassword())
                .withSystemUserCode(properties.getAltinnPTV().getUsername())
                .build();
        final CorrespondenceAgencyClient client = new CorrespondenceAgencyClient(markers, config);
        GetCorrespondenceStatusDetailsV2 receiptRequest = CorrespondenceAgencyMessageFactory.createReceiptRequest(messageId);
        final CorrespondenceRequest request = new CorrespondenceRequest.Builder().withUsername(config
                .getSystemUserCode()).withPassword(config.getPassword()).withPayload(receiptRequest).build();

        GetCorrespondenceStatusDetailsV2Response result = (GetCorrespondenceStatusDetailsV2Response) client
                .sendStatusRequest(request);


        return new DpvExternalReceipt(result);
    }

    private static class DpvExternalReceipt implements ExternalReceipt {
        private final GetCorrespondenceStatusDetailsV2Response result;

        public DpvExternalReceipt(GetCorrespondenceStatusDetailsV2Response result) {
            this.result = result;
        }

        @Override
        public void update(MessageReceipt messageReceipt) {
            // TODO: need to find a way to search for CorrespondenceIDs (in response( as ConversationID is not unqiue
            List<StatusV2> statusList = result.getGetCorrespondenceStatusDetailsV2Result().getValue().getStatusList().getValue().getStatusV2();
            Optional<StatusV2> op = statusList.stream().findFirst();
            if (op.isPresent()) {
                List<StatusChangeV2> statusChanges = op.get().getStatusChanges().getValue().getStatusChangeV2();

                Optional<StatusChangeV2> readStatus = statusChanges.stream()
                        .filter(s -> STATUS_READ.equals(s.getStatusType().value()))
                        .findFirst();
                if (readStatus.isPresent()) {
                    ZonedDateTime readZoned = readStatus.get().getStatusDate().toGregorianCalendar().toZonedDateTime();
                    messageReceipt.setLastUpdate(readZoned.toLocalDateTime());
                    messageReceipt.setReceived(true);
                    Audit.info("Changed status to \"received\" for messageId="+ messageReceipt.getMessageId(), markerFrom(messageReceipt));
                } else {
                    // If no "Read"-status yet, update with "Created" status date
                    Optional<StatusChangeV2> createdStatus = statusChanges.stream()
                            .filter(s -> STATUS_CREATED.equals(s.getStatusType().value()))
                            .findFirst();
                    if (createdStatus.isPresent()) {
                        ZonedDateTime createdZoned = createdStatus.get().getStatusDate().toGregorianCalendar().toZonedDateTime();
                        messageReceipt.setLastUpdate(createdZoned.toLocalDateTime());
                        messageReceipt.setReceived(true);
                        Audit.info("Changed status to \"received\" for messageId="+ messageReceipt.getMessageId(), markerFrom(messageReceipt));
                    }
                }

            }
        }

        @Override
        public void confirmReceipt() {
            // Do nothing
        }
    }
}
