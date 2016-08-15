package no.difi.meldingsutveksling.noarkexchange;

import net.logstash.logback.marker.LogstashMarker;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Abstract factory to create various types of Spring WS templates
 */
@FunctionalInterface
public interface WebServiceTemplateFactory {
    WebServiceTemplate createTemplate(String contextPath, LogstashMarker logMarkers);
}
