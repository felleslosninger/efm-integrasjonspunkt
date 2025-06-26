package no.difi.meldingsutveksling.altinnv3.DPO;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.AltinnTokenUtil;
import no.digdir.altinn3.broker.model.*;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
//@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class BrokerApiClient {

    private final AltinnTokenUtil tokenUtil;
    private final RestClient restClient = RestClient.builder().defaultStatusHandler(HttpStatusCode::isError, this::getBrokerApiException).build();

    private final String readScope = "altinn:broker.read";
    private final String writeScope = "altinn:broker.write";
    private final String serviceOwnerScope = "altinn:serviceowner";

    public FileTransferOverviewExt send(FileTransferInitalizeExt request, byte[] bytes){
        UUID fileTransferId = initialize(request);
        FileTransferOverviewExt response = upload(fileTransferId, bytes);

        return response;
    }

    public UUID initialize(FileTransferInitalizeExt request) {

        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, writeScope, serviceOwnerScope));

        var result = restClient.post()
            .uri("https://platform.tt02.altinn.no/broker/api/v1/filetransfer/")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .body(request)
            .retrieve()
            .body(FileTransferInitializeResponseExt.class)
            ;

        return result.getFileTransferId();
    }

    public FileTransferOverviewExt upload(UUID fileTransferId, byte[] bytes) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, writeScope, serviceOwnerScope));

        FileTransferOverviewExt response = restClient.post()
            .uri("https://platform.tt02.altinn.no/broker/api/v1/filetransfer/{fileTransferId}/upload", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/octet-stream")
            .body(bytes)
            .retrieve()
            .body(FileTransferOverviewExt.class)
            ;

        return response;
    }

    public UUID[] getAvailableFiles() {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, serviceOwnerScope));

        UUID[] response = restClient.get()
            .uri("https://platform.tt02.altinn.no/broker/api/v1/filetransfer?resourceId={resourceId}&status={status}&recipientStatus={recipientStatus}",
                "eformidling-meldingsteneste-test",
                FileTransferStatusExtNullable.PUBLISHED.getValue(),
                RecipientFileTransferStatusExtNullable.INITIALIZED.getValue())
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(UUID[].class)
            ;

        return response;
    }

    public FileTransferStatusDetailsExt getDetails(String fileTransferId) {
        try {

            String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, serviceOwnerScope));

            FileTransferStatusDetailsExt response = restClient.get()
                .uri("https://platform.tt02.altinn.no/broker/api/v1/filetransfer/{fileTransferId}/details", fileTransferId)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .retrieve()
                .body(FileTransferStatusDetailsExt.class)
                ;

           // System.out.println(response.toString());

            return response;
        } catch (Exception e) {}
        return null;
    }

    public byte[] downloadFile(UUID fileTransferId) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, writeScope, serviceOwnerScope));

        byte[] response = restClient.get()
            .uri("https://platform.tt02.altinn.no/broker/api/v1/filetransfer/{fileTransferId}/download", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(byte[].class)
            ;

        return response;
    }

    public void confirmDownload(UUID fileTransferId) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, writeScope, serviceOwnerScope));

        var response = restClient.post()
            .uri("https://platform.tt02.altinn.no/broker/api/v1/filetransfer/{fileTransferId}/confirmdownload", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .toBodilessEntity()
            ;

        System.out.println(response.getStatusCode());
    }

    private void getBrokerApiException(HttpRequest request, ClientHttpResponse response) {
        try {
            String prefix = "Broker api error: %s %s".formatted(request.getURI(), request.getURI().getPath());

            ObjectMapper mapper = new ObjectMapper();
            var body = response.getBody().readAllBytes();

            ProblemDetails problemDetails = mapper.readValue(body, ProblemDetails.class);

            log.error(problemDetails.toString());

            if(problemDetails.getType() == null) {
                throw new BrokerApiException("%s: %d: %s".formatted(
                    prefix,
                    response.getStatusCode().value(),
                    problemDetails.getAdditionalProperties().getOrDefault("message", "")
                ));
            }

            throw new BrokerApiException("%s: %d %s, %s".formatted(
                prefix,
                problemDetails.getStatus(),
                problemDetails.getTitle(),
                problemDetails.getDetail()));

        } catch (IOException e) {
            throw new BrokerApiException("Problem while getting broker api exception", e);
        }
    }

}
