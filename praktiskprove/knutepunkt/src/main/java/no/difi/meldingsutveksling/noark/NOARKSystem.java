package no.difi.meldingsutveksling.noark;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.eventlog.ProcessState;
import no.difi.meldingsutveksling.noarkexchange.schema.NoarkExchange;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.SOAPport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import javax.xml.ws.BindingProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple wrapper around the Web Service client for a knutepunkt. Reads the end point URL
 * from a property file and sends a request to the given URL.
 *
 * @author Glenn Bech
 */

@Component
public class NOARKSystem {

    @Autowired
    EventLog eventLog;

    public static final String KNUTEPUNKT_PROPERTIES = "knutepunkt.properties";
    public static final String NOARKSYSTEM_ENDPOINT = "noarksystem.endpointURL";
    private String endPointURL;

    public NOARKSystem() {

        Properties p;
        try (InputStream is = NOARKSystem.class.getClassLoader().getResourceAsStream(KNUTEPUNKT_PROPERTIES)) {
            if (is == null) {
                throw new IllegalStateException(KNUTEPUNKT_PROPERTIES + " is not on classpath");
            }
            p = new Properties();
            p.load(is);

        } catch (IOException e) {
            throw new IllegalStateException(KNUTEPUNKT_PROPERTIES + " can not be read");
        }
        endPointURL = p.getProperty(NOARKSYSTEM_ENDPOINT);
    }

    public PutMessageResponseType sendEduMeldig(PutMessageRequestType eduMesage) {
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
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPointURL);
        return port.putMessage(eduMesage);
    }


    public EventLog getEventLog() {
        return eventLog;
    }

    public void setEventLog(EventLog eventLog) {
        this.eventLog = eventLog;
    }

    /**
     * TODO Remove
     * Use this to test the actual sending (see knutepunkt.properties for endpoint)
     *
     * @param args
     */
    public static void main(String[] args) {
        XStream xs = new XStream();
        System.setProperty("spring.profiles.active", "dev");
        ApplicationContext ctx = new ClassPathXmlApplicationContext("spring-rest.xml");
        NOARKSystem noark = new NOARKSystem();
        noark.setEventLog(ctx.getBean(EventLog.class));
        PutMessageResponseType response = noark.sendEduMeldig(new PutMessageRequestType());
        System.out.println(xs.toXML(response));
    }

}
