package no.difi.meldingsutveksling.noarkexchange;

import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Abstract factory to create various types of Spring WS templates
 */
public interface WebServiceTemplateFactory {
    public abstract WebServiceTemplate createTemplate(String contextPath);
}
