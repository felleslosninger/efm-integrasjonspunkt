package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.xml.bind.JAXBException;
import no.difi.meldingsutveksling.altinnv3.dpo.payload.ZipUtils;
import no.difi.meldingsutveksling.config.AltinnFormidlingsTjenestenConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.FileTransferOverviewExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AltinnDownloadServiceTest {

    @Mock
    private BrokerApiClient brokerApiClient;

    @Mock
    private IntegrasjonspunktProperties integrasjonspunktProperties;

    @Mock
    private ZipUtils zipUtils;

    @InjectMocks
    private AltinnDPODownloadService altinnDownloadService;

    private final UUID fileWithRandomMessageChannel = UUID.fromString("dc11ecad-dc5c-44a3-b566-b460485580f8");
    private final UUID fileWithMessageChannelAsMessageChannel = UUID.fromString("bbcc4621-d88f-4a94-ae2f-b38072bf5087");
    private final UUID fileWithMessageChannelAsSomethingElse = UUID.fromString("97d7380a-217b-4873-abe8-1530a512d7b6");

    @BeforeEach
    public void beforeAll() {
        FileTransferOverviewExt file1 = new FileTransferOverviewExt();
        file1.setSendersFileTransferReference(UUID.randomUUID().toString());
        file1.setFileTransferId(fileWithRandomMessageChannel);

        FileTransferOverviewExt file2 = new FileTransferOverviewExt();
        file2.setSendersFileTransferReference("messageChannel");
        file2.setFileTransferId(fileWithMessageChannelAsMessageChannel);

        FileTransferOverviewExt file3 = new FileTransferOverviewExt();
        file3.setSendersFileTransferReference("somethingElse");
        file3.setFileTransferId(fileWithMessageChannelAsSomethingElse);

        Mockito.when(brokerApiClient.getDetails(any(), eq(fileWithRandomMessageChannel.toString()))).thenReturn(file1);
        Mockito.when(brokerApiClient.getDetails(any(), eq(fileWithMessageChannelAsMessageChannel.toString()))).thenReturn(file2);
        Mockito.when(brokerApiClient.getDetails(any(), eq(fileWithMessageChannelAsSomethingElse.toString()))).thenReturn(file3);

        Mockito.when(brokerApiClient.getAvailableFiles(any())).thenReturn(
            new UUID[] {fileWithRandomMessageChannel, fileWithMessageChannelAsMessageChannel, fileWithMessageChannelAsSomethingElse});

        Mockito.when(integrasjonspunktProperties.getDpo()).thenReturn(new AltinnFormidlingsTjenestenConfig());
    }

    @Test
    public void getOnlyMessagesWithSameMessageChannelAsConfiguration(){
        Mockito.when(integrasjonspunktProperties.getDpo()).thenReturn(new AltinnFormidlingsTjenestenConfig().setMessageChannel("messageChannel"));

        UUID[] result = altinnDownloadService.getAvailableFiles(integrasjonspunktProperties.getDpo().getSystemUser());

        assertThat(result)
            .as("Should only return messages with same message channel")
            .containsExactlyInAnyOrder(fileWithMessageChannelAsMessageChannel);
    }

    @Test
    public void getOnlyMessagesWithNoSpecifiedMessageChannel(){
        UUID[] result = altinnDownloadService.getAvailableFiles(integrasjonspunktProperties.getDpo().getSystemUser());

        assertThat(result)
            .as("Should only return messages without specified message channel")
            .containsExactlyInAnyOrder(fileWithRandomMessageChannel);
    }

    @Test
    public void downloadShouldCallBrokerApiClient() {
        UUID uuid = UUID.randomUUID();
        byte[] bytes = "Hello world".getBytes();

        Mockito.when(brokerApiClient.downloadFile(any(), eq(uuid))).thenReturn(bytes);
        altinnDownloadService.download(integrasjonspunktProperties.getDpo().getSystemUser()
            , new DownloadRequest(uuid, "123")
        );

        verify(brokerApiClient).downloadFile(any(), eq(uuid));
    }

    @Test
    public void downloadShouldCallZipUtils() throws JAXBException, IOException {
        UUID uuid = UUID.randomUUID();
        byte[] bytes = "Hello world".getBytes();

        Mockito.when(brokerApiClient.downloadFile(any(), eq(uuid))).thenReturn(bytes);
        altinnDownloadService.download(integrasjonspunktProperties.getDpo().getSystemUser(),
            new DownloadRequest(uuid, "123")
        );

        verify(zipUtils).getAltinnPackage(bytes);
    }

    @Test
    public void confirmDownloadShouldCallBrokerApiClient(){
        UUID uuid = UUID.randomUUID();

        altinnDownloadService.confirmDownload(integrasjonspunktProperties.getDpo().getSystemUser(),
            new DownloadRequest(uuid, "123")
        );

        verify(brokerApiClient).confirmDownload(any(), eq(uuid));
    }

    @Test
    public void shouldSortListReturnedFromAltinn(){
        UUID uuidFirst = UUID.fromString("7e2b1a2c-8c3a-4e2a-9f1a-2b6e4c8d9a1b");
        UUID uuidSecond = UUID.fromString("3c5f2d4e-1b7a-4c8e-8f2a-7d9e1b2c3a4f");
        UUID uuidThird = UUID.fromString("9a8b7c6d-5e4f-3a2b-1c0d-8e7f6a5b4c3d");

        var first = new FileTransferOverviewExt();
        first.setCreated(OffsetDateTime.parse("2010-01-01T00:00:01Z"));
        first.setFileTransferId(uuidFirst);
        first.setSendersFileTransferReference(UUID.randomUUID().toString());

        var second = new FileTransferOverviewExt();
        second.setCreated(OffsetDateTime.parse("2010-01-01T00:00:02Z"));
        second.setFileTransferId(uuidSecond);
        second.setSendersFileTransferReference(UUID.randomUUID().toString());

        var third = new FileTransferOverviewExt();
        third.setCreated(OffsetDateTime.parse("2010-01-01T00:00:03Z"));
        third.setFileTransferId(uuidThird);
        third.setSendersFileTransferReference(UUID.randomUUID().toString());


        Mockito.when(brokerApiClient.getAvailableFiles(any())).thenReturn(new UUID[] {uuidThird, uuidFirst, uuidSecond}); // return list in random order
        Mockito.when(brokerApiClient.getDetails(any(), eq(uuidFirst.toString()))).thenReturn(first);
        Mockito.when(brokerApiClient.getDetails(any(), eq(uuidSecond.toString()))).thenReturn(second);
        Mockito.when(brokerApiClient.getDetails(any(), eq(uuidThird.toString()))).thenReturn(third);

        UUID[] result = altinnDownloadService.getAvailableFiles(integrasjonspunktProperties.getDpo().getSystemUser());

        var expected = new UUID[] {uuidFirst, uuidSecond, uuidThird};

        assertThat(result).isEqualTo(expected)
            .as("Should order the list returned from altinn based upon created date, oldest first");;
    }
}
