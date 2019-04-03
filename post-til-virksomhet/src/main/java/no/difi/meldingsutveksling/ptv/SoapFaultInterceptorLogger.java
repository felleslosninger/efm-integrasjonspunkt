package no.difi.meldingsutveksling.ptv;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.logging.Audit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Optional;

/**
 * Used to log soap faults from Spring web service template
 */
public class SoapFaultInterceptorLogger extends ClientInterceptorAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SoapFaultInterceptorLogger.class);
    private static final String FAILED_TO_SEND_MESSAGE = "Failed to send message";

    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    @Override
    public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
        throw new SoapFaultInterceptorLogger.SoapFaultException(FAILED_TO_SEND_MESSAGE);
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
            LOG.error(getLogMarker(), "Failed to marshall webservice response to XML string", e);
        }
        return "";
    }

    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    @Override
    public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {
        if (Optional.ofNullable(ex).filter(e -> e instanceof SoapFaultInterceptorLogger.SoapFaultException).isPresent()) {
            final WebServiceMessage response = messageContext.getResponse();
            Audit.error("Failed to send message", getLogMarker().and(Markers.append("soap_fault", asString(response.getPayloadSource()))), ex);
        } else if (ex != null) {
            Audit.error("Failed to send message", ex);
        }
    }

    private LogstashMarker getLogMarker() {
        return LogstashMarkerHolder.get();
    }
}
