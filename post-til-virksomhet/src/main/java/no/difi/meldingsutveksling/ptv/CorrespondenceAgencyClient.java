package no.difi.meldingsutveksling.ptv;

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
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.addressing.client.ActionCallback;
import org.springframework.ws.soap.addressing.version.Addressing10;
import org.springframework.ws.soap.axiom.AxiomSoapMessageFactory;
import org.springframework.ws.soap.security.wss4j.Wss4jSecurityInterceptor;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Used to send messages to Altinn InsertCorrespondence. InsertCorrespondence is used to send information to private companies.
 */
public class CorrespondenceAgencyClient {

    private final LogstashMarker logstashMarker;
    private final CorrespondenceAgencyConfiguration config;

    /**
     * Creates client to use Altinn Correspondence Agency
     * @param logstashMarker used when logging to keep track of message flow
     */
    public CorrespondenceAgencyClient(LogstashMarker logstashMarker, CorrespondenceAgencyConfiguration config) {
        this.logstashMarker = logstashMarker;
        this.config = config;
    }

    /**
     * Sends correspondence to Altinn Insert Correspondence
     * @param request containing the message along with sender/receiver
     * @return response if successful
     */
    public Object sendCorrespondence(CorrespondenceRequest request) {
        AxiomSoapMessageFactory newSoapMessageFactory = new AxiomSoapMessageFactory();
        newSoapMessageFactory.setSoapVersion(SoapVersion.SOAP_12);
        WebServiceTemplate template = new WebServiceTemplate(newSoapMessageFactory);
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        String contextPath = "no.altinn.services.serviceengine.correspondence._2009._10";
        marshaller.setContextPath(contextPath);
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        ClientInterceptor[] interceptors = new ClientInterceptor[2];
        interceptors[0] = createSecurityInterceptors(request.getUsername(), request.getPassword());
        interceptors[1] = SoapFaultInterceptorLogger.withLogMarkers(logstashMarker);
        template.setInterceptors(interceptors);

        final String uri = config.getEndpointUrl();
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

    public Object sendStatusRequest(CorrespondenceRequest request) {
        AxiomSoapMessageFactory newSoapMessageFactory = new AxiomSoapMessageFactory();
        newSoapMessageFactory.setSoapVersion(SoapVersion.SOAP_12);
        WebServiceTemplate template = new WebServiceTemplate(newSoapMessageFactory);
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        String contextPath = "no.altinn.services.serviceengine.correspondence._2009._10";
        marshaller.setContextPath(contextPath);
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        ClientInterceptor[] interceptors = new ClientInterceptor[2];
        interceptors[0] = createSecurityInterceptors(request.getUsername(), request.getPassword());
        interceptors[1] = SoapFaultInterceptorLogger.withLogMarkers(logstashMarker);
        template.setInterceptors(interceptors);

        final String uri = config.getEndpointUrl();
        final String soapAction = "http://www.altinn.no/services/ServiceEngine/Correspondence/2009/10/ICorrespondenceAgencyExternal/GetCorrespondenceStatusDetailsV2";
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
                .addInterceptorFirst((HttpRequestInterceptor) (httpRequest, httpContext) -> {
                    if(httpRequest.containsHeader(HTTP.CONTENT_LEN)) {
                        httpRequest.removeHeaders(HTTP.CONTENT_LEN);
                    }
                })
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();

        messageSender.setHttpClient(client);
        return messageSender;
    }

    private ClientInterceptor createSecurityInterceptors(String username, String password) {

        final Wss4jSecurityInterceptor securityInterceptor = new Wss4jSecurityInterceptor();
        securityInterceptor.setSecurementActions("UsernameToken");
        securityInterceptor.setSecurementUsername(username);
        securityInterceptor.setSecurementPassword(password);
        securityInterceptor.setSecurementPasswordType("PasswordText");
        securityInterceptor.setSecurementUsernameTokenElements("Nonce Created");

        return securityInterceptor;
    }


}
