package no.difi.meldingsutveksling.ptv;

import lombok.SneakyThrows;
import no.altinn.services._2009._10.Test;
import no.altinn.services.serviceengine.correspondence._2009._10.CorrespondenceStatusHistoryResult;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.addressing.client.ActionCallback;
import org.springframework.ws.soap.addressing.version.Addressing10;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Used to send messages to Altinn InsertCorrespondence. InsertCorrespondence is used to send information to private companies.
 */
@Component
public class CorrespondenceAgencyClient extends WebServiceGatewaySupport {

    private final String endpointUrl;

    /**
     * Creates client to use Altinn Correspondence Agency
     */
    public CorrespondenceAgencyClient(CorrespondenceAgencyConfiguration config) {
        this.endpointUrl = config.getEndpointUrl();
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        String contextPath = "no.altinn.services.serviceengine.correspondence._2009._10";
        String testContextPath = "no.altinn.services._2009._10";
        marshaller.setContextPaths(contextPath, testContextPath);
        marshaller.setMarshallerProperties(getMarshallerProperties());

        WebServiceTemplate template = getWebServiceTemplate();
        template.setInterceptors(getInterceptors(config).toArray(new ClientInterceptor[0]));
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        template.setMessageSender(createMessageSender());
        template.setMessageFactory(getFactory());
    }

    protected Map<String, Object> getMarshallerProperties() {
        return Collections.emptyMap();
    }

    @SneakyThrows
    private static SaajSoapMessageFactory getFactory() {
        MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        return new SaajSoapMessageFactory(messageFactory);
    }

    private List<ClientInterceptor> getInterceptors(CorrespondenceAgencyConfiguration config) {
        List<ClientInterceptor> interceptors = new ArrayList<>();
        interceptors.add(createSecurityInterceptors(config.getSystemUserCode(), config.getPassword()));
        interceptors.add(new SoapFaultInterceptorLogger());
        interceptors.addAll(getAdditionalInterceptors());
        return interceptors;
    }

    protected List<ClientInterceptor> getAdditionalInterceptors() {
        return Collections.emptyList();
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
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(3, TimeUnit.SECONDS);
        cm.setMaxTotal(18);
        cm.setDefaultMaxPerRoute(6);
        return cm;
    }

    private RequestConfig getRequestConfig() {
        return RequestConfig.custom()
                .setSocketTimeout(10000)
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .setCircularRedirectsAllowed(true)
                .setRedirectsEnabled(true)
                .setRelativeRedirectsAllowed(true)
                .build();
    }

    private ClientInterceptor createSecurityInterceptors(String username, String password) {

        final Wss4jSecurityInterceptor securityInterceptor = new Wss4jSecurityInterceptor();

        securityInterceptor.setSecurementActions("UsernameToken");
        securityInterceptor.setSecurementUsername(username);
        securityInterceptor.setSecurementPassword(password);
        securityInterceptor.setSecurementPasswordType("PasswordText");
        securityInterceptor.setSecurementUsernameTokenNonce(true);
        securityInterceptor.setSecurementUsernameTokenCreated(true);
        securityInterceptor.setValidateResponse(false);
//        securityInterceptor.setSecurementUsernameTokenElements("Nonce Created"); // from the old decpreated WSS4JSecurityInterceptor. Not sure if the above is wor

        return securityInterceptor;
    }

    /**
     * Sends correspondence to Altinn Insert Correspondence
     *
     * @return response if successful
     */
    public Object sendCorrespondence(Object payload) {
        final String soapAction = "http://www.altinn.no/services/ServiceEngine/Correspondence/2009/10/ICorrespondenceAgencyExternal/InsertCorrespondenceV2";
        return getWebServiceTemplate().marshalSendAndReceive(this.endpointUrl, payload, getActionCallback(soapAction));
    }

    public Object sendStatusRequest(Object payload) {
        final String soapAction = "http://www.altinn.no/services/ServiceEngine/Correspondence/2009/10/ICorrespondenceAgencyExternal/GetCorrespondenceStatusDetailsV2";
        return getWebServiceTemplate().marshalSendAndReceive(this.endpointUrl, payload, getActionCallback(soapAction));
    }

    public CorrespondenceStatusHistoryResult sendStatusHistoryRequest(Object payload) {
        final String soapAction = "http://www.altinn.no/services/ServiceEngine/Correspondence/2009/10/ICorrespondenceAgencyExternal/GetCorrespondenceStatusHistory";
        return (CorrespondenceStatusHistoryResult) getWebServiceTemplate().marshalSendAndReceive(this.endpointUrl, payload, getActionCallback(soapAction));
    }

    public Object sendTestRequest() {
        final String soapAction = "http://www.altinn.no/services/2009/10/IAltinnContractBase/Test";
        Test testRequest = new Test();
        return getWebServiceTemplate().marshalSendAndReceive(this.endpointUrl, testRequest, getActionCallback(soapAction));
    }

    @SneakyThrows(URISyntaxException.class)
    private ActionCallback getActionCallback(String action) {
        return new ActionCallback(new URI(action), new
                Addressing10());
    }
}
