package no.difi.meldingsutveksling.altinnv3.DPO;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.AltinnConfig;
import no.difi.meldingsutveksling.altinnv3.AltinnTokenUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.FileTransferInitalizeExt;
import no.digdir.altinn3.broker.model.FileTransferOverviewExt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

@Disabled
@SpringBootTest(classes = {
AltinnConfig.class,
AltinnBroker.class,
BrokerApiClient.class,
AltinnTokenUtil.class,
IntegrasjonspunktProperties .class
})
public class BrokerApiClientTest {

    @Inject
    BrokerApiClient brokerApiClient;

    @Test
    void test() {

        FileTransferInitalizeExt fileTransferInitalizeExt = new FileTransferInitalizeExt();
        fileTransferInitalizeExt.fileName("Hello.txt");
        fileTransferInitalizeExt.setResourceId("eformidling-meldingsteneste-test");
        fileTransferInitalizeExt.setSender("0192:991825827");
        //fileTransferInitalizeExt.setRecipients(List.of("0192:310654302"));
        fileTransferInitalizeExt.setRecipients(List.of("0192:991825827"));
        fileTransferInitalizeExt.setSendersFileTransferReference(UUID.randomUUID().toString());

        UUID fileTransferId = brokerApiClient.initialize(fileTransferInitalizeExt);
        FileTransferOverviewExt response = brokerApiClient.upload(fileTransferId, "Hello world".getBytes());


    }

    @Test
    void getAvailableFiles() {

        UUID[] availableFiles = brokerApiClient.getAvailableFiles();
    }

    @Test
    void downlaodFile() {

        byte[] result = brokerApiClient.downloadFile(UUID.fromString("b4e9ae47-806f-46e6-ad1c-e1fddc0b4d0a"));

        String message = new String(result);
        System.out.println(message);
    }

    @Test
    void confirmDownloadFile() {

        brokerApiClient.confirmDownload(UUID.fromString("b4e9ae47-806f-46e6-ad1c-e1fddc0b4d0a"));
    }
}
