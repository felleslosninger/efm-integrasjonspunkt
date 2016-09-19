package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for the AppReceiptStrategy
 */
public class AppReceiptStrategyTest {

    public static final String RECEIVER_ORG_NR = "11111111";
    public static final String SENDER_ORG_NR = "22222222";
    private String receiptPayload = "<AppReceipt type=\"OK\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark/Exchange/types\">\n" +
            "  <message code=\"ID\" xmlns=\"\">\n" +
            "    <text>210725</text>\n" +
            "  </message>\n" +
            "</AppReceipt>";

    private MessageSender messageSender;

    @Before
    public void init() {
        messageSender = Mockito.mock(MessageSender.class);
    }

    /**
     * The AppReceiptStrategy should send the receipt back to the sender
     */
    @Test
    public void appReceiptsShouldBeReturnedToSender() {
        AppReceiptMessageStrategy strategy = new AppReceiptMessageStrategy(messageSender);
        PutMessageRequestType request = createPutMessageRequestType();

        ServiceRegistryLookup srMock = mock(ServiceRegistryLookup.class);
        InfoRecord senderInfoRecord = new InfoRecord();
        InfoRecord receiverInfoRecord = new InfoRecord();
        senderInfoRecord.setOrganisationNumber(SENDER_ORG_NR);
        senderInfoRecord.setOrganizationName("foo");
        receiverInfoRecord.setOrganisationNumber(RECEIVER_ORG_NR);
        receiverInfoRecord.setOrganisationNumber("bar");
        when(srMock.getInfoRecord(SENDER_ORG_NR)).thenReturn(senderInfoRecord);
        when(srMock.getInfoRecord(RECEIVER_ORG_NR)).thenReturn(receiverInfoRecord);

        EDUCoreFactory eduCoreFactory = new EDUCoreFactory(srMock);
        EDUCore message = eduCoreFactory.create(request, SENDER_ORG_NR);
        strategy.putMessage(message);
        verify(messageSender, atLeastOnce()).sendMessage(any(EDUCore.class));
    }

    private PutMessageRequestType createPutMessageRequestType() {
        PutMessageRequestType request = new PutMessageRequestType();
        final EnvelopeType envelope = new EnvelopeType();
        AddressType receiver = new AddressType();
        receiver.setOrgnr(RECEIVER_ORG_NR);
        envelope.setReceiver(receiver);

        AddressType sender = new AddressType();
        sender.setOrgnr(SENDER_ORG_NR);
        envelope.setSender(sender);

        request.setEnvelope(envelope);
        request.setPayload(receiptPayload);
        return request;
    }
}
