package no.difi.meldingsutveksling.altinnv3.proxy;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.proxy.debug.LoggingFilter;
import no.difi.meldingsutveksling.altinnv3.proxy.debug.PingFilter;
import no.difi.meldingsutveksling.altinnv3.proxy.properties.AltinnProperties;
import no.difi.meldingsutveksling.altinnv3.proxy.properties.Oidc;
import no.difi.move.common.cert.KeystoreResourceLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@Slf4j
@EnableConfigurationProperties({Oidc.class, AltinnProperties.class})
@SpringBootApplication
public class ProxyApplication {

	public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ProxyApplication.class);
        app.setResourceLoader(new KeystoreResourceLoader()); // add support for base64: resource loading
        app.run(args);
	}

    @Inject
    private AltinnProperties altinnProperties;

    @Inject
    private AltinnFunctions altinnFunctions;

    @Inject
    private MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        log.info(altinnProperties.toString());
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {

        return builder.routes()
            .route("altinn-service", r -> r.path("/correspondence/api/**")
                .filters(f -> f
                    .filter(new LoggingFilter(meterRegistry))
                    .filter(new ProxyAuthFilter(altinnFunctions))
                ).uri(altinnProperties.baseUrl() + "/correspondence/api/")
            )
            .route("ping-service", r -> r.path("/ping")
                .filters(f -> f
                    .filter(new PingFilter(meterRegistry))
                ).uri("no://op") // will never be called, the delay filter will short circuit the route and return status
            )
            .build();
    }

}
