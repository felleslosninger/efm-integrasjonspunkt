package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.ProblemDetailsParser;
import no.difi.meldingsutveksling.altinnv3.token.DpoTokenProducer;
import no.difi.meldingsutveksling.config.AltinnSystemUser;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class BrokerApiClient {

    private final DpoTokenProducer tokenProducer;
    private final IntegrasjonspunktProperties props;

    private RestClient restClient = RestClient.builder().defaultStatusHandler(HttpStatusCode::isError, this::getBrokerApiException).build();

    private static String readScope = "altinn:broker.read";
    private static String writeScope = "altinn:broker.write";

    private String brokerServiceUrl;

    @PostConstruct
    public void init() {
        brokerServiceUrl = props.getDpo().getBrokerserviceUrl();
    }

    public FileTransferOverviewExt send(AltinnSystemUser systemUser, FileTransferInitalizeExt request, byte[] bytes){
        FileTransferInitializeResponseExt initializeResponse = initialize(systemUser, request);
        if (initializeResponse == null) throw new BrokerApiException("Error while initializing file transfer. Result from initialize call to Altinn was null.");
        if (initializeResponse.getFileTransferId() == null) throw new BrokerApiException("Error while initializing file transfer. Result from initialize call to Altinn did not have FileTransferId.");
        return upload(systemUser, initializeResponse.getFileTransferId(), bytes);
    }

    public FileTransferInitializeResponseExt initialize(AltinnSystemUser systemUser, FileTransferInitalizeExt request) {
        String accessToken = tokenProducer.produceToken(systemUser, List.of(writeScope));

         return restClient.post()
            .uri(brokerServiceUrl + "/filetransfer/")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .body(request)
            .retrieve()
            .body(FileTransferInitializeResponseExt.class)
            ;
    }

    public FileTransferOverviewExt upload(AltinnSystemUser systemUser, UUID fileTransferId, byte[] bytes) {
        String accessToken = tokenProducer.produceToken(systemUser, List.of(writeScope));

        return restClient.post()
            .uri(brokerServiceUrl + "/filetransfer/{fileTransferId}/upload", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/octet-stream")
            .body(bytes)
            .retrieve()
            .body(FileTransferOverviewExt.class)
            ;
    }

    public UUID[] getAvailableFiles(AltinnSystemUser systemUser) {
        String accessToken = tokenProducer.produceToken(systemUser, List.of(readScope));

        return restClient.get()
            .uri(brokerServiceUrl + "/filetransfer?resourceId={resourceId}&status={status}&recipientStatus={recipientStatus}",
                props.getDpo().getResource(),
                FileTransferStatusExtNullable.PUBLISHED.getValue(),
                RecipientFileTransferStatusExtNullable.INITIALIZED.getValue())
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(UUID[].class)
            ;
    }

    public FileTransferOverviewExt getDetails(AltinnSystemUser systemUser, String fileTransferId) {
        String accessToken = tokenProducer.produceToken(systemUser, List.of(readScope));

         return restClient.get()
            .uri(brokerServiceUrl + "/filetransfer/{fileTransferId}", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(FileTransferOverviewExt.class)
            ;
    }

    public byte[] downloadFile(AltinnSystemUser systemUser, UUID fileTransferId) {
        String accessToken = tokenProducer.produceToken(systemUser, List.of(readScope));

        return restClient.get()
            .uri(brokerServiceUrl + "/filetransfer/{fileTransferId}/download", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(byte[].class)
            ;
    }

    public void confirmDownload(AltinnSystemUser systemUser, UUID fileTransferId) {
        String accessToken = tokenProducer.produceToken(systemUser, List.of(readScope));

        restClient.post()
            .uri(brokerServiceUrl + "/filetransfer/{fileTransferId}/confirmdownload", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .toBodilessEntity()
            ;
    }

    private void getBrokerApiException(HttpRequest request, ClientHttpResponse response) {
        var prefix = "Broker api error: %s %s".formatted(request.getURI(), request.getURI().getPath());
        var details = ProblemDetailsParser.parseClientHttpResponse(prefix, response);
        throw new BrokerApiException(details);
    }

}
