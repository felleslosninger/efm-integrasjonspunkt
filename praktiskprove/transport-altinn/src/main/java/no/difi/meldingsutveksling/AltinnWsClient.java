package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceExternalBasicSF;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceInitiation;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.shipping.Request;
import no.difi.meldingsutveksling.shipping.ws.AltinnWsException;
import no.difi.meldingsutveksling.shipping.ws.ManifestBuilder;
import no.difi.meldingsutveksling.shipping.ws.RecipientBuilder;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;

public class AltinnWsClient {
    private final URL url;

    public AltinnWsClient(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public void send(Request request) {
        // 1. Reference <-- Initiate broker service
        // 2. Receipt <-- UploadFileStreamed w. reference
        String senderReference = initiateBrokerService(request);
        System.out.println(senderReference);

    }

    private String initiateBrokerService(Request request) {
        BrokerServiceInitiation brokerServiceInitiation = createInitiationClient(request);
        try {
            BrokerServiceExternalBasicSF brokerService = new BrokerServiceExternalBasicSF(url, new QName("http://www.altinn.no/services/ServiceEngine/Broker/2015/06", "IBrokerServiceExternalBasicImplService"));
            return brokerService.getBasicHttpBindingIBrokerServiceExternalBasic().initiateBrokerServiceBasic("username", "password", brokerServiceInitiation);
        } catch (IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage iBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage) {
            throw new AltinnWsException(iBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage);
        }
    }

    private BrokerServiceInitiation createInitiationClient(Request request) {
        BrokerServiceInitiation brokerServiceInitiation = new BrokerServiceInitiation();

        ManifestBuilder manifestBuilder = new ManifestBuilder()
                .withSender(request.getSender())
                .withSenderReference(request.getSenderReference())
                .withFilename("content.xml");
        brokerServiceInitiation.setManifest(manifestBuilder.build());
        brokerServiceInitiation.setRecipientList(new RecipientBuilder().withPartyNumber(request.getReceiver()).build());

        return brokerServiceInitiation;
    }
}
