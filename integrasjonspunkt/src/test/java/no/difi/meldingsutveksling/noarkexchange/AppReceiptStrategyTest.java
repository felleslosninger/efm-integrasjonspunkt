package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
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

    /**
     * The AppReceiptStrategy should only log the message to the eventlog,
     * and return an OK response type
     */
    @Test
    public void shouldOnlyLogApplicationReceipts() {

        PutMessageContext ctx = new PutMessageContext(Mockito.mock(EventLog.class), Mockito.mock(MessageSender.class));

        AppReceiptPutMessageStrategy strategy = new AppReceiptPutMessageStrategy(ctx);
        strategy.setContext(ctx);

        PutMessageRequestType request = new PutMessageRequestType();
        request.setPayload(receiptPayload);

        PutMessageResponseType response = strategy.putMessage(request);
        assertTrue(response.getResult() != null);
        verify(ctx.getEventlog(), times(1)).log(any(Event.class));
        assertTrue(response.getResult() != null);
        // message sender should not be called for receipts, only log
        verify(ctx.getMessageSender(), times(0)).sendMessage(any(PutMessageRequestType.class));
    }
}
