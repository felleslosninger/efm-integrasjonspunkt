package no.difi.meldingsutveksling.cucumber;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.nimbusds.jose.Payload;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DokumentpakkefingeravtrykkHolder;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackJWT;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackStandardBusinessDocument;
import org.apache.commons.fileupload.FileItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Component
@Profile("cucumber")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.dpi.client.type", havingValue = "json")
public class DpiClientRequestParser {

    private final MultipartParser multipartParser;
    private final AsicParser asicParser;
    private final CmsUtil cmsUtil;
    private final CucumberKeyStore cucumberKeyStore;
    private final UnpackJWT unpackJWT;
    private final UnpackStandardBusinessDocument unpackStandardBusinessDocument;

    @SneakyThrows
    Message parse(LoggedRequest loggedRequest) {
        String contentType = loggedRequest.getHeader(HttpHeaders.CONTENT_TYPE);
        Map<String, FileItem> fileItems = multipartParser.parse(contentType, loggedRequest.getBody());

        StandardBusinessDocument sbd = getStandardBusinessDocument(fileItems);

        PartnerIdentifier receiver = SBDUtil.getReceiver(sbd);
        PrivateKey privateKey = cucumberKeyStore.getPrivateKey(receiver.getOrganizationIdentifier());

        List<Attachment> attachments = getAttachments(fileItems, privateKey);

        sbd.getBusinessMessage(DokumentpakkefingeravtrykkHolder.class)
                .ifPresent(p -> p.getDokumentpakkefingeravtrykk().setDigestValue("dummy"));

        return new Message()
                .setServiceIdentifier(ServiceIdentifier.DPI)
                .setSbd(sbd)
                .attachments(attachments);
    }

    private List<Attachment> getAttachments(Map<String, FileItem> fileItems, PrivateKey privateKey) throws IOException {
        FileItem cmsFileItem = fileItems.get("dokumentpakke");
        assertThat(cmsFileItem.getContentType()).isEqualTo("application/cms");
        assertThat(cmsFileItem.getName()).isEqualTo("asic.cms");
        return getAttachments(cmsUtil.decryptCMSStreamed(cmsFileItem.getInputStream(), privateKey));
    }

    private StandardBusinessDocument getStandardBusinessDocument(Map<String, FileItem> fileItems) {
        FileItem jwtFileItem = fileItems.get("forretningsmelding");
        assertThat(jwtFileItem.getContentType()).isEqualTo("application/jwt");
        assertThat(jwtFileItem.getName()).isEqualTo("sbd.jwt");
        String jwt = new String(jwtFileItem.get(), StandardCharsets.UTF_8);
        Payload payload = unpackJWT.getPayload(jwt);
        return unpackStandardBusinessDocument.unpackStandardBusinessDocument(payload);
    }

    private List<Attachment> getAttachments(InputStream asic) {
        return asicParser.parse(asic);
    }
}
