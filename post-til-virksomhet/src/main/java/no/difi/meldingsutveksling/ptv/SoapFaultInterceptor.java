package no.difi.meldingsutveksling.ptv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.logging.Audit;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

import java.util.Optional;

/**
 * Used to log soap faults from Spring web service template
 */
@Slf4j
@RequiredArgsConstructor
public class SoapFaultInterceptor implements ClientInterceptor {

    private final LogstashMarker logMarkers;

    /**
     * Creates SoapFaultInterceptor that uses provided logmarkers when logging faults
     *
     * @param logMarkers the log markers to be used when logging
     * @return new instance
     */
    static SoapFaultInterceptor withLogMarkers(LogstashMarker logMarkers) {
        return new SoapFaultInterceptor(logMarkers);
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
    static class SoapFaultException extends WebServiceClientException {
        SoapFaultException(String msg) {
            super(msg);
        }
    }

    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    @Override
    public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {
        if (Optional.ofNullable(ex).filter(e -> e instanceof SoapFaultException).isPresent()) {
            final WebServiceMessage response = messageContext.getResponse();
            Audit.error("Failed to send message to correspondence agency",
                    logMarkers.and(Markers.append("soap_fault", XMLUtil.asString(response.getPayloadSource(), logMarkers))), ex);
        }
    }
}
