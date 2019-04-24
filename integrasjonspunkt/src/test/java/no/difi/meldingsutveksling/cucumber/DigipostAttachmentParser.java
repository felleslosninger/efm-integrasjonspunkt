package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
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
    Message parse(String payload, byte[] encryptedAsic) {

        StandardBusinessDocument sbd = xmlMarshaller.unmarshall(payload, StandardBusinessDocument.class);

        Element digitalPost = (Element) sbd.getAny();

        Element personidentifikator = XMLUtil.getElementByTagPath(digitalPost, "mottaker", "person", "personidentifikator");
        String receiverOrgNumber = personidentifikator.getTextContent();
        PrivateKey privateKey = cucumberKeyStore.getPrivateKey(receiverOrgNumber);

        Message message = new Message()
                .setBody(payload);

        byte[] asic = cmsUtil.decryptCMS(encryptedAsic, privateKey);
        List<Attachment> attachments = getAttachments(asic);

        message.attachments(attachments);
        return message;
    }

    private List<Attachment> getAttachments(byte[] asic) {
        return asicParser.parse(new ByteArrayInputStream(asic));
    }
}
