package no.difi.meldingsutveksling.cucumber;

import no.digipost.api.representations.EbmsContext;
import no.digipost.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageInfo;
import no.digipost.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;

public class FakeEbmsClientInterceptor extends ClientInterceptorAdapter {

    @Override
    public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
        EbmsContext context = EbmsContext.from(messageContext);
        SignalMessage signalMessage = new SignalMessage();
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setMessageId("1");
        messageInfo.setRefToMessageId("1");
        signalMessage.setMessageInfo(messageInfo);
        context.receipts.add(signalMessage);
        return true;
    }
}
