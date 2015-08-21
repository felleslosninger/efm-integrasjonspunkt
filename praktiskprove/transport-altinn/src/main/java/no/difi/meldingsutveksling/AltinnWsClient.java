package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceExternalBasicSF;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceInitiation;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.BrokerServiceExternalBasicStreamedSF;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.StreamedPayloadBasicBE;
import no.difi.meldingsutveksling.shipping.Request;
import no.difi.meldingsutveksling.shipping.ws.AltinnWsException;
import no.difi.meldingsutveksling.shipping.ws.ManifestBuilder;
import no.difi.meldingsutveksling.shipping.ws.RecipientBuilder;

import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        BrokerServiceExternalBasicStreamedSF brokerServiceExternalBasicStreamedSF = null;
        try {
            //MTOMFeature feature = new MTOMFeature(true, 128);
            brokerServiceExternalBasicStreamedSF = new BrokerServiceExternalBasicStreamedSF(new URL("http://localhost:7777/altinn/streamedmessage"), new QName("http://www.altinn.no/services/ServiceEngine/Broker/2015/06", "BrokerServiceExternalBasicStreamedSF"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        IBrokerServiceExternalBasicStreamed streamingService = brokerServiceExternalBasicStreamedSF.getBasicHttpBindingIBrokerServiceExternalBasicStreamed();

        try {
            //brokerServiceExternalBasicStreamedSF.
            StreamedPayloadBasicBE parameters = new StreamedPayloadBasicBE();
            AltinnPackage altinnPackage = AltinnPackage.from(request);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(65536);
            altinnPackage.write(outputStream);
            parameters.setDataStream(outputStream.toByteArray());

            //parameters.setDataStream();
            streamingService.uploadFileStreamedBasic(parameters, "sbd.xml", senderReference, request.getSender(), "password", "username");
        } catch (IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String initiateBrokerService(Request request) {
        BrokerServiceInitiation brokerServiceInitiation = createInitiationRequest(request);
        try {
            BrokerServiceExternalBasicSF brokerService = new BrokerServiceExternalBasicSF(url, new QName("http://www.altinn.no/services/ServiceEngine/Broker/2015/06", "IBrokerServiceExternalBasicImplService"));
            return brokerService.getBasicHttpBindingIBrokerServiceExternalBasic().initiateBrokerServiceBasic("username", "password", brokerServiceInitiation);
        } catch (IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(e);
        }
    }

    private BrokerServiceInitiation createInitiationRequest(Request request) {
        BrokerServiceInitiation initiateRequest = new BrokerServiceInitiation();

        ManifestBuilder manifestBuilder = new ManifestBuilder()
                .withSender(request.getSender())
                .withSenderReference(request.getSenderReference())
                .withFilename("sbd.xml");
        initiateRequest.setManifest(manifestBuilder.build());
        initiateRequest.setRecipientList(new RecipientBuilder().withPartyNumber(request.getReceiver()).build());

        return initiateRequest;
    }
}
