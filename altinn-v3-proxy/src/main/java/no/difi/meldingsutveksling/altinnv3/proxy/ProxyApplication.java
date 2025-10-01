package no.difi.meldingsutveksling.altinnv3.proxy;

import jakarta.inject.Inject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties(Oidc.class)
@SpringBootApplication
public class ProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProxyApplication.class, args);
	}

    @Value("${difi.move.altinnv3.proxy.correspondenceApiUrl}")
    private String correspondenceApiUrl;

    @Inject
    AltinnFunctions altinnFunctions;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {

        return builder.routes()
            .route("altinn-service", r -> r.path("/correspondence/api/**")
                .filters(f -> f
                    .filter(new LoggingFilter())
                    .filter(new TokenFilter(altinnFunctions))
                ).uri(correspondenceApiUrl)
            )
            .build();
    }

}
