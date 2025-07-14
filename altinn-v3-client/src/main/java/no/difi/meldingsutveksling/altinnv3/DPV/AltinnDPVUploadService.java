package no.difi.meldingsutveksling.altinnv3.DPV;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.*;
import no.digdir.altinn3.correspondence.model.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
@RequiredArgsConstructor
public class AltinnDPVUploadService {

    private final CorrespondenceApiClient client;
    private final CorrespondenceFactory correspondenceFactory;
    private final FileRetriever fileRetriever;
    private final Helper helper;


    public String send(final NextMoveOutMessage message){

        List<FileUploadRequest> files = fileRetriever.getFiles(message);

        InitializeCorrespondencesExt correspondence = correspondenceFactory.create(message, null, files.stream().map(FileUploadRequest::getBusinessMessageFile).collect(Collectors.toList()));

        var result = client.upload(correspondence, files);

        return correspondence.getCorrespondence().getResourceId();
    }


//    public InitializeCorrespondencesResponseExt sendSeperate(final NextMoveOutMessage message){
//        List<FileUploadRequest> files = fileRetriever.getFiles(message);
//        List<UUID> attachmentIds = uploadFiles(files, message.getSender().getIdentifier(), getResourceId(message));
//
//        InitializeCorrespondencesExt correspondence = correspondenceFactory.create(message, attachmentIds);
//
//        var res =  client.initializeCorrespondence(correspondence);
//
//        return res;
//    }
//
//    private String getResourceId(NextMoveOutMessage message){
//        ServiceRecord serviceRecord = helper.getServiceRecord(message);
//
//        return serviceRecord.getService().getResource();
//    }
//
//    private List<UUID> uploadFiles(List<FileUploadRequest> files, String senderIdentifier, String resourceId){
//        return files.stream()
//            .map(file -> uploadFile(file, senderIdentifier, resourceId))
//            .toList();
//    }
//
//    private UUID uploadFile(FileUploadRequest file, String senderIdentifier, String resourceId){
//        InitializeAttachmentExt request = new InitializeAttachmentExt();
//        BusinessMessageFile metadata = file.getBusinessMessageFile();
//
//        request.isEncrypted(false);
//        request.setFileName(metadata.getFilename());
//        request.setSendersReference("AttachmentReference_as123452");
//        request.setDisplayName(metadata.getTitle());
//        request.setSender(senderIdentifier);
//        request.setResourceId(resourceId);
//
//        UUID attachmentId = client.initializeAttachment(request);
//        try {
//            client.uploadAttachment(attachmentId, file.getFile().getInputStream().readAllBytes());
//        } catch (IOException e) {
//            throw new CorrespondenceApiException("Error", e);
//        }
//        return attachmentId;
//    }
}
