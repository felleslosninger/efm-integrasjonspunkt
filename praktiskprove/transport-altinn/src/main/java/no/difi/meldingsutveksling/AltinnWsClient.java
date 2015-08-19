package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceExternalBasicSF;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceInitiation;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.InitiateBrokerServiceBasic;
import no.difi.meldingsutveksling.shipping.Request;
import no.difi.meldingsutveksling.shipping.ws.AltinnWsException;
import no.difi.meldingsutveksling.shipping.ws.ManifestBuilder;
import no.difi.meldingsutveksling.shipping.ws.RecipientBuilder;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;

public class AltinnWsClient {

    public void send(Request request) {
        // 1. Reference <-- Initiate broker service
        // 2. Receipt <-- UploadFileStreamed w. reference
        BrokerServiceInitiation brokerServiceInitiation = createInitiationClient(request);
        try {
            BrokerServiceExternalBasicSF brokerService = new BrokerServiceExternalBasicSF(new URL("http://localhost:7777/altinn/receipt"), new QName("http://www.altinn.no/services/Intermediary/Receipt/2009/10", "IReceiptExternalBasicImplService"));
            brokerService.getBasicHttpBindingIBrokerServiceExternalBasic().initiateBrokerServiceBasic("username", "password", brokerServiceInitiation);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage iBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage) {
            throw new AltinnWsException(iBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage);
        }
    }

    private BrokerServiceInitiation createInitiationClient(Request request) {
        InitiateBrokerServiceBasic initiateBrokerServiceBasic = new InitiateBrokerServiceBasic();
        initiateBrokerServiceBasic.setSystemUserName("username");
        initiateBrokerServiceBasic.setSystemPassword("password");
        BrokerServiceInitiation brokerServiceInitiation = initiateBrokerServiceBasic.getBrokerServiceInitiation();

        ManifestBuilder manifestBuilder = new ManifestBuilder()
                .withSender(request.getSender())
                .withSenderReference(request.getSenderReference())
                .withFilename("content.xml");
        brokerServiceInitiation.setManifest(manifestBuilder.build());
        brokerServiceInitiation.setRecipientList(new RecipientBuilder().withPartyNumber("12345678").build());
        return brokerServiceInitiation;
    }
}
