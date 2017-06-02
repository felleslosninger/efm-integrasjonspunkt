package no.difi.meldingsutveksling.receipt.strategy;

import no.altinn.schemas.services.serviceengine.correspondence._2014._10.StatusChangeV2;
import no.altinn.schemas.services.serviceengine.correspondence._2014._10.StatusV2;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2Response;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;
import no.difi.meldingsutveksling.receipt.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;

@Component
public class DpvStatusStrategy implements StatusStrategy {
    private static final ServiceIdentifier serviceIdentifier = ServiceIdentifier.DPV;

    @Autowired
    private IntegrasjonspunktProperties properties;

    @Autowired
    private ServiceRegistryLookup serviceRegistryLookup;

    @Autowired
    private ConversationRepository conversationRepository;

    private static final String STATUS_CREATED = "Created";
    private static final String STATUS_READ = "Read";

    @Override
    public void checkStatus(Conversation conversation) {

        CorrespondenceAgencyConfiguration config = new CorrespondenceAgencyConfiguration.Builder()
                .withExternalServiceCode(properties.getAltinnPTV().getExternalServiceCode())
                .withExternalServiceEditionCode(properties.getAltinnPTV().getExternalServiceEditionCode())
                .withPassword(properties.getAltinnPTV().getPassword())
                .withSystemUserCode(properties.getAltinnPTV().getUsername())
                .build();

        ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(conversation.getReceiverIdentifier());
        final CorrespondenceAgencyClient client = new CorrespondenceAgencyClient(markerFrom(conversation), config,
                serviceRecord.getEndPointURL());
        GetCorrespondenceStatusDetailsV2 receiptRequest = CorrespondenceAgencyMessageFactory.createReceiptRequest(conversation);
        final CorrespondenceRequest request = new CorrespondenceRequest.Builder().withUsername(config
                .getSystemUserCode()).withPassword(config.getPassword()).withPayload(receiptRequest).build();

        GetCorrespondenceStatusDetailsV2Response result = (GetCorrespondenceStatusDetailsV2Response) client
                .sendStatusRequest(request);
        if (result == null) {
            // Error is picked up by soap fault interceptor
            return;
        }

        // TODO: need to find a way to search for CorrespondenceIDs (in response( as ConversationID is not unqiue
        List<StatusV2> statusList = result.getGetCorrespondenceStatusDetailsV2Result().getValue().getStatusList().getValue().getStatusV2();
        Optional<StatusV2> op = statusList.stream().findFirst();
        if (op.isPresent()) {
            List<StatusChangeV2> statusChanges = op.get().getStatusChanges().getValue().getStatusChangeV2();

            Optional<StatusChangeV2> createdStatus = statusChanges.stream()
                    .filter(s -> STATUS_CREATED.equals(s.getStatusType().value()))
                    .findFirst();
            boolean hasCreatedStatus = conversation.getMessageStatuses().stream()
                    .anyMatch(r -> GenericReceiptStatus.LEVERT.toString().equals(r.getStatus()) );
            if (!hasCreatedStatus && createdStatus.isPresent()) {
                ZonedDateTime createdZoned = createdStatus.get().getStatusDate().toGregorianCalendar().toZonedDateTime();
                MessageStatus receipt = MessageStatus.of(GenericReceiptStatus.LEVERT.toString(), createdZoned
                        .toLocalDateTime());
                conversation.addMessageStatus(receipt);
                conversationRepository.save(conversation);
            }

            Optional<StatusChangeV2> readStatus = statusChanges.stream()
                    .filter(s -> STATUS_READ.equals(s.getStatusType().value()))
                    .findFirst();
            if (readStatus.isPresent()) {
                ZonedDateTime readZoned = readStatus.get().getStatusDate().toGregorianCalendar().toZonedDateTime();
                MessageStatus receipt = MessageStatus.of(GenericReceiptStatus.LEST.toString(), readZoned
                        .toLocalDateTime());
                conversation.addMessageStatus(receipt);
                conversation.setPollable(false);
                conversation.setFinished(true);
                conversationRepository.save(conversation);
            }

        }

    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return serviceIdentifier;
    }
}
