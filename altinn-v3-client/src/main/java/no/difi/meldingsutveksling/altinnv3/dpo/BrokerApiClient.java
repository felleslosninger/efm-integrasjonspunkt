package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.ProblemDetailsParser;
import no.difi.meldingsutveksling.altinnv3.token.TokenProducer;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.*;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Qualifier("DpoTokenProducer")
    private final TokenProducer tokenProducer;
    private final IntegrasjonspunktProperties props;

    private RestClient restClient = RestClient.builder().defaultStatusHandler(HttpStatusCode::isError, this::getBrokerApiException).build();

    private static String readScope = "altinn:broker.read";
    private static String writeScope = "altinn:broker.write";
    private static String serviceOwnerScope = "altinn:serviceowner"; // FIXME should not be needed, read/write should be enough

    private String brokerServiceUrl;

    @PostConstruct
    public void init() {
        brokerServiceUrl = props.getDpo().getBrokerserviceUrl();
    }

    public FileTransferOverviewExt send(FileTransferInitalizeExt request, byte[] bytes){
        FileTransferInitializeResponseExt initializeResponse = initialize(request);
        if (initializeResponse == null) throw new BrokerApiException("Error while initializing file transfer. Result from initialize call to Altinn was null.");
        if (initializeResponse.getFileTransferId() == null) throw new BrokerApiException("Error while initializing file transfer. Result from initialize call to Altinn did not have FileTransferId.");
        return upload(initializeResponse.getFileTransferId(), bytes);
    }

    public FileTransferInitializeResponseExt initialize(FileTransferInitalizeExt request) {
        String accessToken = tokenProducer.produceToken(List.of(writeScope, serviceOwnerScope));

         return restClient.post()
            .uri(brokerServiceUrl + "/filetransfer/")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .body(request)
            .retrieve()
            .body(FileTransferInitializeResponseExt.class)
            ;
    }

    public FileTransferOverviewExt upload(UUID fileTransferId, byte[] bytes) {
        String accessToken = tokenProducer.produceToken(List.of(writeScope, serviceOwnerScope));

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

    public UUID[] getAvailableFiles() {
        String accessToken = tokenProducer.produceToken(List.of(readScope, serviceOwnerScope));

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

    public FileTransferStatusDetailsExt getDetails(String fileTransferId) {
        String accessToken = tokenProducer.produceToken(List.of(readScope, serviceOwnerScope));

         return restClient.get()
            .uri(brokerServiceUrl + "/filetransfer/{fileTransferId}/details", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(FileTransferStatusDetailsExt.class)
            ;
    }

    public byte[] downloadFile(UUID fileTransferId) {
        String accessToken = tokenProducer.produceToken(List.of(readScope, serviceOwnerScope));

        return restClient.get()
            .uri(brokerServiceUrl + "/filetransfer/{fileTransferId}/download", fileTransferId)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(byte[].class)
            ;
    }

    public void confirmDownload(UUID fileTransferId) {
        String accessToken = tokenProducer.produceToken(List.of(readScope, serviceOwnerScope));

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
