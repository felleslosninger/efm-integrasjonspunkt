package no.difi.meldingsutveksling.altinnv3.proxy;

import no.difi.meldingsutveksling.altinnv3.proxy.errorhandling.GlobalSpringSecurityErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class Config {

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(new GlobalSpringSecurityErrorHandler())
                .accessDeniedHandler(new GlobalSpringSecurityErrorHandler())
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges

                // allow actuator endpoints
                .pathMatchers("/actuator/**").permitAll()

                // allow access to correspondence for clients with correct scope
                .pathMatchers("/correspondence/api/**").hasAnyAuthority("SCOPE_altinn:broker.read")

                // deny all other url's
                .anyExchange().denyAll()

            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt())  // enable JWT validation
            .build();
    }

}
