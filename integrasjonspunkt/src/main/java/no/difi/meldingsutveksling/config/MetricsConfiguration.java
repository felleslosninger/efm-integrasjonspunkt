package no.difi.meldingsutveksling.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.transport.context.TransportContextHolder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableAspectJAutoProxy // Enables defining aspects using @Aspect annotations
@RequiredArgsConstructor
@Slf4j
public class MetricsConfiguration {

    private final MeterRegistry meterRegistry;

    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        // Provides aspect that causes Prometheus metrics to be generated for methods annotated with @Timed
        return new TimedAspect(meterRegistry);
    }

    @Bean
    public ClientInterceptor metricsEndpointInterceptor() {
        // Provides interceptor that when injected causes Prometheus metrics to be generated
        return new ClientInterceptor() {

            private static final String START_TIME_IN_MS = "startTimeInMs";
            private static final String OUTCOME = "outcome";

            @Override
            public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
                messageContext.setProperty(START_TIME_IN_MS, System.currentTimeMillis());
                messageContext.setProperty(OUTCOME, "SUCCESS");
                return true;
            }

            @Override
            public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
                return true;
            }

            @Override
            public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
                messageContext.setProperty(OUTCOME, "FAULT");
                return true;
            }

            @Override
            public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {
                long stopTimeInMs = System.currentTimeMillis();
                long startTimeInMs = (long) messageContext.getProperty(START_TIME_IN_MS);
                long timeInMs = stopTimeInMs - startTimeInMs;
                double timeInS = (double) timeInMs / 1000;

                if (ex != null) {
                    messageContext.setProperty(OUTCOME, "FAULT");
                }

                URI uri;
                try {
                    uri = TransportContextHolder.getTransportContext().getConnection().getUri();
                } catch (URISyntaxException e) {
                    log.warn("URISyntaxException at MetricsConfiguration.afterCompletion");
                    return;
                }

                String clientName = uri.getHost();
                String outcome = (String) messageContext.getProperty(OUTCOME);
                String uriPath = uri.getPath();

                Set<Tag> tags = new HashSet<>();
                tags.add(new ImmutableTag("clientName", clientName));
                tags.add(new ImmutableTag("outcome", outcome));
                tags.add(new ImmutableTag("uri", uriPath));

                meterRegistry.counter("soap_ws_client_requests_seconds_count", tags).increment();
                meterRegistry.counter("soap_ws_client_requests_seconds_sum", tags).increment(timeInS);
                meterRegistry.gauge("soap_ws_client_requests_seconds_max", tags, timeInS);
            }
        };

    }

}
