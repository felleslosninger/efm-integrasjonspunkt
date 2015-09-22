package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import com.sun.istack.internal.NotNull;
import com.sun.xml.internal.bind.api.JAXBRIContext;
import com.sun.xml.internal.bind.api.TypeReference;
import com.sun.xml.internal.ws.api.model.SEIModel;
import com.sun.xml.internal.ws.developer.JAXBContextFactory;
import com.sun.xml.internal.ws.developer.UsesJAXBContextFeature;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocumentHeader;

import javax.xml.bind.JAXBException;
import javax.xml.ws.BindingProvider;
import java.util.List;

/**
 * This class is responsible for calleing the web service method "receive" on the
 * "integrasjonspunkt" that is receiving the document
 *
 * @author Glenn Bech
 */
class ReceiveClient {

    private SOAReceivePort port;

    public ReceiveClient(String endPointURL) {
        no.difi.meldingsutveksling.noarkexchange.schema.receive.Receive exchange = new no.difi.meldingsutveksling.noarkexchange.schema.receive.Receive();

        UsesJAXBContextFeature jaxbContxtFeature = new UsesJAXBContextFeature(new JAXBContextFactory() {
            @Override
            public JAXBRIContext createJAXBContext(@NotNull SEIModel seiModel, @NotNull List<Class> classes, @NotNull List<TypeReference> typeReferences) throws JAXBException {
                classes.add(Payload.class);
                return JAXBContextFactory.DEFAULT.createJAXBContext(seiModel, classes, typeReferences);
            }
        });

        port = exchange.getReceivePort(jaxbContxtFeature);
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPointURL);
    }

    public CorrelationInformation callReceive(StandardBusinessDocument sbd) {
        return port.receive(sbd);
    }


    public static void main(String[] args) {

        StandardBusinessDocument sbd = new StandardBusinessDocument();
        sbd.setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader());
        sbd.setAny(new Payload(new byte[10]));

        ReceiveClient rc = new ReceiveClient("http://localhost:8080/noarkExchange");
        rc.callReceive(sbd) ;

    }

}
