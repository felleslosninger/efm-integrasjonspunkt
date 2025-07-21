package no.difi.meldingsutveksling.altinnv3.dpv;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.status.Conversation;
import no.digdir.altinn3.correspondence.model.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
@RequiredArgsConstructor
public class AltinnDPVService {

    private final CorrespondenceApiClient client;
    private final CorrespondenceCreatorService correspondenceCreatorService;
    private final FileRetriever fileRetriever;

    public UUID send(final NextMoveOutMessage message){

        List<FileUploadRequest> files = fileRetriever.getFiles(message);

        InitializeCorrespondencesExt correspondence = correspondenceCreatorService.create(
            message,
            null,
            files.stream().map(FileUploadRequest::getBusinessMessageFile).collect(Collectors.toList()));

        var result = client.upload(correspondence, files);

        if(result == null || result.getCorrespondences() == null){ throw new CorrespondenceApiException("Error when sending message to Altinn, response was null");}
        return result.getCorrespondences().getFirst().getCorrespondenceId();
    }

    public List<CorrespondenceStatusEventExt> getStatus(Conversation conversation){
        return client.getCorrespondenceDetails(UUID.fromString(conversation.getExternalSystemReference()))
            .getStatusHistory();
    }
}
