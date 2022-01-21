package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayload;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.List;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class ServiceBusMessageParser {

    private final ObjectMapper objectMapper;
    private final AsicParser asicParser;
    private final CmsUtil cmsUtil;
    private final CucumberKeyStore cucumberKeyStore;
    private final SBDService sbdService;

    @SneakyThrows
    public Message parse(byte[] in) {
        ServiceBusPayload serviceBusPayload = objectMapper.readValue(in, ServiceBusPayload.class);
        Message message = new Message()
                .setServiceIdentifier(ServiceIdentifier.DPE)
                .setSbd(serviceBusPayload.getSbd());

        if (serviceBusPayload.getAsic() != null) {
            String receiverOrgNumber = sbdService.getReceiverIdentifier(serviceBusPayload.getSbd());
            PrivateKey privateKey = cucumberKeyStore.getPrivateKey(receiverOrgNumber);

            byte[] encryptedAsic = Base64.getDecoder().decode(serviceBusPayload.getAsic());
            byte[] asic = cmsUtil.decryptCMS(encryptedAsic, privateKey);

            List<Attachment> attachments = asicParser.parse(new ByteArrayInputStream(asic));
            message.setAttachments(attachments);
        }

        return message;
    }
}
