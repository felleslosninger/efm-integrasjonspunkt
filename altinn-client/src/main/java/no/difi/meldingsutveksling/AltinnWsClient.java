package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.*;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.*;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.shipping.ws.AltinnWsException;
import no.difi.meldingsutveksling.shipping.ws.ManifestBuilder;
import no.difi.meldingsutveksling.shipping.ws.RecipientBuilder;

import java.util.ArrayList;
import java.util.List;

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

    public void send(UploadRequest request) {
        String senderReference = initiateBrokerService(request);
        upload(request, senderReference);
    }

    private void upload(UploadRequest request, String senderReference) {
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

    public List<FileReference> availableFiles(String partyNumber) {
        BrokerServiceExternalBasicSF brokerServiceExternalBasicSF;

        brokerServiceExternalBasicSF = new BrokerServiceExternalBasicSF(configuration.getBrokerServiceUrl());

        IBrokerServiceExternalBasic service = brokerServiceExternalBasicSF.getBasicHttpBindingIBrokerServiceExternalBasic();

        BrokerServiceSearch searchParameters = new BrokerServiceSearch();
        searchParameters.setFileStatus(BrokerServiceAvailableFileStatus.UPLOADED);
        searchParameters.setReportee(partyNumber);

        BrokerServiceAvailableFileList filesBasic;
        try {
            filesBasic = service.getAvailableFilesBasic(configuration.getUsername(), configuration.getPassword(), searchParameters);
        } catch (IBrokerServiceExternalBasicGetAvailableFilesBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(AVAILABLE_FILES_ERROR_MESSAGE, e);
        }

        List<FileReference> fileReferences = new ArrayList<>();
        for(BrokerServiceAvailableFile f: filesBasic.getBrokerServiceAvailableFile()) {
            fileReferences.add(new FileReference(f.getFileReference(), f.getReceiptID()));
        }

        return fileReferences;
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

    private String initiateBrokerService(UploadRequest request) {
        BrokerServiceInitiation brokerServiceInitiation = createInitiationRequest(request);
        try {
            BrokerServiceExternalBasicSF brokerService = new BrokerServiceExternalBasicSF(configuration.getBrokerServiceUrl());
            return brokerService.getBasicHttpBindingIBrokerServiceExternalBasic().initiateBrokerServiceBasic(configuration.getUsername(), configuration.getPassword(), brokerServiceInitiation);
        } catch (IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(FAILED_TO_INITATE_ALTINN_BROKER_SERVICE, e);
        }
    }

    private BrokerServiceInitiation createInitiationRequest(UploadRequest request) {
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
