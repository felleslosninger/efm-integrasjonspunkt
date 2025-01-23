package no.difi.meldingsutveksling.oauth2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
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

        // FIXME dette filter tar ikke hensyn til "difi.move.feature.enable-auth" flagget see SecurityConfiguration.java
        // slå sammen med logikken i no.difi.meldingsutveksling.config.SecurityConfiguration

        http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());

        // denne gir 403 Forbidden
        //http.authorizeHttpRequests(requests -> requests.requestMatchers("/").permitAll());

        // denne gir 401 Unauthorized
        //http.authorizeHttpRequests(requests -> requests.requestMatchers("/manage/health", "/health").permitAll().anyRequest().authenticated()).httpBasic(withDefaults());

        return http.build();
    }

    @Bean
    public ClientRegistrationRepository dummyClientRegistrationRepository() {
        // FIXME spring boot needs a ClientRegistrationRepository to start, we just hacked together a dummy one
        ClientRegistration client = ClientRegistration.withRegistrationId("dummy").clientId("dummy").clientSecret("secret").authorizationGrantType(AuthorizationGrantType.JWT_BEARER).build();
        return new InMemoryClientRegistrationRepository(client);
    }

//    @Bean
//    public WebSecurityCustomizer noWebSecurityCustomizer() {
//        // FIXME forsøk på å skru av sikkerhet på REST endepunktene
//        return web -> web.ignoring().anyRequest();
//    }

}
