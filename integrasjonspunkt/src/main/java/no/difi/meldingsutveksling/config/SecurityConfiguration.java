package no.difi.meldingsutveksling.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

// FIXME - denne krever en WebSecurityConfiguration definisjon
//@Configuration
//@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enable-auth", havingValue = "false")
    SecurityFilterChain noAuthConfigSecurityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(management -> management
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)).cors(withDefaults()).csrf(csrf -> csrf.disable());
        http.headers(headers -> headers
                .httpStrictTransportSecurity(security -> security
                        .includeSubDomains(true)));
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/").permitAll());
        return http.build();
    }
    @Bean
    @ConditionalOnProperty(name = "difi.move.feature.enable-auth", havingValue = "true")
    SecurityFilterChain authConfigSecurityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(management -> management
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)).cors(withDefaults()).csrf(csrf -> csrf.disable());
        http.headers(headers -> headers
                .httpStrictTransportSecurity(security -> security
                        .includeSubDomains(true)));
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/manage/health", "/health").permitAll()
                .anyRequest().authenticated()).httpBasic(withDefaults());
        return http.build();

    }

}
