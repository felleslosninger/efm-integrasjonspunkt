package no.difi.meldingsutveksling.noark;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.NoarkExchange;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.SOAPport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.ws.BindingProvider;

/**
 * Simple wrapper around the Web Service client for a integrasjonspunkt. Reads the end point URL
 * from a property file and sends a request to the given URL.
 *
 * @author Glenn Bech
 */

@Component
public class NoarkClient {

    @Autowired
    EventLog eventLog;


    @Autowired
    IntegrasjonspunktConfig config;

    public PutMessageResponseType sendEduMelding(PutMessageRequestType eduMesage) {
        if (eventLog == null) {
            throw new IllegalStateException("malconfigured. EventLog Not set");
        }

        if (eduMesage.getEnvelope() == null || eduMesage.getEnvelope().getReceiver() == null || eduMesage.getEnvelope().getSender() == null) {
            eventLog.log(Event.errorEvent("", "", ProcessState.MESSAGE_SEND_FAIL, "invalid envelope", new XStream().toXML(eduMesage)));
            throw new IllegalStateException("invalid envelope");
        }

        NoarkExchange exchange = new NoarkExchange();
        SOAPport port = exchange.getNoarkExchangePort();
        BindingProvider bp = (BindingProvider) port;
        String endPointURL = config.getNOARKSystemEndPointURL();
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPointURL);
        return port.putMessage(eduMesage);
    }


    public IntegrasjonspunktConfig getConfig() {
        return config;
    }

    public void setConfig(IntegrasjonspunktConfig config) {
        this.config = config;
    }

    public EventLog getEventLog() {
        return eventLog;
    }

    public void setEventLog(EventLog eventLog) {
        this.eventLog = eventLog;
    }
}
