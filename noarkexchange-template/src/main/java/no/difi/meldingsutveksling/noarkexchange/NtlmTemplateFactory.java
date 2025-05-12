package no.difi.meldingsutveksling.noarkexchange;

import net.logstash.logback.marker.LogstashMarker;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import java.util.Collections;

/**
 * Factory to create Spring web template when authentication is provided along with domain
 */
public class NtlmTemplateFactory implements WebServiceTemplateFactory {
    NoarkClientSettings settings;

    public NtlmTemplateFactory(NoarkClientSettings settings) {
        this.settings = settings;
    }

    @Override
    public WebServiceTemplate createTemplate(String contextPath, LogstashMarker logMarkers) {
        WebServiceTemplate template = new DefaultTemplateFactory().createTemplate(contextPath, logMarkers);
        template.setMessageSender(createNTLMMessageSender());
        return template;
    }

    private HttpComponentsMessageSender createNTLMMessageSender() {
        final HttpComponentsMessageSender messageSender = new HttpComponentsMessageSender();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(18);
        cm.setDefaultMaxPerRoute(6);

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(30000)
                .setTargetPreferredAuthSchemes(Collections.singletonList(AuthSchemes.NTLM))
                .setProxyPreferredAuthSchemes(Collections.singletonList(AuthSchemes.BASIC))
                .build();

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new NTCredentials(settings.getUserName(), settings.getPassword(), null, settings.getDomain()));

        HttpClient client = HttpClients.custom()
                .addInterceptorFirst((HttpRequestInterceptor) (httpRequest, httpContext) -> {
                    if(httpRequest.containsHeader(HTTP.CONTENT_LEN)) {
                        httpRequest.removeHeaders(HTTP.CONTENT_LEN);
                    }
                })
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(requestConfig)
                .useSystemProperties()
                .build();
        messageSender.setHttpClient(client);
        return messageSender;
    }
}
