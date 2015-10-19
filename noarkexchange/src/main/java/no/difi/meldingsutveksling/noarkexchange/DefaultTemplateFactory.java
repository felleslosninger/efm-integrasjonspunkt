package no.difi.meldingsutveksling.noarkexchange;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Factory to create Spring ws template when no authentication is provided
 */
public class DefaultTemplateFactory implements WebServiceTemplateFactory {
    @Override
    public WebServiceTemplate createTemplate(String contextPath) {
        WebServiceTemplate template = new WebServiceTemplate();
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(contextPath);
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        return template;
    }
}
