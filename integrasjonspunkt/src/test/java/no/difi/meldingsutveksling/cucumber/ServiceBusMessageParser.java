package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.AsicParser;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayload;
import no.difi.move.common.cert.KeystoreHelper;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class ServiceBusMessageParser {

    private final ObjectMapper objectMapper;
    private final AsicParser asicParser;
    private final DecryptCMSDocument decryptCMSDocument;
    private final KeystoreHelper keystoreHelper;

    @SneakyThrows
    public Message parse(byte[] in) {
        ServiceBusPayload serviceBusPayload = objectMapper.readValue(in, ServiceBusPayload.class);
        Message message = new Message()
                .setServiceIdentifier(ServiceIdentifier.DPE)
                .setSbd(serviceBusPayload.getSbd());

        if (serviceBusPayload.getAsic() != null) {
            PartnerIdentifier receiver = serviceBusPayload.getSbd().getReceiverIdentifier();
            Resource encryptedAsic = new ByteArrayResource(Base64.getDecoder().decode(serviceBusPayload.getAsic()));
            Resource asic = decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                    .resource(encryptedAsic)
                    .keystoreHelper(keystoreHelper)
                    .alias(receiver.getOrganizationIdentifier())
                    .build());

            List<Document> attachments = asicParser.parse(asic);
            message.setAttachments(attachments);
        }

        return message;
    }
}
