package no.difi.meldingsutveksling.ks.svarut.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.FiksConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.ks.svarut.ForsendelseStatus;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.DpfSettings;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.BrregPostadresse;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.difi.move.common.dokumentpakking.CreateCMSDocument;
import no.difi.move.common.io.pipe.Reject;
import no.ks.svarut.klient.SvarUtKlientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tester REST-integrasjonen mot SvarUt (difi.move.fiks.ut.type=rest) med de ekte
 * klientene fra fiks-svarut-klient mot en WireMock-server, slik at endringer i
 * klientbiblioteket (HTTP-kall, JSON-format, Jetty/Jackson-versjoner) fanges av CI.
 */
class SvarUtRestClientIT {

    private static final UUID KONTO_ID = UUID.fromString("5c3a7f6e-3a44-4b2b-8a3b-0e6f7c1d2e3f");
    private static final String MESSAGE_ID = "0b6bb8f1-5f3f-4d6b-9b0e-2b8a5d3c1e7a";
    private static final String SENDER = "910075918";
    private static final String RECEIVER = "910076787";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private SvarUtRestClient client;
    private final OptionalCryptoMessagePersister persister = mock(OptionalCryptoMessagePersister.class);
    private final CreateCMSDocument createCMSDocument = mock(CreateCMSDocument.class);
    private final ServiceRegistryLookup serviceRegistry = mock(ServiceRegistryLookup.class);
    private final GetSvarUtMaskinportenToken getSvarUtMaskinportenToken = mock(GetSvarUtMaskinportenToken.class);

    @BeforeEach
    void setUp() throws Exception {
        IntegrasjonspunktProperties properties = new IntegrasjonspunktProperties();
        FiksConfig fiks = new FiksConfig();
        fiks.getUt().setKonteringsKode("kontering");
        fiks.getUt().setEkskluderesFraPrint(Set.of("txt"));
        fiks.getUt().getRest().setEndpointUrl(URI.create(wireMock.baseUrl()).toURL());
        fiks.getUt().getRest().setKontoId(KONTO_ID.toString());
        fiks.getUt().getRest().setIntegrasjonId("integrasjon-id");
        fiks.getUt().getRest().setIntegrasjonPassord("integrasjon-passord");
        properties.setFiks(fiks);

        given(getSvarUtMaskinportenToken.createMaskinportenToken()).willReturn("maskinporten-token");
        given(persister.read(MESSAGE_ID, "arkivmelding.xml")).willReturn(new ClassPathResource("svarut-rest/arkivmelding.xml"));
        given(persister.read(MESSAGE_ID, "hoveddokument.pdf")).willReturn(resource("PDF-innhold"));
        given(persister.read(MESSAGE_ID, "vedlegg.txt")).willReturn(resource("TXT-innhold"));
        given(createCMSDocument.encrypt(any(CreateCMSDocument.Input.class), any(Reject.class)))
            .willAnswer(i -> i.getArgument(0, CreateCMSDocument.Input.class).getResource());
        given(serviceRegistry.getInfoRecord(RECEIVER)).willReturn(new InfoRecord(RECEIVER, "Mottaker kommune", new EntityType("Organisasjonsledd", "ORGL"),
            new BrregPostadresse("Mottakervegen 1", "5000", "Bergen", "Norge")));

        SvarUtRestConfiguration config = new SvarUtRestConfiguration(properties);
        ClientContext clientContext = config.clientContext();
        var authenticationStrategy = config.integrasjonspunktAuthenticationStrategy(getSvarUtMaskinportenToken);
        var httpConfiguration = config.httpConfiguration();
        client = config.svarUtRestClient(
            config.sendKlientV3(authenticationStrategy, httpConfiguration),
            config.fiksRestMapper(serviceRegistry, persister, createCMSDocument, new ArkivmeldingUtil(), () -> null),
            clientContext,
            config.typerKlientV2(authenticationStrategy, httpConfiguration),
            config.statusKlientV3(authenticationStrategy, httpConfiguration),
            config.eksternRefKlientV2(authenticationStrategy, httpConfiguration),
            config.fiksRestStatusMapper(new MessageStatusFactory(Clock.systemUTC())));
    }

