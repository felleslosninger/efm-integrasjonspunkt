package no.difi.meldingsutveksling.noarkexchange;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.NTLMScheme;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import java.io.IOException;

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
        HttpComponentsMessageSender httpComponentsMessageSender = new HttpComponentsMessageSender();
        HttpClient httpClient = HttpClientBuilder.create()
                .addInterceptorFirst(new PreemtiveNTLMRequestInterceptor()).build();
        httpComponentsMessageSender.setHttpClient(httpClient);


        return httpComponentsMessageSender;
    }


    private class PreemtiveNTLMRequestInterceptor implements HttpRequestInterceptor {
        @Override
        public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
            if (httpRequest.containsHeader(HTTP.CONTENT_LEN)) {
                httpRequest.removeHeaders(HTTP.CONTENT_LEN);
            }
            AuthState authState = (AuthState) httpContext.getAttribute(HttpClientContext.TARGET_AUTH_STATE);
            if (authState.getAuthScheme() == null) {
                AuthScheme ntlmScheme = new NTLMScheme();
                Credentials creds = new NTCredentials(settings.getUserName(), settings.getPassword(), null, settings.getDomain());
                authState.update(ntlmScheme, creds);
            }
        }
    }
}
