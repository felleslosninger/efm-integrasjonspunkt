package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


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

    @Test
    public void testGetTypeReturnsEduMessage() {
        PutMessageRequestType pmrt = createPutMessageRequestTypeEduStringPayload();
        PutMessageRequestWrapper w = new PutMessageRequestWrapper(pmrt);
        PutMessageRequestWrapper.MessageType type =  w.getMessageType();

        assertEquals(PutMessageRequestWrapper.MessageType.EDUMESSAGE, type);
    }

    @Test
    public void testGetTypeReturnsAppReceipt() {
        PutMessageRequestType pmrt = createPutMessageRequestTypeWithAppreceiptPayload();
        PutMessageRequestWrapper w = new PutMessageRequestWrapper(pmrt);
        PutMessageRequestWrapper.MessageType type =  w.getMessageType();

        assertEquals(PutMessageRequestWrapper.MessageType.APPRECEIPT, type);
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

    private PutMessageRequestType createPutMessageRequestTypeEduStringPayload() {
        PutMessageRequestType pmrt = new PutMessageRequestType();
        AddressType sender = new AddressType();
        sender.setOrgnr(SENDER);
        AddressType receiver = new AddressType();
        receiver.setOrgnr(RECEIVER);

        EnvelopeType env = new EnvelopeType();
        pmrt.setEnvelope(env);
        pmrt.getEnvelope().setSender(sender);
        pmrt.getEnvelope().setReceiver(receiver);

        pmrt.setPayload("&lt;?xml version=\"1.0\" encoding=\"utf-8\"?&gt;" +
                "&lt;Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"&gt;" +
                "  &lt;journpost xmlns=\"\"&gt;" +
                "    &lt;jpId&gt;315701&lt;/jpId&gt;" +
                "    &lt;jpJaar&gt;2016&lt;/jpJaar&gt;" +
                "    &lt;jpSeknr&gt;179&lt;/jpSeknr&gt;" +
                "    &lt;jpJpostnr&gt;19&lt;/jpJpostnr&gt;" +
                "    &lt;jpJdato&gt;0001-01-01&lt;/jpJdato&gt;" +
                "    &lt;jpNdoktype&gt;U&lt;/jpNdoktype&gt;" +
                "    &lt;jpDokdato&gt;2016-05-11&lt;/jpDokdato&gt;" +
                "    &lt;jpStatus&gt;R&lt;/jpStatus&gt;\n" +
                "    &lt;jpInnhold&gt;logge melding&lt;/jpInnhold&gt;" +
                "    &lt;jpForfdato /&gt;" +
                "    &lt;jpTgkode&gt;U&lt;/jpTgkode&gt;" +
                "    &lt;jpAgdato /&gt;" +
                "    &lt;jpAntved /&gt;" +
                "    &lt;jpSaar&gt;2015&lt;/jpSaar&gt;" +
                "    &lt;jpSaseknr&gt;944&lt;/jpSaseknr&gt;" +
                "    &lt;jpOffinnhold&gt;logge melding&lt;/jpOffinnhold&gt;" +
                "    &lt;jpTggruppnavn&gt;Alle&lt;/jpTggruppnavn&gt;" +
                "  &lt;/journpost&gt;");

        return pmrt;
    }

    //TODO: Add test for ePhorte messages

    private PutMessageRequestType createPutMessageRequestTypeWithAppreceiptPayload() {
        PutMessageRequestType pmrt = new PutMessageRequestType();
        AddressType sender = new AddressType();
        sender.setOrgnr(SENDER);
        AddressType receiver = new AddressType();
        receiver.setOrgnr(RECEIVER);

        EnvelopeType env = new EnvelopeType();
        pmrt.setEnvelope(env);
        pmrt.getEnvelope().setSender(sender);
        pmrt.getEnvelope().setReceiver(receiver);

        pmrt.setPayload("&lt;AppReceipt type=\"OK\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark/Exchange/types\"&gt;" +
                "  &lt;message code=\"ID\" xmlns=\"\"&gt;" +
                "    &lt;text&gt;210725&lt;/text&gt;" +
                "  &lt;/message&gt;" +
                "&lt;/AppReceipt&gt;");

        return pmrt;
    }
}