    @Test
    void sendMessage_postsForsendelseAndDocumentsAsMultipart() throws Exception {
        UUID forsendelseId = UUID.randomUUID();
        wireMock.stubFor(post(sendPath())
            .willReturn(okJson("{\"id\":\"%s\"}".formatted(forsendelseId))));

        String result = client.sendMessage(MESSAGE_ID, message(), mock(X509Certificate.class), mock(Reject.class));

        assertThat(result).isEqualTo(forsendelseId.toString());
        wireMock.verify(1, postRequestedFor(urlEqualTo(sendPath()))
            .withHeader("Authorization", equalTo("Bearer maskinporten-token"))
            .withHeader("IntegrasjonId", equalTo("integrasjon-id"))
            .withHeader("IntegrasjonPassord", equalTo("integrasjon-passord")));

        LoggedRequest request = wireMock.getAllServeEvents().getFirst().getRequest();
        assertThat(request.isMultipart()).isTrue();
        // Hele JSON-dokumentet sammenlignes, slik at endrede feltnavn eller formater i klientbiblioteket oppdages
        assertThat(OBJECT_MAPPER.readTree(request.getPart("forsendelse").getBody().asString()))
            .isEqualTo(OBJECT_MAPPER.readTree(new ClassPathResource("svarut-rest/forsendelse.json").getInputStream()));

        var hoveddokument = request.getPart("filer_0");
        assertThat(hoveddokument.getHeader("Content-Disposition").firstValue()).contains("filename=\"hoveddokument.pdf\"");
        assertThat(hoveddokument.getHeader("Content-Type").firstValue()).isEqualTo("application/pdf");
        assertThat(hoveddokument.getBody().asString()).isEqualTo("PDF-innhold");

        var vedlegg = request.getPart("filer_1");
        assertThat(vedlegg.getHeader("Content-Disposition").firstValue()).contains("filename=\"vedlegg.txt\"");
        assertThat(vedlegg.getHeader("Content-Type").firstValue()).isEqualTo("text/plain");
        assertThat(vedlegg.getBody().asString()).isEqualTo("TXT-innhold");
    }

