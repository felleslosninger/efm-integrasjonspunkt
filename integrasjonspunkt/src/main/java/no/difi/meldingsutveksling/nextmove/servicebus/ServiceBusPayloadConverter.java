package no.difi.meldingsutveksling.nextmove.servicebus;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.DpeMessage;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class ServiceBusPayloadConverter {

    private ObjectMapper objectMapper;
    private JAXBContext jaxbContext;

    public ServiceBusPayloadConverter(ObjectMapper objectMapper) throws JAXBException {
        this.objectMapper = objectMapper;
        this.jaxbContext = JAXBContextFactory.createContext(new Class[]{StandardBusinessDocument.class,
                Payload.class, ConversationResource.class}, null);
    }

    public ServiceBusPayload convert(String input, String messageId) throws JAXBException {
        return convert(input.getBytes(StandardCharsets.UTF_8), messageId);
    }

    public ServiceBusPayload convert(byte[] input, String messageId) throws JAXBException {
        try {
            return objectMapper.readValue(input, ServiceBusPayload.class);
        } catch (IOException e) {
            log.warn(String.format("Error creating ServiceBusPayload from message id=%s. Trying to create as old format..", messageId), e);
        }

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ByteArrayInputStream bis = new ByteArrayInputStream(input);
        StandardBusinessDocument sbd = unmarshaller.unmarshal(new StreamSource(bis), StandardBusinessDocument.class).getValue();
        Payload any = (Payload) sbd.getAny();
        byte[] asic = null;
        if (!any.getConversation().getServiceIdentifier().equals(ServiceIdentifier.DPE_RECEIPT)) {
            asic = DatatypeConverter.parseBase64Binary(any.getContent());
        }
        String props = any.getConversation().getCustomProperties().values().stream().reduce((a, b) -> a + " " + b).orElse("");
        DpeMessage dpeMessage = new DpeMessage(props);
        sbd.setAny(dpeMessage);
        return ServiceBusPayload.of(sbd, asic);
    }
}
