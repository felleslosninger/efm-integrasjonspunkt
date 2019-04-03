package no.difi.meldingsutveksling.noarkexchange.altinn;

import no.difi.meldingsutveksling.IntegrasjonspunktApplication;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.core.Sender;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.IntegrajonspunktReceiveImpl;
import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktIntegrationTestConfig;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration test for {@link IntegrajonspunktReceiveImpl} (via {@link InternalQueue}).
 * <p>
 * Mock overrides are configured in {@link no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktIntegrationTestConfig}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {
        IntegrasjonspunktApplication.class,
        IntegrasjonspunktIntegrationTestConfig.class
}, webEnvironment = RANDOM_PORT, properties = {"app.local.properties.enable=false"})
@ActiveProfiles("test")
public class IntegrasjonspunktReceiveImplIntegrationTest {

    @Autowired
    InternalQueue internalQueue;

    @Autowired
    IntegrajonspunktReceiveImpl integrajonspunktReceive;

    IntegrajonspunktReceiveImpl integrajonspunktReceiveSpy;

    @Before
    public void setUp() throws MessageException {
        AppReceiptType appReceiptTypeMock = mock(AppReceiptType.class);
        when(appReceiptTypeMock.getType()).thenReturn("OK");
        PutMessageResponseType putMessageResponseTypeMock = mock(PutMessageResponseType.class);
        when(putMessageResponseTypeMock.getResult()).thenReturn(appReceiptTypeMock);
        NoarkClient noarkClientMock = mock(NoarkClient.class);
        when(noarkClientMock.sendEduMelding(any(PutMessageRequestType.class))).thenReturn(putMessageResponseTypeMock);
        integrajonspunktReceive.setLocalNoark(noarkClientMock);

        integrajonspunktReceiveSpy = spy(integrajonspunktReceive);
        doReturn("42".getBytes()).when(integrajonspunktReceiveSpy).decrypt(any(Payload.class));

        Sender senderMock = mock(Sender.class);
        when(senderMock.getIdentifier()).thenReturn("42");
        when(senderMock.getName()).thenReturn("foo");
        Receiver receiverMock = mock(Receiver.class);
        when(receiverMock.getIdentifier()).thenReturn("42");
        when(receiverMock.getName()).thenReturn("foo");
        EDUCore requestMock = mock(EDUCore.class);
        when(requestMock.getSender()).thenReturn(senderMock);
        when(requestMock.getReceiver()).thenReturn(receiverMock);

        doReturn(requestMock).when(integrajonspunktReceiveSpy).convertAsicEntrytoEduCore(any(byte[].class));
        doNothing().when(integrajonspunktReceiveSpy).sendReceiptOpen(any(StandardBusinessDocument.class));
        internalQueue.setIntegrajonspunktReceiveImpl(integrajonspunktReceiveSpy);
    }

    @Test
    public void receiveMessageTest() throws JAXBException {
        StandardBusinessDocument sbd = readSBD("1466595652965.xml");

        internalQueue.forwardToNoark(sbd);

        verify(integrajonspunktReceiveSpy).forwardToNoarkSystemAndSendReceipts(any(StandardBusinessDocument.class), any(EDUCore.class));
    }

    private static StandardBusinessDocument readSBD(String filename) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(StandardBusinessDocument.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        return unmarshaller.unmarshal(new StreamSource(IntegrasjonspunktReceiveImplIntegrationTest.class.getClassLoader().getResourceAsStream(filename)), StandardBusinessDocument.class).getValue();
    }

}