    @Test
    void sendMessage_rejectedBySvarUt_throwsExceptionWithErrorMessage() {
        wireMock.stubFor(post(sendPath())
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "timestamp": 1761638400000,
                      "status": 400,
                      "error": "Bad Request",
                      "errorId": "6f1c2d3e-4a5b-6c7d-8e9f-0a1b2c3d4e5f",
                      "path": "/api/v3/kontoer/%s/forsendelser/organisasjon",
                      "originalPath": "/api/v3/kontoer/%s/forsendelser/organisasjon",
                      "message": "Mottaker 910076787 finnes ikke",
                      "errorCode": "MOTTAKER_FINNES_IKKE",
                      "errorDetails": {"mottaker": "910076787"}
                    }
                    """.formatted(KONTO_ID, KONTO_ID))));

        assertThatExceptionOfType(SvarUtKlientException.class)
            .isThrownBy(() -> client.sendMessage(MESSAGE_ID, message(), mock(X509Certificate.class), mock(Reject.class)))
            .withMessage("Status 400: Mottaker 910076787 finnes ikke")
            .satisfies(e -> {
                assertThat(e.getErrorMessage().getErrorCode()).isEqualTo("MOTTAKER_FINNES_IKKE");
                assertThat(e.getErrorMessage().getErrorId()).isEqualTo(UUID.fromString("6f1c2d3e-4a5b-6c7d-8e9f-0a1b2c3d4e5f"));
                assertThat(e.getErrorMessage().getErrorDetails()).containsEntry("mottaker", "910076787");
            });
    }

    @Test
    void sendMessage_nonJsonErrorResponse_throwsExceptionWithResponseBody() {
        wireMock.stubFor(post(sendPath())
            .willReturn(aResponse()
                .withStatus(502)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><body>502 Bad Gateway</body></html>")));

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> client.sendMessage(MESSAGE_ID, message(), mock(X509Certificate.class), mock(Reject.class)))
            .withMessageContaining("502 Bad Gateway");
    }

    @Test
    void getForsendelseStatuser_mapsLestAndIgnoresOtherStatuses() {
        UUID lest = UUID.randomUUID();
        UUID mottatt = UUID.randomUUID();
        UUID avvist = UUID.randomUUID();
        wireMock.stubFor(post("/api/v3/kontoer/%s/forsendelser/status/sok".formatted(KONTO_ID))
            .willReturn(okJson("""
                {"statuser": [
                  {"id": "%s", "sisteStatusEndring": "2026-10-28T09:00:00+01:00", "status": "LEST"},
                  {"id": "%s", "sisteStatusEndring": "2026-10-28T09:00:00+01:00", "status": "MOTTATT"},
                  {"id": "%s", "sisteStatusEndring": "2026-10-28T09:00:00+01:00", "status": "AVVIST"}
                ]}
                """.formatted(lest, mottatt, avvist))));

        List<ForsendelseStatus> statuser = client.getForsendelseStatuser(SENDER, Set.of(lest.toString()));

        assertThat(statuser).hasSize(3);
        assertThat(statuser.get(0).id()).isEqualTo(lest.toString());
        assertThat(statuser.get(0).status().getStatus()).isEqualTo(ReceiptStatus.LEST.name());
        assertThat(statuser.get(1).status()).isNull();
        assertThat(statuser.get(2).status()).isNull();
        wireMock.verify(postRequestedFor(urlEqualTo("/api/v3/kontoer/%s/forsendelser/status/sok".formatted(KONTO_ID)))
            .withHeader("Authorization", equalTo("Bearer maskinporten-token"))
            .withRequestBody(equalToJson("{\"id\": [\"%s\"]}".formatted(lest))));
    }

    @Test
    void getForsendelseId_looksUpByEksternRef() {
        String forsendelseId = UUID.randomUUID().toString();
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/kontoer/%s/forsendelser".formatted(KONTO_ID)))
            .willReturn(okJson("{\"forsendelseIds\": [\"%s\"]}".formatted(forsendelseId))));
        Conversation conversation = new Conversation()
            .setMessageId(MESSAGE_ID)
            .setSenderIdentifier(SENDER);

        assertThat(client.getForsendelseId(conversation)).isEqualTo(forsendelseId);
        wireMock.verify(getRequestedFor(urlEqualTo("/api/v2/kontoer/%s/forsendelser?eksternRef=%s".formatted(KONTO_ID, MESSAGE_ID))));
    }

    @Test
    void retreiveForsendelseTyper() {
        wireMock.stubFor(get("/api/v2/forsendelsestyper")
            .willReturn(okJson("{\"forsendelsestyper\": [\"Byggesak\", \"Plansak\"]}")));

        assertThat(client.retreiveForsendelseTyper(SENDER)).containsExactlyInAnyOrder("Byggesak", "Plansak");
    }

    private static String sendPath() {
        return "/api/v3/kontoer/%s/forsendelser/organisasjon".formatted(KONTO_ID);
    }

    private static Resource resource(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
    }

    private static NextMoveOutMessage message() {
        ArkivmeldingMessage arkivmeldingMessage = new ArkivmeldingMessage();
        arkivmeldingMessage.setSikkerhetsnivaa(4);
        arkivmeldingMessage.setDpf(new DpfSettings().setForsendelseType("Byggesak"));
        StandardBusinessDocument sbd = new StandardBusinessDocument();
        sbd.setAny(arkivmeldingMessage);

        NextMoveOutMessage message = new NextMoveOutMessage(MESSAGE_ID, MESSAGE_ID, "process", RECEIVER, SENDER, ServiceIdentifier.DPF, sbd);
        message.setFiles(Set.of(
            file("arkivmelding.xml", "application/xml"),
            file("hoveddokument.pdf", "application/pdf"),
            file("vedlegg.txt", "text/plain")));
        return message;
    }

    private static BusinessMessageFile file(String filename, String mimetype) {
        return new BusinessMessageFile()
            .setIdentifier(filename)
            .setFilename(filename)
            .setMimetype(mimetype);
    }
}
