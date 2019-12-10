package no.difi.meldingsutveksling.status.strategy;

import lombok.RequiredArgsConstructor;
import no.altinn.schemas.services.serviceengine.correspondence._2014._10.StatusChangeV2;
import no.altinn.schemas.services.serviceengine.correspondence._2014._10.StatusV2;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2Response;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.receipt.*;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ConversationService;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.difi.meldingsutveksling.api.StatusStrategy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

import static no.difi.meldingsutveksling.ptv.WithLogstashMarker.withLogstashMarker;
import static no.difi.meldingsutveksling.status.ConversationMarker.markerFrom;

@Component
@RequiredArgsConstructor
@Order
public class DpvStatusStrategy implements StatusStrategy {

    private static final ServiceIdentifier serviceIdentifier = ServiceIdentifier.DPV;
    private static final String STATUS_CREATED = "Created";
    private static final String STATUS_READ = "Read";

    private final ConversationService conversationService;
    private final CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory;
    private final CorrespondenceAgencyClient client;
    private final MessageStatusFactory messageStatusFactory;

    @Override
    public void checkStatus(final Conversation conversation) {
        GetCorrespondenceStatusDetailsV2 receiptRequest = correspondenceAgencyMessageFactory.createReceiptRequest(conversation);

        Object response = withLogstashMarker(markerFrom(conversation))
                .execute(() -> client.sendStatusRequest(receiptRequest));

        if (response == null) {
            // Error is picked up by soap fault interceptor
            return;
        }

        GetCorrespondenceStatusDetailsV2Response result = (GetCorrespondenceStatusDetailsV2Response) response;

        // TODO: need to find a way to search for CorrespondenceIDs (in response( as ConversationID is not unqiue
        result.getGetCorrespondenceStatusDetailsV2Result().getValue().getStatusList().getValue().getStatusV2()
                .stream().findFirst()
                .ifPresent(op -> checkStatus(conversation, op));
    }

    private void checkStatus(Conversation conversation, StatusV2 op) {
        List<StatusChangeV2> statusChanges = op.getStatusChanges().getValue().getStatusChangeV2();

        statusChanges.stream()
                .filter(s -> STATUS_CREATED.equals(s.getStatusType().value()))
                .filter(createdStatus -> conversation.getMessageStatuses().stream()
                        .noneMatch(r -> ReceiptStatus.LEVERT.toString().equals(r.getStatus())))
                .findFirst()
                .ifPresent(createdStatus -> {
                    OffsetDateTime createdZoned = createdStatus.getStatusDate().toGregorianCalendar().toZonedDateTime().toOffsetDateTime();
                    conversationService.registerStatus(conversation, messageStatusFactory.getMessageStatus(ReceiptStatus.LEVERT, createdZoned));
                });

        statusChanges.stream()
                .filter(s -> STATUS_READ.equals(s.getStatusType().value()))
                .findFirst()
                .ifPresent(readStatus -> {
                    OffsetDateTime readZoned = readStatus.getStatusDate().toGregorianCalendar().toZonedDateTime().toOffsetDateTime();
                    conversationService.registerStatus(conversation, messageStatusFactory.getMessageStatus(ReceiptStatus.LEST, readZoned));
                });
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return serviceIdentifier;
    }
}
