package no.difi.meldingsutveksling.oauth2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class Oauth2ClientSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // http client enable OAuth2
        http.oauth2Client(Customizer.withDefaults());

        // stateless session, cors defaults and disable csrf
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.cors(withDefaults()).csrf(csrf -> csrf.disable());
        http.headers(headers -> headers.httpStrictTransportSecurity(security -> security.includeSubDomains(true)));

        // FIXME dette filter tar ikke hensyn til "difi.move.feature.enable-auth" flagget
        // slå sammen med logikken i no.difi.meldingsutveksling.config.SecurityConfiguration
        // sørg for å fjerne "github" fra application.yaml (det skal være mulig å lage en client registration med kode, uten ekstern konfig

        // denne gir 403 Forbidden
        //http.authorizeHttpRequests(requests -> requests.requestMatchers("/").permitAll());

        // denne gir 401 Unauthorized
        //http.authorizeHttpRequests(requests -> requests.requestMatchers("/manage/health", "/health").permitAll().anyRequest().authenticated()).httpBasic(withDefaults());

        return http.build();
    }

}
