package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.*;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.*;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.Request;
import no.difi.meldingsutveksling.shipping.ws.AltinnWsException;
import no.difi.meldingsutveksling.shipping.ws.ManifestBuilder;
import no.difi.meldingsutveksling.shipping.ws.RecipientBuilder;

import javax.xml.namespace.QName;

public class AltinnWsClient {
    public static final String FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE = "Failed to upload a message to Altinn broker service";
    public static final String FAILED_TO_INITATE_ALTINN_BROKER_SERVICE = "Failed to initate Altinn broker service";
    public static final String FILE_NAME = "sbd.zip";
    private static final int BUFFER_SIZE = 65536;
    public static final String AVAILABLE_FILES_ERROR_MESSAGE = "Could not get list of available files from Altinn formidlingstjeneste";
    private final AltinnWsConfiguration configuration;

    public AltinnWsClient(AltinnWsConfiguration configuration) {
        this.configuration = configuration;
    }

    public void send(Request request) {
        String senderReference = initiateBrokerService(request);
        upload(request, senderReference);
    }

    private void upload(Request request, String senderReference) {
        BrokerServiceExternalBasicStreamedSF brokerServiceExternalBasicStreamedSF;

        brokerServiceExternalBasicStreamedSF = new BrokerServiceExternalBasicStreamedSF(configuration.getStreamingServiceUrl());

        IBrokerServiceExternalBasicStreamed streamingService = brokerServiceExternalBasicStreamedSF.getBasicHttpBindingIBrokerServiceExternalBasicStreamed();

        try {
            StreamedPayloadBasicBE parameters = new StreamedPayloadBasicBE();

            StandardBusinessDocumentConverter converter = new StandardBusinessDocumentConverter();
            parameters.setDataStream(converter.marshallToBytes(request.getPayload()));

            ReceiptExternalStreamedBE receiptExternal = streamingService.uploadFileStreamedBasic(parameters, FILE_NAME, senderReference, request.getSender(), "password", "username");
        } catch (IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE, e);
        }
    }

    public java.util.List<BrokerServiceAvailableFile> availableFiles(Request request, BrokerServiceAvailableFileStatus serviceStatus) {
        String senderReference = initiateBrokerService(request);
        BrokerServiceExternalBasicSF brokerServiceExternalBasicSF;

        brokerServiceExternalBasicSF = new BrokerServiceExternalBasicSF(configuration.getBrokerServiceUrl(), new QName("http://www.altinn.no/services/ServiceEngine/Broker/2015/06", "IBrokerServiceExternalBasicImplService"));

        IBrokerServiceExternalBasic service = brokerServiceExternalBasicSF.getBasicHttpBindingIBrokerServiceExternalBasic();

        BrokerServiceSearch searchParameters = new BrokerServiceSearch();
        searchParameters.setFileStatus(serviceStatus);
        searchParameters.setReportee(senderReference);

        BrokerServiceAvailableFileList filesBasic;
        try {
            filesBasic = service.getAvailableFilesBasic(configuration.getUsername(), configuration.getPassword(), searchParameters);
        } catch (IBrokerServiceExternalBasicGetAvailableFilesBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(AVAILABLE_FILES_ERROR_MESSAGE, e);
        }
        return filesBasic.getBrokerServiceAvailableFile();
    }

    public StandardBusinessDocument download(DownloadRequest request) {
        BrokerServiceExternalBasicStreamedSF brokerServiceExternalBasicStreamedSF;

        brokerServiceExternalBasicStreamedSF = new BrokerServiceExternalBasicStreamedSF(configuration.getStreamingServiceUrl());

        IBrokerServiceExternalBasicStreamed streamingService = brokerServiceExternalBasicStreamedSF.getBasicHttpBindingIBrokerServiceExternalBasicStreamed();

        byte[] message;
        try {
            message = streamingService.downloadFileStreamedBasic(configuration.getUsername(), configuration.getPassword(), request.getFileReference(), request.getReciever());
        } catch (IBrokerServiceExternalBasicStreamedDownloadFileStreamedBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException("Cannot download file", e);
        }

        StandardBusinessDocumentConverter converter = new StandardBusinessDocumentConverter();
        return converter.unmarshallFrom(message);
    }

    private String initiateBrokerService(Request request) {
        BrokerServiceInitiation brokerServiceInitiation = createInitiationRequest(request);
        try {
            BrokerServiceExternalBasicSF brokerService = new BrokerServiceExternalBasicSF(configuration.getBrokerServiceUrl(), new QName("http://www.altinn.no/services/ServiceEngine/Broker/2015/06", "IBrokerServiceExternalBasicImplService"));
            return brokerService.getBasicHttpBindingIBrokerServiceExternalBasic().initiateBrokerServiceBasic(configuration.getUsername(), configuration.getPassword(), brokerServiceInitiation);
        } catch (IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(FAILED_TO_INITATE_ALTINN_BROKER_SERVICE, e);
        }
    }

    private BrokerServiceInitiation createInitiationRequest(Request request) {
        BrokerServiceInitiation initiateRequest = new BrokerServiceInitiation();

        ManifestBuilder manifestBuilder = new ManifestBuilder()
                .withSender(request.getSender())
                .withSenderReference(request.getSenderReference())
                .withFilename(FILE_NAME);
        initiateRequest.setManifest(manifestBuilder.build());
        initiateRequest.setRecipientList(new RecipientBuilder().withPartyNumber(request.getReceiver()).build());

        return initiateRequest;
    }
}
