package no.difi.meldingsutveksling.receipt.strategy;

import lombok.RequiredArgsConstructor;
import no.altinn.schemas.services.serviceengine.correspondence._2014._10.StatusChangeV2;
import no.altinn.schemas.services.serviceengine.correspondence._2014._10.StatusV2;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2Response;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.receipt.*;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.difi.meldingsutveksling.ptv.WithLogstashMarker.withLogstashMarker;
import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;

@Component
@RequiredArgsConstructor
public class DpvStatusStrategy implements StatusStrategy {

    private static final ServiceIdentifier serviceIdentifier = ServiceIdentifier.DPV;
    private static final String STATUS_CREATED = "Created";
    private static final String STATUS_READ = "Read";

    private final ConversationService conversationService;
    private final CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory;
    private final CorrespondenceAgencyClient client;

    @Override
    public void checkStatus(Conversation conversation) {
        GetCorrespondenceStatusDetailsV2 receiptRequest = correspondenceAgencyMessageFactory.createReceiptRequest(conversation);

        Object response = withLogstashMarker(markerFrom(conversation))
                .execute(() -> client.sendStatusRequest(receiptRequest));

        if (response == null) {
            // Error is picked up by soap fault interceptor
            return;
        }

        GetCorrespondenceStatusDetailsV2Response result = (GetCorrespondenceStatusDetailsV2Response) response;

        // TODO: need to find a way to search for CorrespondenceIDs (in response( as ConversationID is not unqiue
        List<StatusV2> statusList = result.getGetCorrespondenceStatusDetailsV2Result().getValue().getStatusList().getValue().getStatusV2();
        Optional<StatusV2> op = statusList.stream().findFirst();
        if (op.isPresent()) {
            List<StatusChangeV2> statusChanges = op.get().getStatusChanges().getValue().getStatusChangeV2();

            Optional<StatusChangeV2> createdStatus = statusChanges.stream()
                    .filter(s -> STATUS_CREATED.equals(s.getStatusType().value()))
                    .findFirst();
            ReceiptStatus levertStatus = ReceiptStatus.LEVERT;
            boolean hasCreatedStatus = conversation.getMessageStatuses().stream()
                    .anyMatch(r -> levertStatus.toString().equals(r.getStatus()));
            if (!hasCreatedStatus && createdStatus.isPresent()) {
                ZonedDateTime createdZoned = createdStatus.get().getStatusDate().toGregorianCalendar().toZonedDateTime();
                MessageStatus status = MessageStatus.of(levertStatus, createdZoned.toLocalDateTime());
                conversationService.registerStatus(conversation, status);
            }

            Optional<StatusChangeV2> readStatus = statusChanges.stream()
                    .filter(s -> STATUS_READ.equals(s.getStatusType().value()))
                    .findFirst();
            ReceiptStatus lestStatus = ReceiptStatus.LEST;
            if (readStatus.isPresent()) {
                ZonedDateTime readZoned = readStatus.get().getStatusDate().toGregorianCalendar().toZonedDateTime();
                MessageStatus status = MessageStatus.of(lestStatus, readZoned.toLocalDateTime());
                conversation = conversationService.registerStatus(conversation, status);
                conversationService.markFinished(conversation);
            }
        }
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return serviceIdentifier;
    }
}
