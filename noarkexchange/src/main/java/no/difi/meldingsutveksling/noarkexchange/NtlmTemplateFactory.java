package no.difi.meldingsutveksling.noarkexchange;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
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
import org.apache.http.protocol.HttpContext;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import java.io.IOException;
import java.util.Arrays;

/**
 * Factory to create Spring web template when authentication is provided along with domain
 */
public class NtlmTemplateFactory implements WebServiceTemplateFactory {
    NoarkClientSettings settings;

    public NtlmTemplateFactory(NoarkClientSettings settings) {
        this.settings = settings;
    }

    @Override
    public WebServiceTemplate createTemplate(String contextPath) {
        WebServiceTemplate template = new WebServiceTemplate();
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(contextPath);
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        template.setMessageSender(createMessageSender());

        return template;
    }

    private HttpComponentsMessageSender createMessageSender() {
        final HttpComponentsMessageSender messageSender = new HttpComponentsMessageSender();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(18);
        cm.setDefaultMaxPerRoute(6);

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(30000)
                .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM))
                .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
                .build();

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new NTCredentials(settings.getUserName(), settings.getPassword(), null, settings.getDomain()));

        HttpClient client = HttpClients.custom()
                .addInterceptorFirst(new HttpRequestInterceptor() {
                    @Override
                    public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
                        if(httpRequest.containsHeader(HTTP.CONTENT_LEN)) {
                            httpRequest.removeHeaders(HTTP.CONTENT_LEN);
                        }
                    }
                })
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(requestConfig)
                .build();
        messageSender.setHttpClient(client);
        return messageSender;
    }
}
