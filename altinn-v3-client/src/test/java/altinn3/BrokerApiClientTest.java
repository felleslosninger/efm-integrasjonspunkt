package altinn3;

import com.nimbusds.jose.JOSEException;
import no.difi.meldingsutveksling.altinnv3.BrokerApiClient;
import no.digdir.altinn3.broker.model.FileTransferInitalizeExt;
import no.digdir.altinn3.broker.model.FileTransferOverviewExt;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class BrokerApiClientTest {
    @Test
    void test() throws IOException, InterruptedException, JOSEException {
        BrokerApiClient brokerApiClient = new BrokerApiClient();

        FileTransferInitalizeExt fileTransferInitalizeExt = new FileTransferInitalizeExt();
        fileTransferInitalizeExt.fileName("Hello.txt");
        fileTransferInitalizeExt.setResourceId("eformidling-meldingsteneste-test");
        fileTransferInitalizeExt.setSender("0192:991825827");
        fileTransferInitalizeExt.setRecipients(List.of("0192:310654302"));
        fileTransferInitalizeExt.setSendersFileTransferReference(UUID.randomUUID().toString());

        UUID fileTransferId = brokerApiClient.initialize(fileTransferInitalizeExt);
        FileTransferOverviewExt response = brokerApiClient.upload(fileTransferId, "Hello world".getBytes());


    }
}
