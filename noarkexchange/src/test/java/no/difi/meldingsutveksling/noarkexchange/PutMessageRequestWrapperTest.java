package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PutMessageRequestWrapperTest {

    public static final String SENDER = "111111111";
    public static final String RECEIVER = "222222222";

    @Test
    public void testSwapSenderAndReceiver() {
        PutMessageRequestType pmrt = createPutMessageRequestType();
        PutMessageRequestWrapper w = new PutMessageRequestWrapper(pmrt);
        w.swapSenderAndReceiver();
        assertEquals(RECEIVER, w.getRequest().getEnvelope().getSender().getOrgnr());
        assertEquals(SENDER, w.getRequest().getEnvelope().getReceiver().getOrgnr());
    }

    private PutMessageRequestType createPutMessageRequestType() {
        PutMessageRequestType pmrt = new PutMessageRequestType();
        AddressType sender = new AddressType();
        sender.setOrgnr(SENDER);
        AddressType receiver = new AddressType();
        receiver.setOrgnr(RECEIVER);
        EnvelopeType env = new EnvelopeType();
        pmrt.setEnvelope(env);
        pmrt.getEnvelope().setSender(sender);
        pmrt.getEnvelope().setReceiver(receiver);
        return pmrt;
    }
}
