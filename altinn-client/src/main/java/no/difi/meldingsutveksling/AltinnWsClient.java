package no.difi.meldingsutveksling;

import com.google.common.collect.Lists;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.*;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.*;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.shipping.ws.AltinnReasonFactory;
import no.difi.meldingsutveksling.shipping.ws.AltinnWsException;
import no.difi.meldingsutveksling.shipping.ws.ManifestBuilder;
import no.difi.meldingsutveksling.shipping.ws.RecipientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AltinnWsClient {

    private static final String FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE = "Failed to upload a message to Altinn broker service";
    private static final String FAILED_TO_INITATE_ALTINN_BROKER_SERVICE = "Failed to initate Altinn broker service";
    private static final String FILE_NAME = "sbd.zip";
    private static final String AVAILABLE_FILES_ERROR_MESSAGE = "Could not get list of available files from Altinn " +
            "formidlingstjeneste. Reason: {}";
    private static final String CANNOT_DOWNLOAD_FILE = "Cannot download file";
    private static final String CANNOT_CONFIRM_DOWNLOAD = "Cannot confirm download";
    private final AltinnWsConfiguration configuration;

    private static final Logger log = LoggerFactory.getLogger(AltinnWsClient.class);

    public AltinnWsClient(AltinnWsConfiguration altinnWsConfiguration) {
        this.configuration = altinnWsConfiguration;
    }

    public void send(UploadRequest request) {
        String senderReference = initiateBrokerService(request);
        upload(request, senderReference);
    }

    private void upload(UploadRequest request, String senderReference) {

        BrokerServiceExternalBasicStreamedSF brokerServiceExternalBasicStreamedSF
                = new BrokerServiceExternalBasicStreamedSF(configuration.getStreamingServiceUrl());

        IBrokerServiceExternalBasicStreamed streamingService = brokerServiceExternalBasicStreamedSF.getBasicHttpBindingIBrokerServiceExternalBasicStreamed();

        BindingProvider bp = (BindingProvider) streamingService;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getStreamingServiceUrl().toString());

        try {
            StreamedPayloadBasicBE parameters = new StreamedPayloadBasicBE();

            AltinnPackage altinnPackage = AltinnPackage.from(request);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            altinnPackage.write(outputStream);
            parameters.setDataStream(outputStream.toByteArray());

            ReceiptExternalStreamedBE receiptAltinn = streamingService.uploadFileStreamedBasic(parameters, FILE_NAME, senderReference, request.getSender(), configuration.getPassword(), configuration.getUsername());
            Audit.info("Message uploaded", markerFrom(receiptAltinn).and(request.getMarkers()));
        } catch (IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage e) {
            Audit.error("Message failed to upload", request.getMarkers(), e);
            throw new AltinnWsException(FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE, AltinnReasonFactory.from(e), e);
        } catch (IOException e) {
            Audit.error("Message failed to upload", request.getMarkers(), e);
            throw new AltinnWsException(FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE, e);
        }
    }

    /**
     * Creates Logstash Markers to be used with logging. Makes it easier to troubleshoot problems with Altinn
     *
     * @param receiptAltinn the receipt returned from Altinn formidlingstjeneste
     * @return log markers providing information needed to troubleshoot the logs
     */
    private LogstashMarker markerFrom(ReceiptExternalStreamedBE receiptAltinn) {
        LogstashMarker idMarker = Markers.append("altinn-receipt-id", receiptAltinn.getReceiptId());
        LogstashMarker statusCodeMarker = Markers.append("altinn-status-code", receiptAltinn.getReceiptStatusCode().getValue());
        LogstashMarker textMarker = Markers.append("altinn-text", receiptAltinn.getReceiptText().getValue());
        return idMarker.and(statusCodeMarker).and(textMarker);
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
            log.error(AVAILABLE_FILES_ERROR_MESSAGE, AltinnReasonFactory.from(e));
            return Lists.newArrayList();
        }

        return filesBasic.getBrokerServiceAvailableFile()
            .stream().map(f -> new FileReference(f.getFileReference(), f.getReceiptID()))
            .collect(Collectors.toList());
    }


    public EduDocument download(DownloadRequest request) {
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
            return altinnPackage.getEduDocument();

        } catch (IBrokerServiceExternalBasicStreamedDownloadFileStreamedBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(CANNOT_DOWNLOAD_FILE, AltinnReasonFactory.from(e), e);
        } catch (IOException | JAXBException e) {
            throw new AltinnWsException(CANNOT_DOWNLOAD_FILE, e);
        }
    }

    public void confirmDownload(DownloadRequest request) {
        final BrokerServiceExternalBasicSF brokerServiceExternalBasicSF = new BrokerServiceExternalBasicSF(configuration.getBrokerServiceUrl());
        final IBrokerServiceExternalBasic service = brokerServiceExternalBasicSF.getBasicHttpBindingIBrokerServiceExternalBasic();
        final BindingProvider bp = (BindingProvider) service;

        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getBrokerServiceUrl().toString());

        try {
            service.confirmDownloadedBasic(configuration.getUsername(), configuration.getPassword(), request.fileReference, request.getReciever());
        } catch (IBrokerServiceExternalBasicConfirmDownloadedBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(CANNOT_CONFIRM_DOWNLOAD, AltinnReasonFactory.from(e), e);
        }

    }

    private String initiateBrokerService(UploadRequest request) {
        BrokerServiceInitiation brokerServiceInitiation = createInitiationRequest(request);
        try {
            BrokerServiceExternalBasicSF brokerServiceExternalBasicSF;
            brokerServiceExternalBasicSF = new BrokerServiceExternalBasicSF(configuration.getBrokerServiceUrl());
            IBrokerServiceExternalBasic service = brokerServiceExternalBasicSF.getBasicHttpBindingIBrokerServiceExternalBasic();
            BindingProvider bp = (BindingProvider) service;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getBrokerServiceUrl().toString());
            return service.initiateBrokerServiceBasic(configuration.getUsername(), configuration.getPassword(), brokerServiceInitiation);
        } catch (IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(FAILED_TO_INITATE_ALTINN_BROKER_SERVICE, AltinnReasonFactory.from(e), e);
        }
    }

    private BrokerServiceInitiation createInitiationRequest(UploadRequest request) {
        BrokerServiceInitiation initiateRequest = new BrokerServiceInitiation();

        ManifestBuilder manifestBuilder = new ManifestBuilder()
                .withSender(request.getSender())
                .withSenderReference(request.getSenderReference())
                .withExternalServiceCode(configuration.getExternalServiceCode())
                .withExternalServiceEditionCode(configuration.getExternalServiceEditionCode())
                .withFilename(FILE_NAME);
        initiateRequest.setManifest(manifestBuilder.build());
        initiateRequest.setRecipientList(new RecipientBuilder().withPartyNumber(request.getReceiver()).build());
        return initiateRequest;
    }


}