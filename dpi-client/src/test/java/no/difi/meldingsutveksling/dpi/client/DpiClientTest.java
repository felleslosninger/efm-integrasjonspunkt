package no.difi.meldingsutveksling.dpi.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Payload;
import lombok.SneakyThrows;
import net.javacrumbs.jsonunit.core.Option;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.Authority;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.*;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.BusinessMessage;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Digital;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Utskrift;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.*;
import no.difi.meldingsutveksling.dpi.client.internal.DpiMapper;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackJWT;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackStandardBusinessDocument;
import no.difi.move.common.io.ResourceUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.mockserver.model.RequestDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.mail.util.SharedByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.ConfigurationWhen.paths;
import static net.javacrumbs.jsonunit.core.ConfigurationWhen.then;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest(classes = {DpiClientTestConfig.class, DpiClientConfig.class})
@ExtendWith({SpringExtension.class, MockServerExtension.class})
@MockServerSettings(ports = 8900)
class DpiClientTest {

    @Autowired
    private DpiClient dpiClient;

    @Autowired
    private ShipmentFactory shipmentFactory;

    @Autowired
    private DecryptCMSDocument decryptCMSDocument;

    @Autowired
    private ParcelParser parcelParser;

    @Autowired
    private UnpackJWT unpackJWT;

    @Autowired
    private CreateReceiptJWT createReceiptJWT;

    @Autowired
    private DpiMapper dpiMapper;

    @Autowired
    private CreateLeveringskvittering createLeveringskvittering;

    @Autowired
    private UnpackStandardBusinessDocument unpackStandardBusinessDocument;

    @MockBean
    private UUIDGenerator uuidGenerator;

    @Value("classpath:/digital_ready_for_send-sbd.json")
    private Resource digitalReadyForSendSbd;

    @Value("classpath:/utskrift_ready_for_send-sbd.json")
    private Resource utskriftReadyForSendSbd;

    @Value("classpath:/svada.pdf")
    private Resource hoveddokument;

    @Value("classpath:/bilde.png")
    private Resource vedlegg;

    @Value("classpath:/c2.cer")
    private Resource sertifikat;

    @Value("classpath:/message_statuses.json")
    private Resource messageStatusesResource;

