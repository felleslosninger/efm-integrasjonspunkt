package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.altinnv3.dpo.payload.AltinnPackage;
import no.difi.meldingsutveksling.altinnv3.dpo.payload.ZipUtils;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.FileTransferStatusDetailsExt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class AltinnDPODownloadService {

    private final BrokerApiClient brokerApiClient;
    private final IntegrasjonspunktProperties properties;
    private final ZipUtils zipUtils;

    public UUID[] getAvailableFiles() {

        UUID[] fileTransferIds = brokerApiClient.getAvailableFiles();

        List<FileTransferStatusDetailsExt> files = Arrays.stream(fileTransferIds)
                .map(fileTransferId -> brokerApiClient.getDetails((fileTransferId.toString()))).toList();

        files = filterBasedUponSendersFileTransferReference(files);

        return files.stream().map(FileTransferStatusDetailsExt::getFileTransferId).toArray(UUID[]::new);
    }

    public AltinnPackage download(DownloadRequest request) {
        byte[] bytes = brokerApiClient.downloadFile(request.getFileReference());

        try {
            return zipUtils.getAltinnPackage(bytes);
        }catch (IOException | JAXBException e){
            throw new BrokerApiException("Error when downloading file with reference %s".formatted(request.getFileReference()), e);
        }
    }

    public void confirmDownload(DownloadRequest request) {
        brokerApiClient.confirmDownload(request.getFileReference());
    }

    private List<FileTransferStatusDetailsExt> filterBasedUponSendersFileTransferReference(List<FileTransferStatusDetailsExt> files){
        if (!isNullOrEmpty(properties.getDpo().getMessageChannel())) {
            return files.stream().filter(this::isMessageMatchingConfiguredMessageChannel).toList();
        } else {
            // SendersReference is default set to random UUID.
            // Make sure not to consume messages with matching message channel pattern.
            return files.stream().filter(this::isMessageMatchingMessageChannelPattern).toList();
        }
    }

    private boolean isMessageMatchingMessageChannelPattern(FileTransferStatusDetailsExt details){
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9-_]{0,25}$");

        return details.getSendersFileTransferReference() == null ||
            !pattern.matcher(details.getSendersFileTransferReference()).matches();
    }

    private boolean isMessageMatchingConfiguredMessageChannel(FileTransferStatusDetailsExt details) {
        return details.getSendersFileTransferReference() != null &&
            details.getSendersFileTransferReference().equals(properties.getDpo().getMessageChannel());
    }

    private boolean isNullOrEmpty(String s) { return s == null || s.isEmpty(); }
}
