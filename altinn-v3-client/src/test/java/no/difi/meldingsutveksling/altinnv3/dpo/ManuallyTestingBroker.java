package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.AltinnConfig;
import no.difi.meldingsutveksling.altinnv3.AltinnTokenUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.FileTransferInitalizeExt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
@SpringBootTest(classes = {
    AltinnConfig.class,
    BrokerApiClient.class,
    AltinnTokenUtil.class,
    IntegrasjonspunktProperties.class
})
@ConfigurationPropertiesScan
public class ManuallyTestingBroker {

    @Inject
    BrokerApiClient client;

    @Inject
    AltinnTokenUtil tokenUtil;

    @Inject
    IntegrasjonspunktProperties integrasjonspunktProperties;

    @Test
    void testProperties() {
        assertEquals("991825827", integrasjonspunktProperties.getOrg().getNumber(), "Finner ikke kjent organisasjon!");
    }

    @Test
    void testAltinnToken() {
        var altinnToken = tokenUtil.retrieveAltinnAccessToken(List.of("altinn:broker.write","altinn:broker.read","altinn:serviceowner"));
        assertNotNull(altinnToken, "AltinnToken is null");
    }

    @Test
    void testListFiles() {
        var uuids = client.getAvailableFiles();
        assertNotNull(uuids);
        assertNotEquals(0, uuids.length);
        Arrays.stream(uuids).forEach(System.out::println);
    }

    @Test
    void testFileDetails() {
        var details = client.getDetails("3c5c1d8e-3fda-4095-856c-da704bd9f9a5");
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
    void upploadFile() {
        var fileTransfer = new FileTransferInitalizeExt();
        fileTransfer.fileName("test.txt");
        fileTransfer.setResourceId("eformidling-meldingsteneste-test");
        fileTransfer.setSender("0192:991825827");
        fileTransfer.setRecipients(List.of("0192:991825827"));
        fileTransfer.setSendersFileTransferReference("string");
        var uuid = client.initialize(fileTransfer).getFileTransferId();
        var result = client.upload(uuid, "Just some text data from upploadFile() test".getBytes());
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

        byte[] result = client.downloadFile(UUID.fromString("b4e9ae47-806f-46e6-ad1c-e1fddc0b4d0a"));

        String message = new String(result);
        System.out.println(message);
    }

    @Test
    void confirmDownloadFile() {
        client.confirmDownload(UUID.fromString("b4e9ae47-806f-46e6-ad1c-e1fddc0b4d0a"));
    }


}
