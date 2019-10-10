package no.difi.meldingsutveksling.ptv;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.logging.Audit;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;

import java.util.Optional;

/**
 * Used to log soap faults from Spring web service template
 */
@Slf4j
public class SoapFaultInterceptorLogger extends ClientInterceptorAdapter {

    private static final String FAILED_TO_SEND_MESSAGE = "Failed to send message";

    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    @Override
    public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
        throw new SoapFaultException(FAILED_TO_SEND_MESSAGE);
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
        if (Optional.ofNullable(ex).filter(e -> e instanceof SoapFaultInterceptorLogger.SoapFaultException).isPresent()) {
            final WebServiceMessage response = messageContext.getResponse();
            String soapFault = XMLUtil.asString(response.getPayloadSource());
            Audit.error(FAILED_TO_SEND_MESSAGE, Markers.append("soap_fault", soapFault), ex);
            log.error("SoapFault: {}", soapFault);
        } else if (ex != null) {
            Audit.error(FAILED_TO_SEND_MESSAGE, ex);
        }
    }

}
