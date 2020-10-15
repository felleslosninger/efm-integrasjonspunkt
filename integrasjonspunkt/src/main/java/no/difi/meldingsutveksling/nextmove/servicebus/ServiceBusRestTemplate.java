package no.difi.meldingsutveksling.nextmove.servicebus;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ServiceBusRestTemplate extends RestTemplate {

    public ServiceBusRestTemplate(IntegrasjonspunktProperties props, ServiceBusRestErrorHandler serviceBusRestErrorHandler) {
        super(new HttpComponentsClientHttpRequestFactory(getHttpClient(props)));
        setErrorHandler(serviceBusRestErrorHandler);
    }

    private static CloseableHttpClient getHttpClient(IntegrasjonspunktProperties props) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(props.getNextmove().getServiceBus().getConnectTimeout())
                .setConnectionRequestTimeout(props.getNextmove().getServiceBus().getConnectTimeout())
                .setSocketTimeout(props.getNextmove().getServiceBus().getConnectTimeout())
                .build();
        return HttpClientBuilder.create()
                .useSystemProperties()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
