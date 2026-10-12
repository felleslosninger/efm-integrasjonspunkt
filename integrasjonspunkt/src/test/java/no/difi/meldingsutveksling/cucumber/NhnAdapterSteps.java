package no.difi.meldingsutveksling.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dph.client.internal.DphParcelService;
import no.difi.meldingsutveksling.nhn.adapter.model.ContentTypes;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.MultipartFileNames;
import no.difi.meldingsutveksling.nhn.adapter.model.MultipartNames;
import no.difi.meldingsutveksling.nhn.adapter.model.serialization.KxJson;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.dokumentpakking.AsicParser;
import no.difi.move.common.dokumentpakking.DecryptCMSDocument;
import no.difi.move.common.io.ResourceUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static kotlinx.serialization.builtins.BuiltinSerializersKt.ListSerializer;

/**
 * Plays the role of the NHN adapter. The DPH features mock the DigDir certificate (virksert/991825827)
 * to be our own, so we can decrypt what the integrasjonspunkt sends and encrypt what it receives.
 */
@RequiredArgsConstructor
public class NhnAdapterSteps {

    private static final String MESSAGES_OUT = "/nhn-adapter/api/messages/out";
    private static final String MESSAGES_IN = "/nhn-adapter/api/messages/in";
    private static final String NHN_MESSAGE_ID = "4a8e3c1f-6b2d-4e9a-8c7b-1d2e3f4a5b6c";
    private static final String BOUNDARY = "nhn-adapter-boundary";

    private final WireMockServer wireMockServer;
    private final DphParcelService dphParcelService;
    private final DecryptCMSDocument decryptCMSDocument;
    private final KeystoreHelper keystoreHelper;
    private final AsicParser asicParser;
    private final Holder<Message> messageInHolder;
    private final Holder<Message> messageSentHolder;

    @Before
    public void before() {
        wireMockServer.givenThat(post(urlEqualTo(MESSAGES_OUT))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .withBody(NHN_MESSAGE_ID)
            )
        );

        wireMockServer.givenThat(post(urlPathMatching(MESSAGES_IN + "/[^/]+/read"))
            .willReturn(aResponse()
                .withStatus(200)
            )
        );
    }

    @And("^the NHN adapter has the message available with id \"([^\"]*)\"$")
    public void theNhnAdapterHasTheMessageAvailableWithId(String nhnMessageId) {
        Message message = messageInHolder.get();
        StandardBusinessDocument sbd = message.getSbd();
        int receiverHerId = sbd.getReceiverIdentifier().cast(NhnIdentifier.class).getHerId();

        IncomingMessage incomingMessage = new IncomingMessage(
            nhnMessageId,
            receiverHerId,
            sbd.getSenderIdentifier().cast(NhnIdentifier.class).getHerId(),
            sbd.getMessageId(),
            sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getCreationDateAndTime(),
            false);

        wireMockServer.givenThat(get(urlPathEqualTo(MESSAGES_IN))
            .withQueryParam("receiverHerId", equalTo(Integer.toString(receiverHerId)))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(KxJson.encode(List.of(incomingMessage), ListSerializer(IncomingMessage.Companion.serializer())))
            )
        );

        wireMockServer.givenThat(post(urlEqualTo(MESSAGES_IN))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, "multipart/mixed; boundary=" + BOUNDARY)
                .withBody(createMultipart(message))
            )
        );
    }

    @Then("^a DPH message is sent to the NHN adapter$")
    public void aDphMessageIsSentToTheNhnAdapter() {
        RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlEqualTo(MESSAGES_OUT))
            .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.MULTIPART_FORM_DATA_VALUE))
            .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer DummyMaskinportenToken"));

        wireMockServer.verify(1, requestPatternBuilder);

        LoggedRequest loggedRequest = wireMockServer.findAll(requestPatternBuilder).getFirst();
        messageSentHolder.set(parse(loggedRequest));
    }

    @Then("^the NHN adapter message with id \"([^\"]*)\" is marked as read$")
    public void theNhnAdapterMessageWithIdIsMarkedAsRead(String nhnMessageId) {
        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(MESSAGES_IN + "/" + nhnMessageId + "/read")));
    }

    private Message parse(LoggedRequest loggedRequest) {
        String forretningsmelding = loggedRequest.getPart(MultipartNames.FORRETNINGSMELDING).getBody().asString();
        String json = dphParcelService.decryptAndVerify(forretningsmelding).getPayload().toString();

        Message message = new Message()
            .setServiceIdentifier(ServiceIdentifier.DPH)
            .setSbd(dphParcelService.toSBD(json));

        Request.Part dokumentpakke = loggedRequest.getPart(MultipartNames.DOKUMENTPAKKE);
        if (dokumentpakke != null) {
            Resource asic = decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                .resource(new ByteArrayResource(dokumentpakke.getBody().asBytes()))
                .keystoreHelper(keystoreHelper)
                .build());
            message.setAttachments(asicParser.parse(asic));
        }

        return message;
    }

    private byte[] createMultipart(Message message) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        String forretningsmelding = dphParcelService.signAndEncrypt(dphParcelService.toJSON(message.getSbd()));
        writePart(out, MultipartNames.FORRETNINGSMELDING, MultipartFileNames.FORRETNINGSMELDING,
            ContentTypes.APPLICATION_JOSE, forretningsmelding.getBytes(UTF_8));

        if (!message.getAttachments().isEmpty()) {
            Resource encryptedAsic = dphParcelService.createAndEncryptAsic(message.getAttachments().stream());
            writePart(out, MultipartNames.DOKUMENTPAKKE, MultipartFileNames.DOKUMENTPAKKE,
                ContentTypes.APPLICATION_ASICE, ResourceUtils.toByteArray(encryptedAsic));
        }

        out.writeBytes(("--" + BOUNDARY + "--\r\n").getBytes(UTF_8));
        return out.toByteArray();
    }

    private static void writePart(ByteArrayOutputStream out, String name, String filename, String contentType, byte[] content) {
        out.writeBytes(("--" + BOUNDARY + "\r\n"
            + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n"
            + "Content-Type: " + contentType + "\r\n"
            + "\r\n").getBytes(UTF_8));
        out.writeBytes(content);
        out.writeBytes("\r\n".getBytes(UTF_8));
    }
}
