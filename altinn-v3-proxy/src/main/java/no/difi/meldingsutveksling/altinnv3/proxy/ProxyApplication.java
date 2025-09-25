package no.difi.meldingsutveksling.altinnv3.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProxyApplication.class, args);
	}

    @Value("${difi.move.altinnv3.proxy.correspondenceApiUrl}")
    private String correspondenceApiUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("test-service", r -> r.path("/test**")
                // FIXME dette er bare en test rute å teste filter og funksjoner
                .filters(f -> f
                    .rewritePath("/test(?<segment>.*)", "/get/#${segment}")
                    .filter(new LoggingFilter())
                ).uri("http://httpbin.org")
            )
            .route("altinn-service", r -> r.path("/correspondence/api/**")
                .filters(f -> f
                    .filter(new LoggingFilter())
                ).uri(correspondenceApiUrl)
            )
            .build();
    }

}
