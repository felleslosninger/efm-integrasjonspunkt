package no.difi.meldingsutveksling.noark;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.ws.BindingProvider;

/**
 * Simple wrapper around the Web Service client for a integrasjonspunkt. Reads the end point URL
 * from a property file and sends a request to the given URL.
 *
 * @author Glenn Bech
 */
public class NoarkClient {

    @Autowired
    EventLog eventLog;

    IntegrasjonspunktConfig.NoarkClientSettings settings;

    public PutMessageResponseType sendEduMelding(PutMessageRequestType eduMesage) {
        if (eventLog == null) {
            throw new IllegalStateException("malconfigured. EventLog Not set");
        }

        if (eduMesage.getEnvelope() == null || eduMesage.getEnvelope().getReceiver() == null || eduMesage.getEnvelope().getSender() == null) {
            eventLog.log(Event.errorEvent("", "", ProcessState.MESSAGE_SEND_FAIL, "invalid envelope", new XStream().toXML(eduMesage)));
            throw new IllegalStateException("invalid envelope");
        }

        SOAPport port = getSoapPport();
        return port.putMessage(eduMesage);
    }

    private SOAPport getSoapPport() {
        NoarkExchange exchange = new NoarkExchange();
        SOAPport port = exchange.getNoarkExchangePort();
        BindingProvider bp = (BindingProvider) port;
        String endPointURL = settings.getEndpointUrl();
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPointURL);
        return port;
    }


    public IntegrasjonspunktConfig.NoarkClientSettings getSettings() {
        return settings;
    }

    public void setSettings(IntegrasjonspunktConfig.NoarkClientSettings settings) {
        this.settings = settings;
    }

    public EventLog getEventLog() {
        return eventLog;
    }

    public void setEventLog(EventLog eventLog) {
        this.eventLog = eventLog;
    }

    public boolean canGetRecieveMessage(String orgnr) {
        GetCanReceiveMessageRequestType req = new GetCanReceiveMessageRequestType();
        AddressType addressType = new AddressType();
        addressType.setOrgnr(orgnr);
        req.setReceiver(addressType);
        GetCanReceiveMessageResponseType responseType = getSoapPport().getCanReceiveMessage(req);
        return responseType.isResult();
    }
}
