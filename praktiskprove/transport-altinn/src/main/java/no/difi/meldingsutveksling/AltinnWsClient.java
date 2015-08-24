package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceExternalBasicSF;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceInitiation;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.*;
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
    public static final String INVALID_URL_FOR_ALTINN_BROKER_SERVICE = "Invalid url for Altinn broker service";
    public static final String FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE = "Failed to upload a message to Altinn broker service";
    public static final String FAILED_TO_INITATE_ALTINN_BROKER_SERVICE = "Failed to initate Altinn broker service";
    public static final String FILE_NAME = "sbd.zip";
    private static final int BUFFER_SIZE = 65536;

    private final URL url;

    public AltinnWsClient(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public void send(Request request) {
        String senderReference = initiateBrokerService(request);
        upload(request, senderReference);
    }

    private void upload(Request request, String senderReference) {
        BrokerServiceExternalBasicStreamedSF brokerServiceExternalBasicStreamedSF;
        URL wsdlLocation;
        try {
            wsdlLocation = new URL("http://localhost:7777/altinn/streamedmessage");
        } catch (MalformedURLException e) {
            throw new AltinnWsException(INVALID_URL_FOR_ALTINN_BROKER_SERVICE, e);
        }
        brokerServiceExternalBasicStreamedSF = new BrokerServiceExternalBasicStreamedSF(wsdlLocation, new QName("http://www.altinn.no/services/ServiceEngine/Broker/2015/06", "BrokerServiceExternalBasicStreamedSF"));

        IBrokerServiceExternalBasicStreamed streamingService = brokerServiceExternalBasicStreamedSF.getBasicHttpBindingIBrokerServiceExternalBasicStreamed();

        try {
            StreamedPayloadBasicBE parameters = new StreamedPayloadBasicBE();
            AltinnPackage altinnPackage = AltinnPackage.from(request);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
            altinnPackage.write(outputStream);
            parameters.setDataStream(outputStream.toByteArray());

            ReceiptExternalStreamedBE receiptExternal = streamingService.uploadFileStreamedBasic(parameters, FILE_NAME, senderReference, request.getSender(), "password", "username");
        } catch (IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage | IOException e) {
            throw new AltinnWsException(FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE, e);
        }
    }

    private String initiateBrokerService(Request request) {
        BrokerServiceInitiation brokerServiceInitiation = createInitiationRequest(request);
        try {
            BrokerServiceExternalBasicSF brokerService = new BrokerServiceExternalBasicSF(url, new QName("http://www.altinn.no/services/ServiceEngine/Broker/2015/06", "IBrokerServiceExternalBasicImplService"));
            return brokerService.getBasicHttpBindingIBrokerServiceExternalBasic().initiateBrokerServiceBasic("username", "password", brokerServiceInitiation);
        } catch (IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(FAILED_TO_INITATE_ALTINN_BROKER_SERVICE, e);
        }
    }

    private BrokerServiceInitiation createInitiationRequest(Request request) {
        BrokerServiceInitiation initiateRequest = new BrokerServiceInitiation();

        ManifestBuilder manifestBuilder = new ManifestBuilder()
                .withSender(request.getSender())
                .withSenderReference(request.getSenderReference())
                .withFilename("FILE_NAME");
        initiateRequest.setManifest(manifestBuilder.build());
        initiateRequest.setRecipientList(new RecipientBuilder().withPartyNumber(request.getReceiver()).build());

        return initiateRequest;
    }
}
