package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.*;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.*;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.shipping.ws.AltinnWsException;
import no.difi.meldingsutveksling.shipping.ws.ManifestBuilder;
import no.difi.meldingsutveksling.shipping.ws.RecipientBuilder;

import javax.xml.bind.JAXBException;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AltinnWsClient {

    public static final String FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE = "Failed to upload a message to Altinn broker service";
    public static final String FAILED_TO_INITATE_ALTINN_BROKER_SERVICE = "Failed to initate Altinn broker service";
    public static final String FILE_NAME = "sbd.zip";
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

        BindingProvider bp = (BindingProvider) streamingService;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getStreamingServiceUrl().toString());

        try {
            StreamedPayloadBasicBE parameters = new StreamedPayloadBasicBE();

            AltinnPackage altinnPackage = AltinnPackage.from(request);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            altinnPackage.write(outputStream);
            parameters.setDataStream(outputStream.toByteArray());

            ReceiptExternalStreamedBE receiptExternal = streamingService.uploadFileStreamedBasic(parameters, FILE_NAME, senderReference, request.getSender(), configuration.getPassword(), configuration.getUsername());
        } catch (IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage | IOException e) {
            throw new AltinnWsException(FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE, e);
        }
    }

    public List<FileReference> availableFiles(String partyNumber) {

        BrokerServiceExternalBasicSF brokerServiceExternalBasicSF;
        brokerServiceExternalBasicSF = new BrokerServiceExternalBasicSF(configuration.getBrokerServiceUrl());
        IBrokerServiceExternalBasic service = brokerServiceExternalBasicSF.getBasicHttpBindingIBrokerServiceExternalBasic();
        BindingProvider bp = (BindingProvider) service;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getBrokerServiceUrl().toString());

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
        for (BrokerServiceAvailableFile f : filesBasic.getBrokerServiceAvailableFile()) {
            fileReferences.add(new FileReference(f.getFileReference(), f.getReceiptID()));
        }

        return fileReferences;
    }

    public StandardBusinessDocument download(DownloadRequest request) {
        BrokerServiceExternalBasicStreamedSF brokerServiceExternalBasicStreamedSF;
        brokerServiceExternalBasicStreamedSF = new BrokerServiceExternalBasicStreamedSF(configuration.getStreamingServiceUrl());
        IBrokerServiceExternalBasicStreamed streamingService = brokerServiceExternalBasicStreamedSF.getBasicHttpBindingIBrokerServiceExternalBasicStreamed(new MTOMFeature(true));
        BindingProvider bp = (BindingProvider) streamingService;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getStreamingServiceUrl().toString());
        SOAPBinding binding = (SOAPBinding) bp.getBinding();
        binding.setMTOMEnabled(true);

        byte[] message;
        try {
            message = streamingService.downloadFileStreamedBasic(configuration.getUsername(), configuration.getPassword(), request.getFileReference(), request.getReciever());
            AltinnPackage altinnPackage = AltinnPackage.from(new ByteArrayInputStream(message));
            return altinnPackage.getDocument();

        } catch (IBrokerServiceExternalBasicStreamedDownloadFileStreamedBasicAltinnFaultFaultFaultMessage | IOException | JAXBException e) {
            throw new AltinnWsException("Cannot download file", e);
        }
    }

    private String initiateBrokerService(UploadRequest request) {
        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
        BrokerServiceInitiation brokerServiceInitiation = createInitiationRequest(request);
        try {
            BrokerServiceExternalBasicSF brokerServiceExternalBasicSF;
            brokerServiceExternalBasicSF = new BrokerServiceExternalBasicSF(configuration.getBrokerServiceUrl());
            IBrokerServiceExternalBasic service = brokerServiceExternalBasicSF.getBasicHttpBindingIBrokerServiceExternalBasic();
            BindingProvider bp = (BindingProvider) service;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getBrokerServiceUrl().toString());
            return service.initiateBrokerServiceBasic(configuration.getUsername(), configuration.getPassword(), brokerServiceInitiation);
        } catch (IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(FAILED_TO_INITATE_ALTINN_BROKER_SERVICE, e);
        }
    }

    private BrokerServiceInitiation createInitiationRequest(UploadRequest request) {
        BrokerServiceInitiation initiateRequest = new BrokerServiceInitiation();

        ManifestBuilder manifestBuilder = new ManifestBuilder()
                .withSender(request.getSender())
                .withSenderReference(request.getSenderReference())
                .withExternalServiceCode("v3888") // TODO: extract into properties
                .withExternalServiceEditionCode(70515)
                .withFilename(FILE_NAME);
        initiateRequest.setManifest(manifestBuilder.build());
        initiateRequest.setRecipientList(new RecipientBuilder().withPartyNumber(request.getReceiver()).build());

        return initiateRequest;
    }


}
