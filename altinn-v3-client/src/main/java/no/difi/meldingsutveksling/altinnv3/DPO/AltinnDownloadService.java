package no.difi.meldingsutveksling.altinnv3.DPO;

import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.TmpFile;
import no.difi.meldingsutveksling.altinnv3.DPO.altinn2.AltinnPackage;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.FileTransferStatusDetailsExt;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
//@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class AltinnDownloadService {

    private final BrokerApiClient brokerApiClient;
    private final IntegrasjonspunktProperties properties;
    private final ApplicationContext context;

    public List<FileReference> getAvailableFiles() {

        UUID[] fileTransferIds = brokerApiClient.getAvailableFiles();

        List<FileTransferStatusDetailsExt> files = Arrays.stream(fileTransferIds)
                .map(fileTransferId -> brokerApiClient.getDetails((fileTransferId.toString()))).toList();

        files = filterBasedUponSendersFileTransferReference(files);

        return files.stream().map(file ->
            new FileReference(file.getFileTransferId(), 9))
            .collect(Collectors.toList());
    }

    public AltinnPackage download(DownloadRequest request) {
        try {
            byte[] bytes = brokerApiClient.downloadFile(request.getFileReference());

            TmpFile tmpFile = TmpFile.create();

            try {
                File file = tmpFile.getFile();
                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                    FileUtils.copyInputStreamToFile(inputStream, file);
                }
                return AltinnPackage.from(file, context);
            } finally {
                tmpFile.delete();
            }
        } catch (IOException | JAXBException e){
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
