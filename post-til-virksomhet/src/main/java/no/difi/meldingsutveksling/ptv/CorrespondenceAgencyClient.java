package no.difi.meldingsutveksling.ptv;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.addressing.client.ActionCallback;
import org.springframework.ws.soap.addressing.version.Addressing10;
import org.springframework.ws.soap.axiom.AxiomSoapMessageFactory;
import org.springframework.ws.soap.security.wss4j.Wss4jSecurityInterceptor;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class CorrespondenceAgencyClient {

    public Object send(CorrespondenceRequest request) {
        AxiomSoapMessageFactory newSoapMessageFactory = new AxiomSoapMessageFactory();
        newSoapMessageFactory.setSoapVersion(SoapVersion.SOAP_12);
        WebServiceTemplate template = new WebServiceTemplate(newSoapMessageFactory);
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        String contextPath = "no.altinn.services.serviceengine.correspondence._2009._10";
        marshaller.setContextPath(contextPath);
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        final ClientInterceptor[] interceptors = createSecurityInterceptors(request.getUsername(), request.getPassword());
        template.setInterceptors(interceptors);

        final String uri = "https://tt02.altinn.basefarm.net/ServiceEngineExternal/CorrespondenceAgencyExternal.svc";
        final String soapAction = "http://www.altinn.no/services/ServiceEngine/Correspondence/2009/10/ICorrespondenceAgencyExternal/InsertCorrespondenceV2";
        template.setMessageSender(createMessageSender());
        final URI actionURI;
        try {
            actionURI = new URI(soapAction);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return template.marshalSendAndReceive(uri, request.getCorrespondence(), new ActionCallback(actionURI, new Addressing10()));
    }

    private HttpComponentsMessageSender createMessageSender() {
        System.out.println("Created message sender");
        final HttpComponentsMessageSender messageSender = new HttpComponentsMessageSender();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(18);
        cm.setDefaultMaxPerRoute(6);

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(30000)
                .setConnectionRequestTimeout(30000)
                .setCircularRedirectsAllowed(true)
                .setRedirectsEnabled(true)
                .setRelativeRedirectsAllowed(true)
                .build();

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
                .setDefaultRequestConfig(requestConfig)
                .build();

        messageSender.setHttpClient(client);
        return messageSender;
    }

    private ClientInterceptor[] createSecurityInterceptors(String username, String password) {

        final ClientInterceptor[] clientInterceptors = new ClientInterceptor[1];
        final Wss4jSecurityInterceptor securityInterceptor = new Wss4jSecurityInterceptor();
        securityInterceptor.setSecurementActions("UsernameToken");
        securityInterceptor.setSecurementUsername(username);
        securityInterceptor.setSecurementPassword(password);
        securityInterceptor.setSecurementPasswordType("PasswordText");
        securityInterceptor.setSecurementUsernameTokenElements("Nonce Created");

        clientInterceptors[0] = securityInterceptor;
        return clientInterceptors;
    }


}
