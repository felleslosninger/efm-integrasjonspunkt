package no.difi.meldingsutveksling.oauth2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
    @ConditionalOnProperty(name = "difi.move.feature.enable-auth", havingValue = "false")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // http client enable OAuth2
        http.oauth2Client(Customizer.withDefaults());

        // stateless session, cors defaults and disable csrf
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.cors(withDefaults()).csrf(AbstractHttpConfigurer::disable);
        http.headers(headers -> headers.httpStrictTransportSecurity(security -> security.includeSubDomains(true)));

        // uten auth er alt åpent
        http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());

        return http.build();

    }

    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enable-auth", havingValue = "true")
    public SecurityFilterChain filterChainWithBasicAuth(HttpSecurity http) throws Exception {

        // http client enable OAuth2
        http.oauth2Client(Customizer.withDefaults());

        // stateless session, cors defaults and disable csrf
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.cors(withDefaults()).csrf(AbstractHttpConfigurer::disable);
        http.headers(headers -> headers.httpStrictTransportSecurity(security -> security.includeSubDomains(true)));

        // med security så er kun observability åpent, api'er og websider stengt ned
        http.authorizeHttpRequests(requests ->
                requests.requestMatchers("/manage/*").permitAll().anyRequest().authenticated()).httpBasic(withDefaults());

        return http.build();

    }

    @Bean
    public ClientRegistrationRepository dummyClientRegistrationRepository() {
        // spring boot needs a ClientRegistrationRepository to start, we just hacked together a dummy one (consider refactoring)
        ClientRegistration client = ClientRegistration.withRegistrationId("dummy").clientId("dummy").clientSecret("secret").authorizationGrantType(AuthorizationGrantType.JWT_BEARER).build();
        return new InMemoryClientRegistrationRepository(client);
    }

}
