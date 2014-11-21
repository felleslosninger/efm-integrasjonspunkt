package no.difi.meldingsutveksling.oxalisexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.NoarkExchange;
import no.difi.meldingsutveksling.noarkexchange.schema.SOAPport;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kubkaray on 19.11.2014.
 */
public class NoarkWSClient   {
    NoarkExchange noarkExchange = new NoarkExchange();
    List listToRemoveAfterImpl= new ArrayList();
    SOAPport soaPport;
    public NoarkWSClient () {
        soaPport= noarkExchange.getNoarkExchangePort();
        listToRemoveAfterImpl.add(soaPport.getCanReceiveMessage(new GetCanReceiveMessageRequestType()));
    }
}
