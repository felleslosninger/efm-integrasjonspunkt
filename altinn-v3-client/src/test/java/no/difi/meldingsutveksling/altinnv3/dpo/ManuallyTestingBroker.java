package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.UseFullTestConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.AltinnConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.DpoTokenProducer;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.FileTransferInitalizeExt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
@SpringBootTest(classes = {
    BrokerApiClient.class,
    DpoTokenProducer.class,
    AltinnConfiguration.class,
    IntegrasjonspunktProperties.class
})
@UseFullTestConfiguration
public class ManuallyTestingBroker {

    @Inject
    BrokerApiClient client;

    @Inject
    DpoTokenProducer dpoTokenProducer;

    @Inject
    IntegrasjonspunktProperties integrasjonspunktProperties;

    @Test
    void testProperties() {
        assertEquals("991825827", integrasjonspunktProperties.getOrg().getNumber(), "Finner ikke kjent organisasjon!");
    }

    @Test
    void testAltinnToken() {
        var altinnToken = dpoTokenProducer.produceToken(List.of("altinn:broker.write","altinn:broker.read","altinn:serviceowner"));
        assertNotNull(altinnToken, "AltinnToken is null");
    }

    @Test
    void testListFiles() {
        var uuids = client.getAvailableFiles();
        assertNotNull(uuids);
        assertEquals(1, uuids.length);
        Arrays.stream(uuids).forEach(System.out::println);
    }

    @Test
    void testFileDetails() {
        var details = client.getDetails("3c10e2ab-4106-425f-8942-a83548a96e47");
        assertNotNull(details);
        System.out.println(details);
    }

    @Test
    void testListDetailsAllFiles() {
        var uuids = client.getAvailableFiles();
        Arrays.stream(uuids).forEach(
            it -> {
                var details = client.getDetails(it.toString());
                System.out.println(details);
            }
        );
    }

    @Test
    void uploadFile() {
        var fileTransfer = new FileTransferInitalizeExt();
        fileTransfer.fileName("test.txt");
        fileTransfer.setResourceId("eformidling-meldingsteneste-test2");
        fileTransfer.setSender("0192:991825827");
        fileTransfer.setRecipients(List.of("0192:991825827"));
        fileTransfer.setSendersFileTransferReference("string");
        var uuid = client.initialize(fileTransfer).getFileTransferId();
        var result = client.upload(uuid, "Just some text data from uploadFile() test".getBytes());
        assertNotNull(result);
        assertNotNull(result.getFileTransferId());
    }

    @Test
    void clearFiles(){
        UUID[] availableFiles = client.getAvailableFiles();
        Arrays.stream(availableFiles).forEach(id -> {
            client.downloadFile(id);
            client.confirmDownload(id);
        });
    }

    @Test
    void downloadFile() {
        byte[] result = client.downloadFile(UUID.fromString("3c10e2ab-4106-425f-8942-a83548a96e47"));
        String message = new String(result);
        assertEquals("Just some text data from uploadFile() test", message);
    }

    @Test
    void confirmDownloadFile() {
        client.confirmDownload(UUID.fromString("28a1d3f7-53db-452f-aa1e-65fe34eb96e8"));
    }

}
