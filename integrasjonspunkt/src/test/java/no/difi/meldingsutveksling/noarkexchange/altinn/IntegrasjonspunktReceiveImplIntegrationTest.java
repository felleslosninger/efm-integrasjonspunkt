package no.difi.meldingsutveksling.noarkexchange.altinn;

import no.difi.meldingsutveksling.IntegrasjonspunktApplication;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.vefa.peppol.lookup.api.LookupException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
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

    @Test
    public void receiveMessageTest() throws LookupException, JAXBException, IOException, MessageException {
        AppReceiptType appReceiptTypeMock = mock(AppReceiptType.class);
        when(appReceiptTypeMock.getType()).thenReturn("OK");
        PutMessageResponseType putMessageResponseTypeMock = mock(PutMessageResponseType.class);
        when(putMessageResponseTypeMock.getResult()).thenReturn(appReceiptTypeMock);
        NoarkClient noarkClientMock = mock(NoarkClient.class);
        when(noarkClientMock.sendEduMelding(any(PutMessageRequestType.class))).thenReturn(putMessageResponseTypeMock);
        integrajonspunktReceive.setLocalNoark(noarkClientMock);

        IntegrajonspunktReceiveImpl integrajonspunktReceiveSpy = spy(integrajonspunktReceive);
        doReturn("42".getBytes()).when(integrajonspunktReceiveSpy).decrypt(any(Payload.class));
        PutMessageRequestType putMessageRequestTypeMock = mock(PutMessageRequestType.class);
        doReturn(putMessageRequestTypeMock).when(integrajonspunktReceiveSpy).convertAsicEntrytoEduDocument(any(byte[].class));
        doNothing().when(integrajonspunktReceiveSpy).sendReceiptOpen(any(StandardBusinessDocumentWrapper.class));
        internalQueue.setIntegrajonspunktReceiveImpl(integrajonspunktReceiveSpy);

        JAXBElement<StandardBusinessDocument> fromDocument;
        JAXBContext ctx = JAXBContext.newInstance(no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        fromDocument = unmarshaller.unmarshal(new StreamSource(getClass().getClassLoader().getResourceAsStream("1466595652965.xml")), no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class);
        EduDocument eduDocument = StandardBusinessDocumentFactory.create(fromDocument.getValue());

        internalQueue.forwardToNoark(eduDocument);

        verify(integrajonspunktReceiveSpy).forwardToNoarkSystemAndSendReceipts(any(StandardBusinessDocumentWrapper.class), any(PutMessageRequestType.class));
    }

}
