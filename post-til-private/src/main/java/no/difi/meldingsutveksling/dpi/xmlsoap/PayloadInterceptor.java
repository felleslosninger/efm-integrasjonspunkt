package no.difi.meldingsutveksling.dpi.xmlsoap;

import org.slf4j.Logger;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

public class PayloadInterceptor implements ClientInterceptor {
    private static final Logger logger = getLogger(MethodHandles.lookup().lookupClass().getName());
    private final Consumer<String> callback;

    public PayloadInterceptor(Consumer<String> payloadConsumer) {
        this.callback = payloadConsumer;
    }

    @Override
    public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
        Source payloadSource = messageContext.getResponse().getPayloadSource();
        if (payloadSource != null) {
            String payload = asXmlString(payloadSource);
            callback.accept(payload);
        }
        return true;
    }

    private String asXmlString(Source source) {
        StringWriter sw = new StringWriter();
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final Transformer transformer = factory.newTransformer();
            StreamResult sr = new StreamResult(sw);
            transformer.transform(source, sr);
            return sw.toString();
        } catch (TransformerException e) {
            logger.error("Failed to marshall webservice response to XML string", e);
        }
        return "";
    }

    @Override
    public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {
        // don't need it
    }
}
