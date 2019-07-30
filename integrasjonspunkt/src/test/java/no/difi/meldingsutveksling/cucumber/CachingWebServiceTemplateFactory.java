package no.difi.meldingsutveksling.cucumber;

import lombok.Getter;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.noarkexchange.WebServiceTemplateFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

public class CachingWebServiceTemplateFactory implements WebServiceTemplateFactory {

    private final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();

    @Getter
    private final WebServiceTemplate webServiceTemplate;

    public CachingWebServiceTemplateFactory(RequestCaptureClientInterceptor requestCaptureClientInterceptor) {
        webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(marshaller);
        webServiceTemplate.setUnmarshaller(marshaller);
        webServiceTemplate.setInterceptors(new ClientInterceptor[]{requestCaptureClientInterceptor});
    }

    @Override
    public WebServiceTemplate createTemplate(String contextPath, LogstashMarker logMarkers) {
        marshaller.setContextPath(contextPath);
        return webServiceTemplate;
    }
}
