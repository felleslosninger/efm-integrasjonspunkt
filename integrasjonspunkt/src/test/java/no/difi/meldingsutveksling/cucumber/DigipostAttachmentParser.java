package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.AsicParser;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.util.List;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class DigipostAttachmentParser {

    private final XMLMarshaller xmlMarshaller;
    private final AsicParser asicParser;
    private final DecryptCMSDocument decryptCMSDocument;
    private final KeystoreHelper keystoreHelper;

    @SneakyThrows
    Message parse(String payload, Resource encryptedAsic) {
        String receiverOrgNumber = getReceiverOrgNumber(payload);
        Resource asic = decrypt(encryptedAsic, receiverOrgNumber);

        return new Message()
                .setBody(payload)
                .setServiceIdentifier(ServiceIdentifier.DPI)
                .attachments(getAttachments(asic));
    }

    private String getReceiverOrgNumber(String payload) {
        StandardBusinessDocument sbd = xmlMarshaller.unmarshall(payload, StandardBusinessDocument.class);
        Element digitalPost = (Element) sbd.getAny();
        Element personidentifikator = XMLUtil.getElementByTagPath(digitalPost, "mottaker", "person", "personidentifikator");
        return personidentifikator.getTextContent();
    }

    private Resource decrypt(Resource encryptedAsic, String receiverOrgNumber) {
        return decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                .resource(encryptedAsic)
                .keystoreHelper(keystoreHelper)
                .alias(receiverOrgNumber)
                .build());
    }

    private List<Document> getAttachments(Resource asic) {
        return asicParser.parse(asic);
    }
}
