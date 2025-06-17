package no.difi.meldingsutveksling.altinnv3;

import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.digdir.altinn3.broker.model.FileTransferInitalizeExt;
import no.digdir.altinn3.broker.model.FileTransferInitializeResponseExt;
import no.digdir.altinn3.broker.model.FileTransferOverviewExt;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondencesResponseExt;
import org.eclipse.angus.mail.iap.ByteArray;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class BrokerApiClient {

    private final RestClient restClient = RestClient.create();

    public UUID initialize(FileTransferInitalizeExt request) throws IOException, InterruptedException, JOSEException {


        String accessToken = TokenUtil.retrieveAccessToken("altinn:broker.write altinn:broker.read altinn:serviceowner");


        var response = restClient.post()
            .uri("https://platform.tt02.altinn.no/broker/api/v1/filetransfer/")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .body(request)
            .retrieve()
            .body(FileTransferInitializeResponseExt.class)
            ;

        return response.getFileTransferId();
    }

    public FileTransferOverviewExt upload(UUID fileTransferId, byte[] bytes) throws IOException, InterruptedException, JOSEException {
        String accessToken = TokenUtil.retrieveAccessToken("altinn:broker.write altinn:broker.read altinn:serviceowner");

        FileTransferOverviewExt response = restClient.post()
            .uri("https://platform.tt02.altinn.no/broker/api/v1/filetransfer/{fileTransferId}", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/octet-stream")
            .body(bytes)
            .retrieve()
            .body(FileTransferOverviewExt.class)
            ;

        return response;
    }
}
