package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Test for hte AppReceiptStrategy
 *
 * @author Glenn Bech
 */
public class AppReceiptStrategyTest {

    private String receiptPayload = "&lt;AppReceipt type=\"OK\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark/Exchange/types\"&gt;\n" +
            "  &lt;message code=\"ID\" xmlns=\"\"&gt;\n" +
            "    &lt;text&gt;210725&lt;/text&gt;\n" +
            "  &lt;/message&gt;\n" +
            "&lt;/AppReceipt&gt;";

    private PutMessageContext ctx;

    @Before
    public void init() {
        ctx = new PutMessageContext(Mockito.mock(EventLog.class), Mockito.mock(MessageSender.class));
    }

    /**
     * The AppReceiptStrategy should send the receipt back to the sender
     * and return an OK response type
     */
    @Test
    public void appReceiptsShouldBeReturnedToSender() {
        AppReceiptPutMessageStrategy strategy = new AppReceiptPutMessageStrategy(ctx);
        PutMessageRequestType request = new PutMessageRequestType();
        request.setPayload(receiptPayload);

        strategy.putMessage(request);

        verify(ctx.getMessageSender(), atLeastOnce()).sendMessage(any(PutMessageRequestType.class));
    }
}
