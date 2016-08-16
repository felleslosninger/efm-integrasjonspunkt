package no.difi.meldingsutveksling.noarkexchange;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.webservice.support.SoapFaultInterceptorLogger;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

/**
 * Factory to create Spring ws template when no authentication is provided
 */
public class DefaultTemplateFactory implements WebServiceTemplateFactory {

    @Override
    public WebServiceTemplate createTemplate(String contextPath, LogstashMarker logMarkers) {
        WebServiceTemplate template = new WebServiceTemplate();
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(contextPath);
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        template.setInterceptors(new ClientInterceptor[]{SoapFaultInterceptorLogger.withLogMarkers(logMarkers)});
        return template;
    }
}
