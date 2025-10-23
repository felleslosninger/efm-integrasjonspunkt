package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.UseFullTestConfiguration;
import no.difi.meldingsutveksling.altinnv3.systemregister.SystemuserTokenProducer;
import no.difi.meldingsutveksling.altinnv3.token.AltinnConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.DpoTokenProducer;
import no.difi.meldingsutveksling.altinnv3.token.SystemUserTokenProducer;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.FileTransferInitalizeExt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest(classes = {
    BrokerApiClient.class,
    DpoTokenProducer.class,
    SystemuserTokenProducer.class,
    SystemUserTokenProducer.class,
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
        var altinnToken = dpoTokenProducer.produceToken(List.of("altinn:broker.write","altinn:broker.read"));
        System.out.println(altinnToken);
        assertNotNull(altinnToken, "AltinnToken is null");
    }

    @Test
    void testListFiles() {
        var uuids = client.getAvailableFiles();
        assertNotNull(uuids);
        Arrays.stream(uuids).forEach(System.out::println);
        assertEquals(2, uuids.length);
    }

    @Test
    void testFileDetails() {
        var details = client.getDetails("5d59e5ef-6723-45d4-9f9f-2cb4664230a1");
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
        var resource_id = integrasjonspunktProperties.getDpo().getResource();
        var fileTransfer = new FileTransferInitalizeExt();
        fileTransfer.fileName("test.txt");
        fileTransfer.setResourceId(resource_id);
        fileTransfer.setSender("0192:" + integrasjonspunktProperties.getOrg().getNumber());
        fileTransfer.setRecipients(List.of("0192:314240979"));
        fileTransfer.setSendersFileTransferReference("string");
        var uuid = client.initialize(fileTransfer).getFileTransferId();
        var result = client.upload(uuid, "Just some text data from uploadFile() test".getBytes());
        assertNotNull(result);
        assertNotNull(result.getFileTransferId());
    }

    @Test
    void clearFiles(){
        UUID[] availableFiles = client.getAvailableFiles();
        Arrays.stream(availableFiles).peek(p -> System.out.println(p)).forEach(id -> {
            client.downloadFile(id);
            client.confirmDownload(id);
        });
    }

    @Test
    void downloadFile() {
        byte[] result = client.downloadFile(UUID.fromString("e06aa73f-fe4d-40eb-90c1-58a431e38f1c"));
        String message = new String(result);
        assertEquals("Just some text data from uploadFile() test", message);
    }

    @Test
    void confirmDownloadFile() {
        client.confirmDownload(UUID.fromString("e06aa73f-fe4d-40eb-90c1-58a431e38f1c"));
    }

}
