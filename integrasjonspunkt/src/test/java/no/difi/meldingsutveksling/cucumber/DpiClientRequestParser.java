package no.difi.meldingsutveksling.cucumber;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.nimbusds.jose.Payload;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.AsicParser;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DokumentpakkefingeravtrykkHolder;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackJWT;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackStandardBusinessDocument;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Component
@Profile("cucumber")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.dpi.client-type", havingValue = "json")
public class DpiClientRequestParser {

    private final MultipartParser multipartParser;
    private final AsicParser asicParser;
    private final DecryptCMSDocument decryptCMSDocument;
    private final KeystoreHelper keystoreHelper;
    private final UnpackJWT unpackJWT;
    private final UnpackStandardBusinessDocument unpackStandardBusinessDocument;

    @SneakyThrows
    Message parse(LoggedRequest loggedRequest) {
        String contentType = loggedRequest.getHeader(HttpHeaders.CONTENT_TYPE);
        Map<String, FileItemResource> fileItems = multipartParser.parse(contentType, loggedRequest.getBody());

        StandardBusinessDocument sbd = getStandardBusinessDocument(fileItems);
        PartnerIdentifier receiver = sbd.getReceiverIdentifier();
        List<Document> attachments = getAttachments(fileItems, receiver);

        sbd.getBusinessMessage(DokumentpakkefingeravtrykkHolder.class)
                .ifPresent(p -> p.getDokumentpakkefingeravtrykk().setDigestValue("dummy"));

        return new Message()
                .setServiceIdentifier(ServiceIdentifier.DPI)
                .setSbd(sbd)
                .attachments(attachments);
    }

    private List<Document> getAttachments(Map<String, FileItemResource> fileItems, PartnerIdentifier receiver) {
        FileItemResource cmsFileItem = fileItems.get("dokumentpakke");
        assertThat(cmsFileItem.getContentType()).isEqualTo("application/cms");
        assertThat(cmsFileItem.getName()).isEqualTo("asic.cms");
        return getAttachments(decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                .resource(cmsFileItem)
                .keystoreHelper(keystoreHelper)
                .alias(receiver.getOrganizationIdentifier())
                .build()));
    }

    private StandardBusinessDocument getStandardBusinessDocument(Map<String, FileItemResource> fileItems) {
        FileItemResource jwtFileItem = fileItems.get("forretningsmelding");
        assertThat(jwtFileItem.getContentType()).isEqualTo("application/jwt");
        assertThat(jwtFileItem.getName()).isEqualTo("sbd.jwt");
        String jwt = new String(jwtFileItem.getByteArray(), StandardCharsets.UTF_8);
        Payload payload = unpackJWT.getPayload(jwt);
        return unpackStandardBusinessDocument.unpackStandardBusinessDocument(payload);
    }

    private List<Document> getAttachments(Resource asic) {
        return asicParser.parse(asic);
    }
}
