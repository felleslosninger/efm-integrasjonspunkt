package no.difi.meldingsutveksling.noarkexchange;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.webservice.support.SoapFaultInterceptorLogger;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;
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
        template.setMessageSender(createMessageSender());
        return template;
    }

    private HttpComponentsMessageSender createMessageSender() {
        final HttpComponentsMessageSender messageSender = new HttpComponentsMessageSender();
        messageSender.setHttpClient(getHttpClient());
        return messageSender;
    }

    private HttpClient getHttpClient() {
        return HttpClients.custom()
                .addInterceptorFirst((HttpRequestInterceptor) (httpRequest, httpContext) -> {
                    if (httpRequest.containsHeader(HTTP.CONTENT_LEN)) {
                        httpRequest.removeHeaders(HTTP.CONTENT_LEN);
                    }
                })
                .setConnectionManager(getConnectionManager())
                .setDefaultRequestConfig(getRequestConfig())
                .useSystemProperties()
                .build();
    }

    private PoolingHttpClientConnectionManager getConnectionManager() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(18);
        cm.setDefaultMaxPerRoute(6);
        return cm;
    }

    private RequestConfig getRequestConfig() {
        return RequestConfig.custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(30000)
                .setConnectionRequestTimeout(30000)
                .setCircularRedirectsAllowed(true)
                .setRedirectsEnabled(true)
                .setRelativeRedirectsAllowed(true)
                .build();
    }
}
