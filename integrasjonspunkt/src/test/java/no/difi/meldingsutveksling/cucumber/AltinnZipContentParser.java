package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.util.List;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.NextMoveConsts.SBD_FILE;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class AltinnZipContentParser {

    private final ObjectMapper objectMapper;
    private final AsicParser asicParser;
    private final CmsUtil cmsUtil;
    private final CucumberKeyStore cucumberKeyStore;

    @SneakyThrows
    Message parse(ZipContent zipContent) {
        StandardBusinessDocument sbd = getSbd(zipContent);

        PartnerIdentifier receiver = sbd.getReceiverIdentifier();
        PrivateKey privateKey = cucumberKeyStore.getPrivateKey(receiver.getPrimaryIdentifier());

        Message message = new Message()
                .setServiceIdentifier(ServiceIdentifier.DPO)
                .setSbd(sbd);

        zipContent.getOptionalFile(ASIC_FILE).ifPresent(asicFile -> {
            InputStream asicInputStream = cmsUtil.decryptCMSStreamed(asicFile.getInputStream(), privateKey);
            message.attachments(getAttachments(asicInputStream));
        });

        return message;
    }

    private StandardBusinessDocument getSbd(ZipContent zipContent) throws java.io.IOException {
        return objectMapper.readValue(zipContent.getFile(SBD_FILE).getInputStream(), StandardBusinessDocument.class);
    }

    private List<Attachment> getAttachments(InputStream is) {
        return asicParser.parse(new BufferedInputStream(is));
    }
}
