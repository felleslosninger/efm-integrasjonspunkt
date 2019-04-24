package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.TransformerHelper;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class RequestCaptureClientInterceptor extends ClientInterceptorAdapter {

    private final TransformerHelper transformerHelper = new TransformerHelper();
    private final Holder<List<String>> webServicePayloadHolder;
    private final Holder<Message> messageSentHolder;
    private final DigipostAttachmentParser digipostAttachmentParser;

    @Override
    @SneakyThrows({TransformerException.class, IOException.class})
    public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {

        WebServiceMessage request = messageContext.getRequest();
        Source payloadSource = request.getPayloadSource();
        StringResult stringResult = new StringResult();
        transformerHelper.transform(payloadSource, stringResult);
        String payload = stringResult.toString();
        webServicePayloadHolder.getOrCalculate(ArrayList::new)
                .add(payload);

        if (request instanceof SaajSoapMessage) {
            handleSaajSoapMessage(payload, (SaajSoapMessage) request);
        }

        return true;
    }

    private void handleSaajSoapMessage(String payload, SaajSoapMessage soapMessage) throws IOException {
        if (soapMessage.getAttachments().hasNext()) {
            byte[] encryptedAsic = IOUtils.toByteArray(soapMessage.getAttachments().next().getInputStream());
            Message message = digipostAttachmentParser.parse(payload, encryptedAsic);
            messageSentHolder.set(message);
        }
    }
}
