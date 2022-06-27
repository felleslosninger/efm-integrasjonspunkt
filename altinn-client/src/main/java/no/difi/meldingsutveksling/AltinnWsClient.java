package no.difi.meldingsutveksling;

import com.sun.xml.ws.developer.JAXWSProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.ObjectFactory;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.*;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.*;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.shipping.ws.AltinnReasonFactory;
import no.difi.meldingsutveksling.shipping.ws.AltinnWsException;
import no.difi.meldingsutveksling.shipping.ws.ManifestBuilder;
import no.difi.meldingsutveksling.shipping.ws.RecipientBuilder;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
@RequiredArgsConstructor
public class AltinnWsClient {

    private static final String FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE = "Failed to upload a message to Altinn broker service";
    private static final String FAILED_TO_INITATE_ALTINN_BROKER_SERVICE = "Failed to initate Altinn broker service";
    private static final String FILE_NAME = "sbd.zip";
    private static final String AVAILABLE_FILES_ERROR_MESSAGE = "Could not get list of available files from Altinn " +
            "formidlingstjeneste. Reason: {}";
    private static final String CANNOT_DOWNLOAD_FILE = "Cannot download file";
    private static final String CANNOT_CONFIRM_DOWNLOAD = "Cannot confirm download";

    private final AltinnWsConfiguration configuration;
    private final ApplicationContext context;
    private final Plumber plumber;
    private final PromiseMaker promiseMaker;
    private final IntegrasjonspunktProperties properties;
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final IBrokerServiceExternalBasic iBrokerServiceExternalBasic = brokerServiceExternalBasicSF();
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final IBrokerServiceExternalBasicStreamed iBrokerServiceExternalBasicStreamed = brokerServiceExternalBasicStreamedSF();

    public void send(UploadRequest request) {
        String senderReference = initiateBrokerService(request);
        upload(request, senderReference);
    }

    private void upload(UploadRequest request, String senderReference) {
        try {
            promiseMaker.promise(reject -> {
                try (InputStream inputStream = getInputStream(request, reject)) {
                    StreamedPayloadBasicBE parameters = new StreamedPayloadBasicBE();
                    parameters.setDataStream(new DataHandler(InputStreamDataSource.of(inputStream)));
                    uploadToAltinn(request, senderReference, parameters);
                    return null;
                } catch (IOException e) {
                    throw new AltinnWsException(FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE, e);
                }
            }).await();
        } catch (Exception e) {
            auditError(request, e);
            throw e;
        }
    }

    private InputStream getInputStream(UploadRequest request, Reject reject) {
        return plumber.pipe("write Altinn zip",
                inlet -> {
                    AltinnPackage altinnPackage = AltinnPackage.from(request);
                    writeAltinnZip(request, altinnPackage, inlet);
                }, reject).outlet();
    }

    private void writeAltinnZip(UploadRequest request, AltinnPackage altinnPackage, PipedOutputStream pos) {
        try {
            altinnPackage.write(pos, context);
        } catch (IOException e) {
            auditError(request, e);
            throw new AltinnWsException(FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE, e);
        }
    }

    private void auditError(UploadRequest request, Exception e) {
        Audit.error("Message failed to upload to altinn", request.getMarkers(), e);
    }

