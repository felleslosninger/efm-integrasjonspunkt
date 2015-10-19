package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
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

import org.modelmapper.ModelMapper;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;


import javax.xml.bind.JAXBElement;
import java.io.IOException;

public class P360Client implements NoarkClient {

    public static final String SOAP_ACTION = "http://www.arkivverket.no/Noark/Exchange/IEDUImport/PutMessage";
    private NoarkClientSettings settings;

    public P360Client(NoarkClientSettings settings) {
        this.settings = settings;
    }

    @Override
    public boolean canRecieveMessage(String orgnr) {
        return false;
    }

    @Override
    public PutMessageResponseType sendEduMelding(PutMessageRequestType request) {

        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("no.difi.meldingsutveksling.noarkexchange.p360.schema");
        webServiceTemplate.setMarshaller(marshaller);
        webServiceTemplate.setUnmarshaller(marshaller);

        no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType r =
                new no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType();

        ModelMapper mapper = new ModelMapper();
        mapper.map(request, r);

        if (isAuthenticationEnabled() && isNTLMAuthentication()) {
            HttpComponentsMessageSender httpComponentsMessageSender = new HttpComponentsMessageSender();
            HttpClient httpClient = HttpClientBuilder.create()
                    .addInterceptorFirst(new PreemtiveNTLMRequestInterceptor()).build();
            httpComponentsMessageSender.setHttpClient(httpClient);
            webServiceTemplate.setMessageSender(httpComponentsMessageSender);
        }

        JAXBElement<no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType> p360request
                = new no.difi.meldingsutveksling.noarkexchange.p360.schema.ObjectFactory().createPutMessageRequest(r);

        JAXBElement<no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageResponseType> response
                = (JAXBElement) webServiceTemplate.marshalSendAndReceive(settings.getEndpointUrl(), p360request,
                new SoapActionCallback(SOAP_ACTION));

        PutMessageResponseType theResponse = new PutMessageResponseType();
        mapper.map(response.getValue(), theResponse);
        return theResponse;
    }

    private boolean isNTLMAuthentication() {
        return settings.getDomain() != null && !settings.getDomain().isEmpty();
    }

    private boolean isAuthenticationEnabled() {
        return settings.getUserName() != null && !settings.getUserName().isEmpty();
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
