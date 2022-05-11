package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.security.PrivateKey;
import java.util.List;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class DigipostAttachmentParser {

    private final XMLMarshaller xmlMarshaller;
    private final AsicParser asicParser;
    private final CmsUtil cmsUtil;
    private final CucumberKeyStore cucumberKeyStore;

    @SneakyThrows
    Message parse(String payload, InputStream encryptedAsic) {

        StandardBusinessDocument sbd = xmlMarshaller.unmarshall(payload, StandardBusinessDocument.class);

        Element digitalPost = (Element) sbd.getAny();

        Element personidentifikator = XMLUtil.getElementByTagPath(digitalPost, "mottaker", "person", "personidentifikator");
        String receiverOrgNumber = personidentifikator.getTextContent();
        PrivateKey privateKey = cucumberKeyStore.getPrivateKey(receiverOrgNumber);

        return new Message()
                .setBody(payload)
                .setServiceIdentifier(ServiceIdentifier.DPI)
                .attachments(getAttachments(cmsUtil.decryptCMSStreamed(encryptedAsic, privateKey)));
    }

    private List<Attachment> getAttachments(InputStream asic) {
        return asicParser.parse(asic);
    }
}
