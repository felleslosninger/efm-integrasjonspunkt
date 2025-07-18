package no.difi.meldingsutveksling.altinnv3.dpo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.AltinnTokenUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class BrokerApiClient {

    private final AltinnTokenUtil tokenUtil;
    private final RestClient restClient = RestClient.builder().defaultStatusHandler(HttpStatusCode::isError, this::getBrokerApiException).build();

    private final String readScope = "altinn:broker.read";
    private final String writeScope = "altinn:broker.write";
    private final String serviceOwnerScope = "altinn:serviceowner";
    private final IntegrasjonspunktProperties props;

    public FileTransferOverviewExt send(FileTransferInitalizeExt request, byte[] bytes){
        FileTransferInitializeResponseExt initializeResponse = initialize(request);

        if(initializeResponse == null) throw new BrokerApiException("Error while initializing file transfer. Result from initialize call to Altinn was null.");
        if(initializeResponse.getFileTransferId() == null) throw new BrokerApiException("Error while initializing file transfer. Result from initialize call to Altinn did not have FileTransferId.");

        return upload(initializeResponse.getFileTransferId(), bytes);
    }

    public FileTransferInitializeResponseExt initialize(FileTransferInitalizeExt request) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(writeScope, serviceOwnerScope));

         return restClient.post()
            .uri(props.getDpo().getBrokerserviceUrl() + "/filetransfer/")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .body(request)
            .retrieve()
            .body(FileTransferInitializeResponseExt.class)
            ;
    }

    public FileTransferOverviewExt upload(UUID fileTransferId, byte[] bytes) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(writeScope, serviceOwnerScope));

        return restClient.post()
            .uri(props.getDpo().getBrokerserviceUrl() + "/filetransfer/{fileTransferId}/upload", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/octet-stream")
            .body(bytes)
            .retrieve()
            .body(FileTransferOverviewExt.class)
            ;
    }

    public UUID[] getAvailableFiles() {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, serviceOwnerScope));

        return restClient.get()
            .uri(props.getDpo().getBrokerserviceUrl() + "/filetransfer?resourceId={resourceId}&status={status}&recipientStatus={recipientStatus}",
                props.getDpo().getResource(),
                FileTransferStatusExtNullable.PUBLISHED.getValue(),
                RecipientFileTransferStatusExtNullable.INITIALIZED.getValue())
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(UUID[].class)
            ;
    }

    public FileTransferStatusDetailsExt getDetails(String fileTransferId) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, serviceOwnerScope));

         return restClient.get()
            .uri(props.getDpo().getBrokerserviceUrl() + "/filetransfer/{fileTransferId}/details", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(FileTransferStatusDetailsExt.class)
            ;
    }

    public byte[] downloadFile(UUID fileTransferId) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, serviceOwnerScope));

        return restClient.get()
            .uri(props.getDpo().getBrokerserviceUrl() + "/filetransfer/{fileTransferId}/download", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(byte[].class)
            ;
    }

    public void confirmDownload(UUID fileTransferId) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, serviceOwnerScope));

        restClient.post()
            .uri(props.getDpo().getBrokerserviceUrl() + "/filetransfer/{fileTransferId}/confirmdownload", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .toBodilessEntity()
            ;
    }

    private void getBrokerApiException(HttpRequest request, ClientHttpResponse response) {
        try {
            String prefix = "Broker api error: %s %s".formatted(request.getURI(), request.getURI().getPath());

            ObjectMapper mapper = new ObjectMapper();
            byte[] body = response.getBody().readAllBytes();

            ProblemDetails problemDetails = mapper.readValue(body, ProblemDetails.class);

            log.error(problemDetails.toString()); //todo change?

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
