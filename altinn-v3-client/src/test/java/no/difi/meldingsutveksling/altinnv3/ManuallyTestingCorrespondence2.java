package no.difi.meldingsutveksling.altinnv3;


import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.dpv.CorrespondenceApiClient;
import no.difi.meldingsutveksling.altinnv3.dpv.DotNotationFlattener;
import no.difi.meldingsutveksling.altinnv3.dpv.FileUploadRequest;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.digdir.altinn3.correspondence.model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import java.nio.file.Files;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Disabled
@SpringBootTest(classes = {
    AltinnConfig.class,
    CorrespondenceApiClient.class,
    AltinnTokenUtil.class,
    IntegrasjonspunktProperties.class,
    DotNotationFlattener.class,
})
@ConfigurationPropertiesScan
public class ManuallyTestingCorrespondence2 {

    @Inject
    CorrespondenceApiClient client;

    @Test
    public void getAttachmentDetails(){
        var res = client.getAttachmentDetails(UUID.fromString("0197c4ef-9f81-7a05-bf84-9333c169fd28"));
        System.out.println(res);
    }

    @Test
    public void fileFirstThenCorrespondence() {
        InitializeAttachmentExt request = new InitializeAttachmentExt();

        var isEncrypted = false;
        var fileName = "Testfile3.txt";
        var senderReference = "string";
        var displayname = "test";


        request.isEncrypted(isEncrypted); //idk
        request.setFileName(fileName);
        request.sender("0192:991825827");
        request.resourceId("eformidling-meldingsteneste-test");
        request.setSendersReference(senderReference);
        request.setDisplayName(displayname);

        UUID attatchmentId = client.initializeAttachment(request);
        var res = client.uploadAttachment(attatchmentId, "Just some test data: summertime".getBytes());


        InitializeCorrespondencesExt request2 = new InitializeCorrespondencesExt();
        BaseCorrespondenceExt correspondence = new BaseCorrespondenceExt();
        correspondence.setResourceId("eformidling-meldingsteneste-test");
        correspondence.setSender("0192:991825827");
        correspondence.setSendersReference(senderReference);
        correspondence.setIsConfirmationNeeded(false);
        correspondence.setRequestedPublishTime(OffsetDateTime.now().plusMinutes(5));


        InitializeCorrespondenceContentExt content = new InitializeCorrespondenceContentExt();
        content.setLanguage("nb");
        content.setMessageTitle("Testmelding fra Digdir");
        content.setMessageBody("Testmelding fra Digdir");
        content.setMessageSummary("Testmelding fra Digdir");

        request2.setExistingAttachments(List.of(attatchmentId));

        correspondence.setContent(content);

        request2.setCorrespondence(correspondence);
        request2.setRecipients(List.of("urn:altinn:organization:identifier-no:310654302"));


        var res2 = client.initializeCorrespondence(request2);
    }

    @Test
    public void uploadAllAtTheSameTime() throws IOException {

        var fileName = "Testfile3.txt";
        var senderReference = "string";
        var displayname = "test";


        var attachment = new InitializeCorrespondenceAttachmentExt();
        attachment.setIsEncrypted(false);
        attachment.setFileName(fileName);
        attachment.setSendersReference(senderReference);
        attachment.setDisplayName(displayname);
        attachment.setDataLocationType(InitializeAttachmentDataLocationTypeExt.NEW_CORRESPONDENCE_ATTACHMENT);


        InitializeCorrespondencesExt request2 = new InitializeCorrespondencesExt();
        BaseCorrespondenceExt correspondence = new BaseCorrespondenceExt();
        correspondence.setResourceId("eformidling-meldingsteneste-test");
        correspondence.setSender("0192:991825827");
        correspondence.setSendersReference(senderReference);
        correspondence.setIsConfirmationNeeded(false);
        correspondence.setRequestedPublishTime(OffsetDateTime.now().plusMinutes(5));


        InitializeCorrespondenceContentExt content = new InitializeCorrespondenceContentExt();
        content.setLanguage("nb");
        content.setMessageTitle("Testmelding fra Digdir");
        content.setMessageBody("Testmelding fra Digdir");
        content.setMessageSummary("Testmelding fra Digdir");
        content.setAttachments(List.of(attachment));

        correspondence.setContent(content);

        request2.setCorrespondence(correspondence);
        request2.setRecipients(List.of("urn:altinn:organization:identifier-no:314244370"));


        String filename = "Testfile3.txt";
        String tempDir = System.getProperty("java.io.tmpdir");
        File file = new File(tempDir, filename);

        Files.write(file.toPath(), ("This is test file a").getBytes());
        Resource resource = new FileSystemResource(file);

        FileUploadRequest fileUploadRequest = new FileUploadRequest(new BusinessMessageFile().setFilename(filename), resource);

        var res = client.upload(request2, List.of( fileUploadRequest ));

    }

    @Test
    public void getCorrespondenceDetails() {
        var res = client.getCorrespondenceDetails(UUID.fromString("0197ef56-0519-7157-b819-bfa65777569a"));
        System.out.println(res);
    }

    @Test
    public void downloadAttachment(){
        var res = client.downloadAttachment(UUID.fromString("0197ef56-0519-7157-b819-bfa65777569a"), UUID.fromString("0197ef56-04de-7713-93b1-d5562e4e261b"));

        String message = new String(res);
        System.out.println(message);
    }
}
