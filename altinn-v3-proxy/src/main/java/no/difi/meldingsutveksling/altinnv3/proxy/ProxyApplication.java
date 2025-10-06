package no.difi.meldingsutveksling.altinnv3.proxy;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.proxy.debug.LoggingFilter;
import no.difi.meldingsutveksling.altinnv3.proxy.properties.AltinnProperties;
import no.difi.meldingsutveksling.altinnv3.proxy.properties.Oidc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties({Oidc.class, AltinnProperties.class})
@SpringBootApplication
public class ProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProxyApplication.class, args);
	}

    @Inject
    private AltinnProperties altinnProperties;

    @Inject
    private AltinnFunctions altinnFunctions;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {

        return builder.routes()
            .route("altinn-service", r -> r.path("/correspondence/api/**")
                .filters(f -> f
                    .filter(new LoggingFilter())
                    .filter(new TokenFilter(altinnFunctions))
                ).uri(altinnProperties.baseUrl() + "/correspondence/api/")
            )
            .build();
    }

}
