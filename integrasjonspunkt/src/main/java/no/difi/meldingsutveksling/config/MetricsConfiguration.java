package no.difi.meldingsutveksling.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.boot.actuate.metrics.web.client.DefaultRestTemplateExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTags;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableAspectJAutoProxy // Enables defining aspects using @Aspect annotations
public class MetricsConfiguration {

    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        // Provides aspect that causes Prometheus metrics to be generated for methods annotated with @Timed
        return new TimedAspect(meterRegistry);
    }

    @Bean
    public RestTemplateExchangeTagsProvider restTemplateExchangeTagsProvider() {
        return new DefaultRestTemplateExchangeTagsProvider() {

            @Override
            public Iterable<Tag> getTags(String urlTemplate, HttpRequest request, ClientHttpResponse response) {
                String urlTemplateWithoutQueryString = stripQueryString(urlTemplate);
                return super.getTags(urlTemplateWithoutQueryString, request, response);
            }

            private String stripQueryString(String url) {
                if (url == null) {
                    return null;
                }
                int i = url.indexOf("?");
                if (i == -1) {
                    return url;
                }
                return url.substring(0, i);
            }
        };
    }

}
