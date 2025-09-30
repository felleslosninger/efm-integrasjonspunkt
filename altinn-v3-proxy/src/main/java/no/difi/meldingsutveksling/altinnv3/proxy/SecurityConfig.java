package no.difi.meldingsutveksling.altinnv3.proxy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(new GlobalSpringSecurityErrorHandler())
                .accessDeniedHandler(new GlobalSpringSecurityErrorHandler())
            )
            .authorizeExchange(exchanges -> exchanges

                // allow actuator endpoints, health, metrics / prometheus etc
                .pathMatchers("/actuator/**").permitAll()

                // allow access for clients with correct scope
                .anyExchange().hasAnyAuthority("SCOPE_altinn:broker.read")

                // just require a valid token
                //.anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt())  // enable JWT validation
            .build();
    }

}
