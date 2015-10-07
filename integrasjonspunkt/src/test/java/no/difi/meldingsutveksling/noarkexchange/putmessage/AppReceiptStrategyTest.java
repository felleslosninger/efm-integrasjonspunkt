package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.putmessage.AppReceiptPutMessageStrategy;
import no.difi.meldingsutveksling.noarkexchange.putmessage.PutMessageContext;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
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
        AppReceiptPutMessageStrategy strategy = new AppReceiptPutMessageStrategy(ctx.getEventlog());
        PutMessageRequestType request = new PutMessageRequestType();
        request.setPayload(receiptPayload);

        PutMessageResponseType response = strategy.putMessage(request);
        verify(ctx.getEventlog()).log(any(Event.class));
        // message sender should not be called for receipts, only log
        verify(ctx.getMessageSender(), never()).sendMessage(any(PutMessageRequestType.class));
    }
}
