package no.difi.meldingsutveksling.altinnv3.DPV;

import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.Service;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.digdir.altinn3.correspondence.model.BaseCorrespondenceExt;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondencesExt;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondencesResponseExt;
import no.digdir.altinn3.correspondence.model.InitializedCorrespondencesExt;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = AltinnDPVService.class)
public class AltinnUploadServiceTest {

    @Autowired
    private AltinnDPVService altinnUploadService;

    @MockitoBean
    private CorrespondenceApiClient correspondenceApiClient;

    @MockitoBean
    private CorrespondenceFactory correspondenceFactory;

    @MockitoBean
    private FileRetriever fileRetriever;

    @MockitoBean
    private Helper helper;

    private static final Iso6523 SENDER = Iso6523.of(ICD.NO_ORG, "111111111");
    private static final Iso6523 RECEIVER = Iso6523.of(ICD.NO_ORG, "222222222");

    @Test
    public void upload() {
        String resource = "test";
        UUID correspondenceId = UUID.randomUUID();
        InitializeCorrespondencesExt initializeCorrespondencesExt = new InitializeCorrespondencesExt();
        BaseCorrespondenceExt baseCorrespondenceExt = new BaseCorrespondenceExt();
        baseCorrespondenceExt.setResourceId(resource);
        initializeCorrespondencesExt.setCorrespondence(baseCorrespondenceExt);
        InitializedCorrespondencesExt response = new InitializedCorrespondencesExt();
        response.setCorrespondenceId(correspondenceId);
        InitializeCorrespondencesResponseExt response2 = new InitializeCorrespondencesResponseExt();
        response2.setCorrespondences(List.of(response));

        NextMoveOutMessage message = new NextMoveOutMessage();
        StandardBusinessDocument sbd = new StandardBusinessDocument()
            .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                .setSenderIdentifier(SENDER)
                .setReceiverIdentifier(RECEIVER)
            );
        message.setSbd(sbd);

        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.setService(new Service());


        Mockito.when(helper.getServiceRecord(Mockito.any())).thenReturn(serviceRecord);
        Mockito.when(fileRetriever.getFiles(Mockito.any())).thenReturn(List.of(new FileUploadRequest(new BusinessMessageFile(), null)));
        Mockito.when(correspondenceFactory.create(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(initializeCorrespondencesExt);
        Mockito.when(correspondenceApiClient.upload(Mockito.any(), Mockito.any())).thenReturn(response2);

        var result = altinnUploadService.send(message);

        assertEquals(correspondenceId, result, "The returned value needs to be the correspondence id of the correspondence");
        verify(correspondenceApiClient).upload(Mockito.any(), Mockito.any());
    }
}
