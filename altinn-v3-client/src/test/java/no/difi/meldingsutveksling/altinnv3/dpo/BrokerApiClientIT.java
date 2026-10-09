package no.difi.meldingsutveksling.altinnv3.dpo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import no.difi.meldingsutveksling.altinnv3.token.DpoTokenProducer;
import no.difi.meldingsutveksling.config.AltinnFormidlingsTjenestenConfig;
import no.difi.meldingsutveksling.config.AltinnSystemUser;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.broker.model.FileTransferInitalizeExt;
import no.digdir.altinn3.broker.model.FileTransferOverviewExt;
import no.digdir.altinn3.broker.model.FileTransferStatusExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link BrokerApiClient} against a WireMock stand-in for the Altinn Broker API.
 */
@Isolated
class BrokerApiClientIT {

    private static final String TOKEN = "test-token";
    private static final String RESOURCE = "eformidling-dpo-meldingsutveksling";
    private static final String BROKER_BASE_PATH = "/broker/api/v1";

    private WireMockServer wireMockServer;
    private DpoTokenProducer tokenProducer;
    private BrokerApiClient client;
    private AltinnSystemUser systemUser;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        tokenProducer = mock(DpoTokenProducer.class);
        when(tokenProducer.produceToken(any(), anyList())).thenReturn(TOKEN);

        var dpoConfig = new AltinnFormidlingsTjenestenConfig();
        dpoConfig.setBrokerserviceUrl(wireMockServer.baseUrl() + BROKER_BASE_PATH);
        dpoConfig.setResource(RESOURCE);
        var props = mock(IntegrasjonspunktProperties.class);
        when(props.getDpo()).thenReturn(dpoConfig);

        systemUser = new AltinnSystemUser().setOrgId("0192:111111111").setName("111111111_integrasjonspunkt_systembruker_test");

        client = new BrokerApiClient(tokenProducer, props);
        client.init();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void initializePostsRequestWithWriteTokenAndReturnsFileTransferId() {
        var fileTransferId = UUID.randomUUID();
        wireMockServer.stubFor(post(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"fileTransferId\":\"%s\"}".formatted(fileTransferId))));

        var response = client.initialize(systemUser, initRequest());

        assertEquals(fileTransferId, response.getFileTransferId());
        verify(tokenProducer).produceToken(systemUser, List.of("altinn:broker.write"));
        wireMockServer.verify(postRequestedFor(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/"))
            .withHeader("Authorization", equalTo("Bearer " + TOKEN))
            .withHeader("Accept", equalTo("application/json"))
            .withRequestBody(equalToJson("""
                {
                  "fileName": "arkivmelding.zip",
                  "resourceId": "%s",
                  "sender": "0192:111111111",
                  "sendersFileTransferReference": "ref-1",
                  "recipients": ["0192:222222222"]
                }
                """.formatted(RESOURCE), true, true)));
    }

    @Test
    void uploadPostsOctetStreamWithWriteToken() {
        var fileTransferId = UUID.randomUUID();
        var bytes = new byte[]{1, 2, 3, 4};
        wireMockServer.stubFor(post(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/" + fileTransferId + "/upload"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"fileTransferId\":\"%s\",\"fileTransferStatus\":\"Published\"}".formatted(fileTransferId))));

        FileTransferOverviewExt overview = client.upload(systemUser, fileTransferId, bytes);

        assertEquals(fileTransferId, overview.getFileTransferId());
        assertEquals(FileTransferStatusExt.PUBLISHED, overview.getFileTransferStatus());
        verify(tokenProducer).produceToken(systemUser, List.of("altinn:broker.write"));
        wireMockServer.verify(postRequestedFor(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/" + fileTransferId + "/upload"))
            .withHeader("Authorization", equalTo("Bearer " + TOKEN))
            .withHeader("Content-Type", containing("application/octet-stream"))
            .withRequestBody(WireMock.binaryEqualTo(bytes)));
    }

