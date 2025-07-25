package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.xml.bind.JAXBException;
import no.difi.meldingsutveksling.altinnv3.UseFullTestConfiguration;
import no.difi.meldingsutveksling.altinnv3.dpo.payload.ZipUtils;
import no.difi.meldingsutveksling.config.AltinnFormidlingsTjenestenConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.FileTransferStatusDetailsExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = AltinnDPODownloadService.class)
@UseFullTestConfiguration
public class AltinnDownloadServiceTest {

    @MockitoBean
    private BrokerApiClient brokerApiClient;

    @MockitoBean
    private IntegrasjonspunktProperties integrasjonspunktProperties;

    @MockitoBean
    private ZipUtils zipUtils;

    @Autowired
    private AltinnDPODownloadService altinnDownloadService;

    private final UUID fileWithRandomMessageChannel = UUID.fromString("dc11ecad-dc5c-44a3-b566-b460485580f8");
    private final UUID fileWithMessageChannelAsMessageChannel = UUID.fromString("bbcc4621-d88f-4a94-ae2f-b38072bf5087");
    private final UUID fileWithMessageChannelAsSomethingElse = UUID.fromString("97d7380a-217b-4873-abe8-1530a512d7b6");

    @BeforeEach
    public void beforeAll() {
        FileTransferStatusDetailsExt file1 = new FileTransferStatusDetailsExt();
        file1.setSendersFileTransferReference(UUID.randomUUID().toString());
        file1.setFileTransferId(fileWithRandomMessageChannel);

        FileTransferStatusDetailsExt file2 = new FileTransferStatusDetailsExt();
        file2.setSendersFileTransferReference("messageChannel");
        file2.setFileTransferId(fileWithMessageChannelAsMessageChannel);

        FileTransferStatusDetailsExt file3 = new FileTransferStatusDetailsExt();
        file3.setSendersFileTransferReference("somethingElse");
        file3.setFileTransferId(fileWithMessageChannelAsSomethingElse);

        Mockito.when(brokerApiClient.getDetails(fileWithRandomMessageChannel.toString())).thenReturn(file1);
        Mockito.when(brokerApiClient.getDetails(fileWithMessageChannelAsMessageChannel.toString())).thenReturn(file2);
        Mockito.when(brokerApiClient.getDetails(fileWithMessageChannelAsSomethingElse.toString())).thenReturn(file3);

        Mockito.when(brokerApiClient.getAvailableFiles()).thenReturn(
            new UUID[] {fileWithRandomMessageChannel, fileWithMessageChannelAsMessageChannel, fileWithMessageChannelAsSomethingElse});
    }

    @Test
    public void getOnlyMessagesWithSameMessageChannelAsConfiguration(){
        Mockito.when(integrasjonspunktProperties.getDpo()).thenReturn(new AltinnFormidlingsTjenestenConfig().setMessageChannel("messageChannel"));

        UUID[] result = altinnDownloadService.getAvailableFiles();

        assertThat(result)
            .as("Should only return messages with same message channel")
            .containsExactlyInAnyOrder(fileWithMessageChannelAsMessageChannel);
    }

    @Test
    public void getOnlyMessagesWithNoSpecifiedMessageChannel(){
        Mockito.when(integrasjonspunktProperties.getDpo()).thenReturn(new AltinnFormidlingsTjenestenConfig());

        UUID[] result = altinnDownloadService.getAvailableFiles();

        assertThat(result)
            .as("Should only return messages without specified message channel")
            .containsExactlyInAnyOrder(fileWithRandomMessageChannel);
    }

    @Test
    public void downloadShouldCallBrokerApiClient() throws JAXBException, IOException {
        UUID uuid = UUID.randomUUID();
        byte[] bytes = "Hello world".getBytes();

        Mockito.when(brokerApiClient.downloadFile(uuid)).thenReturn(bytes);
        altinnDownloadService.download(new DownloadRequest(uuid, "123"));

        verify(brokerApiClient).downloadFile(uuid);
    }

    @Test
    public void downloadShouldCallZipUtils() throws JAXBException, IOException {
        UUID uuid = UUID.randomUUID();
        byte[] bytes = "Hello world".getBytes();

        Mockito.when(brokerApiClient.downloadFile(uuid)).thenReturn(bytes);
        altinnDownloadService.download(new DownloadRequest(uuid, "123"));

        verify(zipUtils).getAltinnPackage(bytes);
    }

    @Test
    public void confirmDownloadShouldCallBrokerApiClient(){
        UUID uuid = UUID.randomUUID();

        altinnDownloadService.confirmDownload(new DownloadRequest(uuid, "123"));

        verify(brokerApiClient).confirmDownload(uuid);
    }

}
