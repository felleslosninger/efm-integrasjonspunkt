package no.difi.meldingsutveksling.ks.svarut;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.QueueInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

@Component
public class SvarUtFaultInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SvarUtFaultInterceptor.class);

    @Override
    public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
        String soapFault = formatSoapFault(messageContext.getResponse().getPayloadSource());
        LogstashMarker markers = Markers.append("service_identifier", "DPF")
            .and(Markers.append("soap_fault", soapFault));
        log.error(markers, "Failed to send message");
        isUnrecoverable(soapFault);
        return true; // FIXME refactor this? : Unreachable in practice since isUnrecoverable always throws
    }

    private void isUnrecoverable(String soapFault) {
        if (soapFault.contains("Duplikat") ||
            soapFault.contains("Forsendelse med samme mottaker") ||
            soapFault.contains("Ugyldig innhold") ||
            soapFault.contains("Feil under lesing av") ||
            soapFault.contains("Kan ikke lagre tomt dokument")) {
            throw new QueueInterruptException(soapFault);
        } else {
            throw new SoapFaultException("Failed to send message");
        }
    }

    private String formatSoapFault(Source source) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StringWriter sw = new StringWriter();
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StreamResult sr = new StreamResult(sw);
            transformer.transform(source, sr);
            return sw.toString();
        } catch (TransformerException e) {
            throw new SoapFaultException("Failed to marshall webservice response to XML string", e);
        }
    }

    @Override
    public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {
        // no-op
    }

    static class SoapFaultException extends WebServiceClientException {
        public SoapFaultException(String msg, Throwable t) { super(msg, t); }
        public SoapFaultException(String msg) { super(msg); }
    }

}
