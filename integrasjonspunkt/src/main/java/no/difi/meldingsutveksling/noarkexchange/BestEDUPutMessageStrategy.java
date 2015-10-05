package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

/**
 * Created by kons-gbe on 05.10.2015.
 */
public class BestEDUPutMessageStrategy implements PutMessageStrategy {

    private PutMessageContext context;

    public BestEDUPutMessageStrategy(PutMessageContext context) {
        this.context = context;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType requestType) {
        return context.getMessageSender().sendMessage(requestType);
    }

}
