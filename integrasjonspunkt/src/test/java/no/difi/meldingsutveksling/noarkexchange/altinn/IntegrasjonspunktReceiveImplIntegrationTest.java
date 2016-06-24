package no.difi.meldingsutveksling.noarkexchange.altinn;

import no.difi.meldingsutveksling.IntegrasjonspunktApplication;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.bind.JAXBException;
import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * Integration test for {@link IntegrajonspunktReceiveImpl} (via {@link InternalQueue}).
 *
 * Mock overrides are configured in {@link no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktIntegrationTestConfig}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(IntegrasjonspunktApplication.class)
@WebIntegrationTest
@ActiveProfiles("test")
public class IntegrasjonspunktReceiveImplIntegrationTest {

    @Autowired
    InternalQueue internalQueue;

    @Autowired
    IntegrajonspunktReceiveImpl integrajonspunktReceive;

    IntegrajonspunktReceiveImpl integrajonspunktReceiveSpy;

    @Before
    public void setUp() throws JAXBException, IOException, MessageException {
        AppReceiptType appReceiptTypeMock = mock(AppReceiptType.class);
        when(appReceiptTypeMock.getType()).thenReturn("OK");
        PutMessageResponseType putMessageResponseTypeMock = mock(PutMessageResponseType.class);
        when(putMessageResponseTypeMock.getResult()).thenReturn(appReceiptTypeMock);
        NoarkClient noarkClientMock = mock(NoarkClient.class);
        when(noarkClientMock.sendEduMelding(any(PutMessageRequestType.class))).thenReturn(putMessageResponseTypeMock);
        integrajonspunktReceive.setLocalNoark(noarkClientMock);

        integrajonspunktReceiveSpy = spy(integrajonspunktReceive);
        doReturn("42".getBytes()).when(integrajonspunktReceiveSpy).decrypt(any(Payload.class));
        PutMessageRequestType putMessageRequestTypeMock = mock(PutMessageRequestType.class);
        doReturn(putMessageRequestTypeMock).when(integrajonspunktReceiveSpy).convertAsicEntrytoEduDocument(any(byte[].class));
        doNothing().when(integrajonspunktReceiveSpy).sendReceiptOpen(any(StandardBusinessDocumentWrapper.class));
        internalQueue.setIntegrajonspunktReceiveImpl(integrajonspunktReceiveSpy);
    }

    @Test
    public void receiveMessageTest() throws JAXBException {
        EduDocument eduDocument = SBDFileReader.readSBD("1466595652965.xml");

        internalQueue.forwardToNoark(eduDocument);

        verify(integrajonspunktReceiveSpy).forwardToNoarkSystemAndSendReceipts(any(StandardBusinessDocumentWrapper.class), any(PutMessageRequestType.class));
    }

}