    @Test
    void sendInitializesThenUploads() {
        var fileTransferId = UUID.randomUUID();
        wireMockServer.stubFor(post(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"fileTransferId\":\"%s\"}".formatted(fileTransferId))));
        wireMockServer.stubFor(post(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/" + fileTransferId + "/upload"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"fileTransferId\":\"%s\"}".formatted(fileTransferId))));

        var overview = client.send(systemUser, initRequest(), new byte[]{9});

        assertEquals(fileTransferId, overview.getFileTransferId());
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/")));
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/" + fileTransferId + "/upload")));
    }

    @Test
    void sendFailsWhenInitializeResponseHasNoFileTransferId() {
        wireMockServer.stubFor(post(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{}")));

        var exception = assertThrows(BrokerApiException.class, () -> client.send(systemUser, initRequest(), new byte[]{9}));

        assertTrue(exception.getMessage().contains("FileTransferId"), exception.getMessage());
        wireMockServer.verify(1, postRequestedFor(WireMock.urlMatching(".*")));
    }

    @Test
    void getAvailableFilesQueriesPublishedAndInitializedWithReadToken() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        wireMockServer.stubFor(get(urlPathEqualTo(BROKER_BASE_PATH + "/filetransfer"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("[\"%s\",\"%s\"]".formatted(first, second))));

        var files = client.getAvailableFiles(systemUser);

        assertArrayEquals(new UUID[]{first, second}, files);
        verify(tokenProducer).produceToken(systemUser, List.of("altinn:broker.read"));
        wireMockServer.verify(getRequestedFor(urlPathEqualTo(BROKER_BASE_PATH + "/filetransfer"))
            .withQueryParam("resourceId", equalTo(RESOURCE))
            .withQueryParam("status", equalTo("Published"))
            .withQueryParam("recipientStatus", equalTo("Initialized"))
            .withHeader("Authorization", equalTo("Bearer " + TOKEN)));
    }

    @Test
    void getAvailableFilesReturnsEmptyArrayWhenNothingIsAvailable() {
        wireMockServer.stubFor(get(urlPathEqualTo(BROKER_BASE_PATH + "/filetransfer"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));

        assertEquals(0, client.getAvailableFiles(systemUser).length);
    }

    @Test
    void getDetailsReturnsOverview() {
        var fileTransferId = UUID.randomUUID();
        wireMockServer.stubFor(get(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/" + fileTransferId))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"fileTransferId":"%s","resourceId":"%s","fileName":"arkivmelding.zip","fileTransferStatus":"UploadProcessing"}
                    """.formatted(fileTransferId, RESOURCE))));

        var overview = client.getDetails(systemUser, fileTransferId.toString());

        assertEquals(fileTransferId, overview.getFileTransferId());
        assertEquals(RESOURCE, overview.getResourceId());
        assertEquals("arkivmelding.zip", overview.getFileName());
        assertEquals(FileTransferStatusExt.UPLOAD_PROCESSING, overview.getFileTransferStatus());
        verify(tokenProducer).produceToken(systemUser, List.of("altinn:broker.read"));
    }

    @Test
    void downloadFileReturnsBytes() {
        var fileTransferId = UUID.randomUUID();
        var content = new byte[]{10, 20, 30, 40, 50};
        wireMockServer.stubFor(get(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/" + fileTransferId + "/download"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/octet-stream")
                .withBody(content)));

        assertArrayEquals(content, client.downloadFile(systemUser, fileTransferId));
        wireMockServer.verify(getRequestedFor(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/" + fileTransferId + "/download"))
            .withHeader("Authorization", equalTo("Bearer " + TOKEN)));
    }

    @Test
    void confirmDownloadPostsToConfirmEndpoint() {
        var fileTransferId = UUID.randomUUID();
        wireMockServer.stubFor(post(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/" + fileTransferId + "/confirmdownload"))
            .willReturn(aResponse().withStatus(200)));

        client.confirmDownload(systemUser, fileTransferId);

        verify(tokenProducer).produceToken(systemUser, List.of("altinn:broker.read"));
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/" + fileTransferId + "/confirmdownload"))
            .withHeader("Authorization", equalTo("Bearer " + TOKEN)));
    }

    @Test
    void errorResponseWithBodyIsThrownAsBrokerApiException() {
        var fileTransferId = UUID.randomUUID();
        wireMockServer.stubFor(get(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/" + fileTransferId))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("{\"title\":\"Not Found\",\"detail\":\"File transfer not found\"}")));

        var exception = assertThrows(BrokerApiException.class, () -> client.getDetails(systemUser, fileTransferId.toString()));

        assertTrue(exception.getMessage().contains("Broker api error"), exception.getMessage());
        assertTrue(exception.getMessage().contains("File transfer not found"), exception.getMessage());
    }

    @Test
    void errorResponseWithoutBodyIsThrownAsBrokerApiException() {
        var fileTransferId = UUID.randomUUID();
        wireMockServer.stubFor(post(urlEqualTo(BROKER_BASE_PATH + "/filetransfer/" + fileTransferId + "/confirmdownload"))
            .willReturn(aResponse().withStatus(500)));

        var exception = assertThrows(BrokerApiException.class, () -> client.confirmDownload(systemUser, fileTransferId));

        assertTrue(exception.getMessage().contains("500"), exception.getMessage());
        assertTrue(exception.getMessage().contains("No body returned"), exception.getMessage());
    }

    private FileTransferInitalizeExt initRequest() {
        var request = new FileTransferInitalizeExt();
        request.setFileName("arkivmelding.zip");
        request.setResourceId(RESOURCE);
        request.setSender("0192:111111111");
        request.setSendersFileTransferReference("ref-1");
        request.setRecipients(List.of("0192:222222222"));
        return request;
    }
}