    @BeforeEach
    public void beforeEach(MockServerClient client) {
        client.when(request()
                        .withMethod("POST")
                        .withPath("/token"))
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{ \"access_token\" : \"DummyMaskinportenToken\" }")
                );
    }

    @AfterEach
    public void afterEach(MockServerClient client) {
        client.reset();
    }

    @Test
    void testSendDigital(MockServerClient client) {
        testSend(client, getDpiDigitalTestInput(), digitalReadyForSendSbd);
    }

    private DpiTestInput getDpiDigitalTestInput() {
        return createDpiTestInput(new Digital()
                .setAvsender(new Avsender()
                        .setVirksomhetsidentifikator(
                                new Identifikator()
                                        .setAuthority(Authority.ISO6523_ACTORID_UPIS)
                                        .setValue("0192:999888999")
                        ))
                .setMottaker(new Personmottaker()
                        .setPostkasseadresse("ola.nordmann#9YDT")
                )
                .setSikkerhetsnivaa(3)
                .setVirkningsdato(LocalDate.parse("2021-01-01"))
                .setVirkningstidspunkt(OffsetDateTime.parse("2021-01-01T08:00:00.000+01:00"))
                .setAapningskvittering(false)
                .setIkkesensitivtittel("ikkeSensitivTittel")
                .setSpraak("NO")
                .setVarsler(new Varsler()
                        .setEpostvarsel(new Epostvarsel()
                                .setEpostadresse("test@epost.no")
                                .setVarslingstekst("Dette er en varslingstekst")
                                .setRepetisjoner(Arrays.asList(1, 7)))
                        .setSmsvarsel(new Smsvarsel()
                                .setMobiltelefonnummer("12345678")
                                .setVarslingstekst("Dette er en varslingstekst")
                                .setRepetisjoner(Arrays.asList(1, 7)))
                )
        );
    }

    @Test
    void testSendFysisk(MockServerClient client) {
        testSend(client, createDpiTestInput(new Utskrift()
                .setAvsender(new Avsender()
                        .setVirksomhetsidentifikator(new Identifikator()
                                .setAuthority(Authority.ISO6523_ACTORID_UPIS)
                                .setValue("0192:999888999")))
                .setMottaker(new AdresseInformasjon()
                        .setNavn("navn")
                        .setAdresselinje1("adresselinje1")
                        .setAdresselinje2("adresselinje2")
                        .setAdresselinje3("adresselinje3")
                        .setAdresselinje4("adresselinje4")
                        .setLand("land"))
                .setUtskriftstype(Utskrift.Utskriftstype.SORT_HVIT)
                .setPosttype(Utskrift.Posttype.B)
                .setRetur(new Retur()
                        .setMottaker(new AdresseInformasjon()
                                .setNavn("navn")
                                .setAdresselinje1("adresselinje1")
                                .setAdresselinje2("adresselinje2")
                                .setAdresselinje3("adresselinje3")
                                .setPostnummer("1234")
                                .setPoststed("poststed"))
                        .setReturposthaandtering(Retur.Returposthaandtering.DIREKTE_RETUR))
        ), utskriftReadyForSendSbd);
    }

    private DpiTestInput createDpiTestInput(BusinessMessage businessMessage) {
        return new DpiTestInput()
                .setSender(Iso6523.parse("0192:987654321"))
                .setReceiver(Iso6523.parse("0192:123456789"))
                .setMessageId("ff88849c-e281-4809-8555-7cd54952b916")
                .setConversationId("37efbd4c-413d-4e2c-bbc5-257ef4a65a45")
                .setExpectedResponseDateTime(OffsetDateTime.parse("2021-04-21T15:29:58.753+02:00"))
                .setBusinessMessage(businessMessage)
                .setMainDocument(hoveddokument)
                .setAttachments(Collections.singletonList(vedlegg))
                .setMailbox("dummy")
                .setReceiverCertificate(sertifikat);
    }

    @Test
    void testSendWhenResponseError(MockServerClient client) {
        HttpResponse httpResponse = response()
                .withBody("{}")
                .withStatusCode(400);

        DpiTestInput input = getDpiDigitalTestInput();

        assertThatThrownBy(() -> send(client, input, httpResponse))
                .isInstanceOf(DpiException.class)
                .hasMessage(String.format("400 Bad Request from POST http://localhost:8900/dpi/messages/out:%n{}"))
                .hasCauseInstanceOf(WebClientResponseException.BadRequest.class);
    }

    @Test
    void testGetMessageStatuses(MockServerClient client) {
        UUID uuid = UUID.randomUUID();

        client.when(request()
                        .withMethod("GET")
                        .withPath(String.format("/dpi/messages/out/%s/statuses", uuid)))
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(ResourceUtils.toByteArray(messageStatusesResource))
                );

        StepVerifier.create(dpiClient.getMessageStatuses(uuid))
                .recordWith(ArrayList::new)
                .thenConsumeWhile(x -> true)
                .consumeRecordedWith(elements -> assertThat(elements)
                        .containsExactly(
                                new MessageStatus()
                                        .setStatus(ReceiptStatus.OPPRETTET)
                                        .setTimestamp(OffsetDateTime.parse("2021-06-29T05:49:47Z")),
                                new MessageStatus()
                                        .setStatus(ReceiptStatus.SENDT)
                                        .setTimestamp(OffsetDateTime.parse("2021-06-29T07:12:40Z"))
                        ))
                .verifyComplete();

        client.verify(request()
                .withMethod("GET")
                .withPath(String.format("/dpi/messages/out/%s/statuses", uuid)));
    }

    @Test
    @SneakyThrows
    void testGetMessages(MockServerClient client) {
        given(uuidGenerator.generate())
                .willReturn("ff88849c-e281-4809-8555-7cd54952b916");

        String avsenderidentifikator = "123";
        String forretningsmelding = createReceiptJWT.createReceiptJWT(dpiMapper.readStandardBusinessDocument(digitalReadyForSendSbd), createLeveringskvittering);
        client.when(request()
                        .withMethod("GET")
                        .withPath("/dpi/messages/in")
                        .withQueryStringParameter("avsenderidentifikator", avsenderidentifikator)
                )
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(new ObjectMapper().writeValueAsString(new Message()
                                .setForretningsmelding(forretningsmelding)
                                .setDownloadurl(URI.create("http://localhost:8900/dpi/downloadmessage/a9bc8498-13b1-4cef-9cf9-4873a03b484d"))
                        ))
                );

        StepVerifier.create(dpiClient.getMessages(new GetMessagesInput()
                        .setSenderId(avsenderidentifikator)
                ))
                .recordWith(ArrayList::new)
                .thenConsumeWhile(x -> true)
                .consumeRecordedWith(elements -> assertThat(elements).hasSize(1)
                        .first()
                        .satisfies(receivedMessage -> {
                            assertThat(receivedMessage.getMessage().getForretningsmelding()).isEqualTo(forretningsmelding);
                            assertThat(receivedMessage.getMessage().getDownloadurl()).isEqualTo(URI.create("http://localhost:8900/dpi/downloadmessage/a9bc8498-13b1-4cef-9cf9-4873a03b484d"));
                        })
                )
                .verifyComplete();

        client.verify(request()
                .withMethod("GET")
                .withPath("/dpi/messages/in")
                .withQueryStringParameter("avsenderidentifikator", avsenderidentifikator));
    }

    @Test
    void testGetCmsEncryptedAsice(MockServerClient client) {
        UUID uuid = UUID.randomUUID();
        String path = String.format("/dpi/downloadmessage/%s", uuid);
        byte[] bytes = new byte[1024 * 100];
        ThreadLocalRandom.current().nextBytes(bytes);

        client.when(request()
                        .withMethod("GET")
                        .withPath(path))
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(bytes)
                );

        CmsEncryptedAsice cmsEncryptedAsice = dpiClient.getCmsEncryptedAsice(URI.create("http://localhost:8900" + path));

        assertThat(cmsEncryptedAsice.getResource().contentLength()).isEqualTo(1024 * 100);
        assertThat(ResourceUtils.toByteArray(cmsEncryptedAsice.getResource())).isEqualTo(bytes);

        client.verify(request()
                .withMethod("GET")
                .withPath(path));
    }

    @Test
    void testMarkAsRead(MockServerClient client) {
        UUID uuid = UUID.randomUUID();
        String path = String.format("/dpi/messages/in/%s/read", uuid);

        client.when(request()
                        .withMethod("POST")
                        .withPath(path))
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                );

        dpiClient.markAsRead(uuid);

        client.verify(request()
                .withMethod("POST")
                .withPath(path));
    }

    @SneakyThrows
    private void testSend(MockServerClient client, DpiTestInput input, Resource out) {
        MimeMultipart mimeMultipart = send(client, input, response()
                .withStatusCode(200));
        assertThat(mimeMultipart.getCount()).isEqualTo(2);
        StandardBusinessDocument standardBusinessDocument = assertThatStandardBusinessDocumentIsCorrect(mimeMultipart.getBodyPart(0), out);
        assertThatParcelIsCorrect(standardBusinessDocument, mimeMultipart.getBodyPart(1));
    }

    @SneakyThrows
    private StandardBusinessDocument assertThatStandardBusinessDocumentIsCorrect(BodyPart sbdPart, Resource expectedSBD) {
        assertThat(sbdPart.getContentType()).isEqualTo("application/jwt");
        assertThat(sbdPart.getFileName()).isEqualTo("sbd.jwt");

        SharedByteArrayInputStream content = (SharedByteArrayInputStream) sbdPart.getContent();
        String jwt = IOUtils.toString(content, StandardCharsets.UTF_8);
        Payload payload = unpackJWT.getPayload(jwt);
        StandardBusinessDocument standardBusinessDocument = unpackStandardBusinessDocument.unpackStandardBusinessDocument(payload);

        String expected = IOUtils.toString(expectedSBD.getInputStream(), StandardCharsets.UTF_8);

        assertThatJson(payload.toString())
                .when(paths(String.format("standardBusinessDocument.%s.dokumentpakkefingeravtrykk.digestValue", standardBusinessDocument.getType())), then(Option.IGNORING_VALUES))
                .isEqualTo(expected);

        return standardBusinessDocument;
    }

    private MimeMultipart send(MockServerClient client, DpiTestInput input, HttpResponse httpResponse) throws MessagingException {
        HttpRequest requestDefinition = request()
                .withMethod("POST")
                .withPath("/dpi/messages/out");

        client.when(requestDefinition)
                .respond(httpResponse);

        Shipment shipment = shipmentFactory.getShipment(input);

        dpiClient.sendMessage(shipment);

        client.verify(request()
                .withMethod("POST")
                .withPath("/token"));

        client.verify(requestDefinition);

        HttpRequest httpRequest = getRecordedRequest(client, requestDefinition);
        return getMimeMultipart(httpRequest);
    }

    @SneakyThrows
    private void assertThatParcelIsCorrect(StandardBusinessDocument standardBusinessDocument, BodyPart cmsPart) {
        assertThat(cmsPart.getContentType()).isEqualTo("application/cms");
        assertThat(cmsPart.getFileName()).isEqualTo("asic.cms");

        SharedByteArrayInputStream content = (SharedByteArrayInputStream) cmsPart.getContent();
        InputStream asicInputStream = decryptCMSDocument.decrypt(content);
        Parcel parcel = parcelParser.parse(
                standardBusinessDocument.getStandardBusinessDocumentHeader().getDocumentIdentification().getInstanceIdentifier(),
                asicInputStream);

        assertThat(parcel.getMainDocument().getFilename()).isEqualTo("svada.pdf");
        assertThat(parcel.getMainDocument().getMimeType()).isEqualTo("application/pdf");
        assertThat(ResourceUtils.toByteArray(parcel.getMainDocument().getResource()))
                .isEqualTo(ResourceUtils.toByteArray(hoveddokument));
        assertThat(parcel.getAttachments()).hasSize(1);
        assertThat(parcel.getAttachments().get(0).getFilename()).isEqualTo("bilde.png");
        assertThat(parcel.getAttachments().get(0).getMimeType()).isEqualTo("image/png");
        assertThat(ResourceUtils.toByteArray(parcel.getAttachments().get(0).getResource()))
                .isEqualTo(ResourceUtils.toByteArray(vedlegg));
    }

    private MimeMultipart getMimeMultipart(HttpRequest httpRequest) throws MessagingException {
        String contentTypeValue = httpRequest.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        ContentType contentType = ContentType.parse(contentTypeValue);
        return new MimeMultipart(new ByteArrayDataSource(httpRequest.getBodyAsRawBytes(), contentType.getMimeType()));
    }

    private HttpRequest getRecordedRequest(MockServerClient client, HttpRequest requestDefinition) {
        RequestDefinition[] recordedRequests = client.retrieveRecordedRequests(requestDefinition);
        assertThat(recordedRequests).hasSize(1);
        RequestDefinition recordedRequest = recordedRequests[0];
        assertThat(recordedRequest).isInstanceOf(HttpRequest.class);
        return (HttpRequest) recordedRequest;
    }
}