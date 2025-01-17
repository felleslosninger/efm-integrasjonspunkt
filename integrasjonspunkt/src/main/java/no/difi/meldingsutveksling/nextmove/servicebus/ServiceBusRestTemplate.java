package no.difi.meldingsutveksling.nextmove.servicebus;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Component
public class ServiceBusRestTemplate extends RestTemplate {

    public ServiceBusRestTemplate(IntegrasjonspunktProperties props, ServiceBusRestErrorHandler serviceBusRestErrorHandler) {
        super(new HttpComponentsClientHttpRequestFactory(getHttpClient(props)));
        setErrorHandler(serviceBusRestErrorHandler);
    }

    private static CloseableHttpClient getHttpClient(IntegrasjonspunktProperties props) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(props.getNextmove().getServiceBus().getConnectTimeout(), TimeUnit.MILLISECONDS)
                .setConnectionRequestTimeout(props.getNextmove().getServiceBus().getConnectTimeout(), TimeUnit.MILLISECONDS)
                .setResponseTimeout(props.getNextmove().getServiceBus().getConnectTimeout(), TimeUnit.MILLISECONDS)
                .build();
        return HttpClientBuilder.create()
                .useSystemProperties()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