    private void uploadToAltinn(UploadRequest request, String senderReference, StreamedPayloadBasicBE parameters) {
        log.debug("Starting thread: upload to altinn");
        try {
            ReceiptExternalStreamedBE receiptAltinn = getIBrokerServiceExternalBasicStreamed().uploadFileStreamedBasic(parameters, FILE_NAME, senderReference, request.getSender(), configuration.getPassword(), configuration.getUsername());
            log.debug(markerFrom(receiptAltinn).and(request.getMarkers()), "Message uploaded to altinn");
        } catch (IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage e) {
            auditError(request, e);
            throw new AltinnWsException(FAILED_TO_UPLOAD_A_MESSAGE_TO_ALTINN_BROKER_SERVICE, AltinnReasonFactory.from(e), e);
        }

        log.debug("Thread finished: upload to altinn");
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

    public List<FileReference> availableFiles(String orgnr) {
        Stream<BrokerServiceAvailableFile> fileStream = getBrokerServiceAvailableFileList(orgnr)
            .map(BrokerServiceAvailableFileList::getBrokerServiceAvailableFile)
            .orElse(Collections.emptyList())
            .stream();
        if (!isNullOrEmpty(properties.getDpo().getMessageChannel())) {
            fileStream = fileStream.filter(f -> f.getSendersReference() != null &&
                f.getSendersReference().getValue().equals(properties.getDpo().getMessageChannel()));
        }else if(properties.getOrg().getIdentifier().hasOrganizationPartIdentifier()){
            fileStream = fileStream.filter(f -> f.getSendersReference() != null &&
                    f.getSendersReference().getValue().equals(properties.getOrg().getIdentifier().getIdentifier()));
        } else {
            // SendersReference is default set to random UUID.
            // Make sure not to consume messages with matching message channel pattern.
            Pattern uuidRegexpPattern = Pattern.compile("^[a-zA-Z0-9-_]{0,25}$");
            fileStream = fileStream.filter(f -> f.getSendersReference() == null ||
                !uuidRegexpPattern.matcher(f.getSendersReference().getValue()).matches());
        }
        return fileStream
            .map(f -> new FileReference(f.getFileReference(), f.getReceiptID()))
            .collect(Collectors.toList());
    }

    public boolean checkIfAvailableFiles(String orgnr) throws IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage {
        BrokerServicePoll params = new BrokerServicePoll();
        ArrayOfstring recipients = new ArrayOfstring();
        recipients.getString().add(orgnr);
        params.setRecipients(recipients);

        ObjectFactory of = new ObjectFactory();
        JAXBElement<String> serviceCode = of.createBrokerServicePollExternalServiceCode(configuration.getExternalServiceCode());
        params.setExternalServiceCode(serviceCode);
        params.setExternalServiceEditionCode(configuration.getExternalServiceEditionCode());

        return getIBrokerServiceExternalBasic().checkIfAvailableFilesBasic(configuration.getUsername(), configuration.getPassword(), params);
    }

    private Optional<BrokerServiceAvailableFileList> getBrokerServiceAvailableFileList(String orgnr) {
        try {
            return Optional.of(getIBrokerServiceExternalBasic().getAvailableFilesBasic(configuration.getUsername(), configuration.getPassword(), getBrokerServiceSearch(orgnr)));
        } catch (IBrokerServiceExternalBasicGetAvailableFilesBasicAltinnFaultFaultFaultMessage e) {
            log.error(AVAILABLE_FILES_ERROR_MESSAGE, AltinnReasonFactory.from(e));
            return Optional.empty();
        }
    }

    private BrokerServiceSearch getBrokerServiceSearch(String orgnr) {
        BrokerServiceSearch searchParameters = new BrokerServiceSearch();
        searchParameters.setFileStatus(BrokerServiceAvailableFileStatus.UPLOADED);
        searchParameters.setReportee(orgnr);
        ObjectFactory of = new ObjectFactory();
        JAXBElement<String> serviceCode = of.createBrokerServiceAvailableFileExternalServiceCode(configuration.getExternalServiceCode());
        searchParameters.setExternalServiceCode(serviceCode);
        searchParameters.setExternalServiceEditionCode(configuration.getExternalServiceEditionCode());
        return searchParameters;
    }

    public AltinnPackage download(DownloadRequest request) {
        try {
            DataHandler dh = getIBrokerServiceExternalBasicStreamed().downloadFileStreamedBasic(configuration.getUsername(), configuration.getPassword(), request.getFileReference(), request.getReciever());
            // TODO: rewrite this when Altinn fixes zip
            TmpFile tmpFile = TmpFile.create();
            File file = tmpFile.getFile();
            FileUtils.copyInputStreamToFile(dh.getInputStream(), file);
            AltinnPackage altinnPackage = AltinnPackage.from(file, context);
            tmpFile.delete();

            return altinnPackage;
        } catch (IBrokerServiceExternalBasicStreamedDownloadFileStreamedBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(CANNOT_DOWNLOAD_FILE, AltinnReasonFactory.from(e), e);
        } catch (IOException | JAXBException e) {
            throw new AltinnWsException(CANNOT_DOWNLOAD_FILE, e);
        }
    }

    public void confirmDownload(DownloadRequest request) {
        try {
            getIBrokerServiceExternalBasic().confirmDownloadedBasic(configuration.getUsername(), configuration.getPassword(), request.fileReference, request.getReciever());
        } catch (IBrokerServiceExternalBasicConfirmDownloadedBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(CANNOT_CONFIRM_DOWNLOAD, AltinnReasonFactory.from(e), e);
        }
    }

    private String initiateBrokerService(UploadRequest request) {
        BrokerServiceInitiation brokerServiceInitiation = createInitiationRequest(request);
        try {
            return getIBrokerServiceExternalBasic().initiateBrokerServiceBasic(configuration.getUsername(), configuration.getPassword(), brokerServiceInitiation);
        } catch (IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage e) {
            throw new AltinnWsException(FAILED_TO_INITATE_ALTINN_BROKER_SERVICE, AltinnReasonFactory.from(e), e);
        }
    }

    private BrokerServiceInitiation createInitiationRequest(UploadRequest request) {
        BrokerServiceInitiation initiateRequest = new BrokerServiceInitiation();
        initiateRequest.setManifest(getManifest(request));
        initiateRequest.setRecipientList(new RecipientBuilder().withPartyNumber(request.getReceiver()).build());
        return initiateRequest;
    }

    private Manifest getManifest(UploadRequest request) {
        return new ManifestBuilder()
                .withSender(request.getSender())
                .withSenderReference(request.getSenderReference())
                .withExternalServiceCode(configuration.getExternalServiceCode())
                .withExternalServiceEditionCode(configuration.getExternalServiceEditionCode())
                .withFilename(FILE_NAME)
                .build();
    }

    private IBrokerServiceExternalBasic brokerServiceExternalBasicSF() {
        BrokerServiceExternalBasicSF brokerServiceExternalBasicSF;
        brokerServiceExternalBasicSF = new BrokerServiceExternalBasicSF(Objects.requireNonNull(configuration).getBrokerServiceUrl());
        IBrokerServiceExternalBasic service = brokerServiceExternalBasicSF.getBasicHttpBindingIBrokerServiceExternalBasic();
        BindingProvider bp = (BindingProvider) service;
        bp.getRequestContext().put(JAXWSProperties.REQUEST_TIMEOUT, configuration.getRequestTimeout());
        bp.getRequestContext().put(JAXWSProperties.CONNECT_TIMEOUT, configuration.getConnectTimeout());
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getBrokerServiceUrl().toString());
        return service;
    }

    private IBrokerServiceExternalBasicStreamed brokerServiceExternalBasicStreamedSF() {
        BrokerServiceExternalBasicStreamedSF brokerServiceExternalBasicStreamedSF;
        brokerServiceExternalBasicStreamedSF = new BrokerServiceExternalBasicStreamedSF(Objects.requireNonNull(configuration).getStreamingServiceUrl());
        IBrokerServiceExternalBasicStreamed streamingService = brokerServiceExternalBasicStreamedSF.getBasicHttpBindingIBrokerServiceExternalBasicStreamed(new MTOMFeature(true));

        BindingProvider bp = (BindingProvider) streamingService;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getStreamingServiceUrl().toString());
        bp.getRequestContext().put(JAXWSProperties.REQUEST_TIMEOUT, configuration.getRequestTimeout());
        bp.getRequestContext().put(JAXWSProperties.CONNECT_TIMEOUT, configuration.getConnectTimeout());
        bp.getRequestContext().put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);
        SOAPBinding binding = (SOAPBinding) bp.getBinding();
        binding.setMTOMEnabled(true);
        return streamingService;
    }
}