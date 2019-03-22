package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.util.List;

import static no.difi.meldingsutveksling.NextMoveConsts.ALTINN_SBD_FILE;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;

@Component
@RequiredArgsConstructor
public class AltinnZipContentParser {

    private final ObjectMapper objectMapper;
    private final AsicParser asicParser;
    private final CmsUtil cmsUtil;
    private final CucumberKeyStore cucumberKeyStore;

    @SneakyThrows
    Message parse(ZipContent zipContent) {
        StandardBusinessDocument sbd = getSbd(zipContent);

        String receiverOrgNumber = sbd.getReceiverOrgNumber();
        PrivateKey privateKey = cucumberKeyStore.getPrivateKey(receiverOrgNumber);

        byte[] encryptedAsic = zipContent.getFile(ASIC_FILE).getBytes();
        byte[] asic = cmsUtil.decryptCMS(encryptedAsic, privateKey);

        return new Message()
                .setSbd(sbd)
                .attachments(getAttachments(asic));
    }

    private StandardBusinessDocument getSbd(ZipContent zipContent) throws java.io.IOException {
        return objectMapper.readValue(zipContent.getFile(ALTINN_SBD_FILE).getBytes(), StandardBusinessDocument.class);
    }

    private List<Attachment> getAttachments(byte[] asic) {
        return asicParser.parse(new ByteArrayInputStream(asic));
    }
}
