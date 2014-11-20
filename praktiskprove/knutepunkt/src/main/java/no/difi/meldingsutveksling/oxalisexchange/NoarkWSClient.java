package no.difi.meldingsutveksling.oxalisexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.NoarkExchange;
import no.difi.meldingsutveksling.noarkexchange.schema.SOAPport;

/**
 * Created by kubkaray on 19.11.2014.
 */
public class NoarkWSClient   {
    NoarkExchange noarkExchange = new NoarkExchange();
    SOAPport soaPport;
    public NoarkWSClient () {
        soaPport= noarkExchange.getNoarkExchangePort();

        System.out.println(soaPport.getCanReceiveMessage(new GetCanReceiveMessageRequestType()));
    }
}
