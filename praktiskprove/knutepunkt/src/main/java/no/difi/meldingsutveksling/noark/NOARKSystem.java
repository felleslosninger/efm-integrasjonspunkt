package no.difi.meldingsutveksling.noark;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.noarkexchange.schema.NoarkExchange;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.SOAPport;

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
public class NOARKSystem {


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
        NoarkExchange exchange = new NoarkExchange();
        SOAPport port = exchange.getNoarkExchangePort();
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPointURL);
        return port.putMessage(eduMesage);
    }

    public static void main(String[] args) {
        NOARKSystem noark = new NOARKSystem();
        PutMessageResponseType response = noark.sendEduMeldig(new PutMessageRequestType());
        XStream xs = new XStream();
        System.out.println(xs.toXML(response));

    }

}
