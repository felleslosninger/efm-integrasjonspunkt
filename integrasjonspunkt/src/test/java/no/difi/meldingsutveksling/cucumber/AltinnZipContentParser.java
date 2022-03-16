package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.NextMoveConsts.SBD_FILE;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class AltinnZipContentParser {

    private final ObjectMapper objectMapper;
    private final AsicParser asicParser;
    private final DecryptCMSDocument decryptCMSDocument;
    private final KeystoreHelper cucumberKeystoreHelper;

    @SneakyThrows
    Message parse(ZipContent zipContent) {
        StandardBusinessDocument sbd = getSbd(zipContent);

        PartnerIdentifier receiver = sbd.getReceiverIdentifier();

        Message message = new Message()
                .setServiceIdentifier(ServiceIdentifier.DPO)
                .setSbd(sbd);

        zipContent.getOptionalFile(ASIC_FILE).ifPresent(asicFile -> {
            Resource asic = decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                    .keystoreHelper(cucumberKeystoreHelper)
                    .resource(asicFile.getResource())
                    .alias(receiver.getPrimaryIdentifier())
                    .build());

            message.attachments(getAttachments(asic));
        });

        return message;
    }

    private StandardBusinessDocument getSbd(ZipContent zipContent) throws IOException {
        try (InputStream inputStream = zipContent.getFile(SBD_FILE).getResource().getInputStream()) {
            return objectMapper.readValue(inputStream, StandardBusinessDocument.class);
        }
    }

    private List<Document> getAttachments(Resource asic) {
        return asicParser.parse(asic);
    }
}
