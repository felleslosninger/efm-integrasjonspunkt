package no.difi.meldingsutveksling.altinnv3.dpv;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.status.Conversation;
import no.digdir.altinn3.correspondence.model.CorrespondenceStatusEventExt;
import no.digdir.altinn3.correspondence.model.CorrespondenceStatusExt;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondencesExt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
@RequiredArgsConstructor
public class AltinnDPVService {

    private final CorrespondenceApiClient client;
    private final CorrespondenceCreatorService correspondenceCreatorService;
    private final FileRetriever fileRetriever;

    public UUID send(final NextMoveOutMessage message) {

        List<FileUploadRequest> files = fileRetriever.getFiles(message);

        InitializeCorrespondencesExt correspondence = correspondenceCreatorService.create(
            message,
            null,
            files.stream().map(FileUploadRequest::getBusinessMessageFile).collect(Collectors.toList()));

        // selv om den er String i koden skal den inneholde en UUID (ingen andre steder i kodebasen wrapper
        // tilsvarende UUID.fromString-kall i try/catch, alle stoler på den samme oppstrøms bean-valideringen)
        correspondence.setIdempotentKey(UUID.fromString(message.getMessageId()));

        var result = client.upload(correspondence, files);

        if (result == null || result.getCorrespondences() == null) { throw new CorrespondenceApiException("Error when sending message to Altinn, response was null");}
        return result.getCorrespondences().getFirst().getCorrespondenceId();
    }

    // brukes til å finne igjen correspondenceId etter en 409-konflikt på upload (idempotentKey allerede kjent
    // hos Altinn), siden sendersReference alltid settes til messageId ved opplasting, se CorrespondenceFactory
    public Optional<UUID> findExistingCorrespondenceId(String messageId) {
        var correspondences = client.findCorrespondences(messageId);

        if (correspondences == null || correspondences.getIds() == null || correspondences.getIds().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(correspondences.getIds().getFirst());
    }

    public List<CorrespondenceStatusEventExt> getStatus(Conversation conversation) {
        var correspondenceId = conversation.getExternalSystemReference();

        if (correspondenceId == null || correspondenceId.isEmpty()) {
            throw new InvalidConversationReferenceException("Missing correspondenceId in conversation reference for conversation " + conversation.getConversationId());
        }

        List<CorrespondenceStatusEventExt> statusEvents = new ArrayList<>();
        var overview = client.getCorrespondenceOverview(UUID.fromString(correspondenceId));
        statusEvents.add(createStatusEvent(CorrespondenceStatusExt.INITIALIZED, overview.getCreated()));
        if (overview.getPublished() != null) statusEvents.add(createStatusEvent(CorrespondenceStatusExt.PUBLISHED, overview.getPublished()));
        if (overview.getRead() != null) statusEvents.add(createStatusEvent(CorrespondenceStatusExt.READ, overview.getRead()));
        if (isPurged(overview.getStatus())) statusEvents.add(createStatusEvent(overview.getStatus(), overview.getStatusChanged()));
        return statusEvents;

    }

    public static boolean isPurged(CorrespondenceStatusExt status) {
        return CorrespondenceStatusExt.PURGED_BY_RECIPIENT.equals(status)
                || CorrespondenceStatusExt.PURGED_BY_ALTINN.equals(status);
    }

    private CorrespondenceStatusEventExt createStatusEvent(CorrespondenceStatusExt status, OffsetDateTime timestamp) {
        return new CorrespondenceStatusEventExt().status(status).statusChanged(timestamp).statusText(status.getValue());
    }

}
