package no.difi.webservice.support;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.logging.Audit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Optional;

/**
 * Used to log soap faults from Spring web service template
 */
public class SoapFaultInterceptorLogger implements ClientInterceptor {
    private LogstashMarker logMarkers;
    private static final Logger logger = LoggerFactory.getLogger(SoapFaultInterceptorLogger.class);

    private SoapFaultInterceptorLogger(LogstashMarker logMarkers) {
        this.logMarkers = logMarkers;
    }

    /**
     * Creates no.difi.webservice.support.SoapFaultInterceptorLogger that uses provided logmarkers when logging faults
     * @param logMarkers the log markers to be used when logging
     * @return new instance
     */
    public static SoapFaultInterceptorLogger withLogMarkers(LogstashMarker logMarkers) {
        return new SoapFaultInterceptorLogger(logMarkers);
    }

    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    @Override
    public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
        return true;
    }

    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    @Override
    public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
        return true;
    }

    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    @Override
    public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
        throw new SoapFaultException("Failed to send message to correspondence agency");
    }

    @SuppressWarnings("squid:MaximumInheritanceDepth")
    class SoapFaultException extends WebServiceClientException {
        public SoapFaultException(String msg) {
            super(msg);
        }
    }

    private String asString(Source source) {
        StringWriter sw = new StringWriter();
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StreamResult sr = new StreamResult(sw);
            transformer.transform(source, sr);
            return sw.toString();
        } catch (TransformerException e) {
            logger.error(logMarkers, "Failed to marshall webservice response to XML string", e);
        }
        return "";
    }

    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    @Override
    public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {
        if (Optional.ofNullable(ex).filter(e -> e instanceof SoapFaultException).isPresent()) {
            final WebServiceMessage response = messageContext.getResponse();
            System.out.println("Fault from server: " + asString(response.getPayloadSource()));
            Audit.error("Failed to send message to correspondence agency", logMarkers.and(Markers.append("soap_fault", asString(response.getPayloadSource()))), ex);
        } else if (ex != null)
        {
            System.out.println("Fault from server: " + asString(messageContext.getResponse().getPayloadSource()));
            Audit.error("Failed to send message", logMarkers.and(Markers.append("soap_fault", asString(messageContext.getResponse().getPayloadSource()))), ex);
        }
    }
}
