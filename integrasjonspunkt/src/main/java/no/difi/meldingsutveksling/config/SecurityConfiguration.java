package no.difi.meldingsutveksling.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @ConditionalOnProperty(name = "difi.move.feature.enable-auth", havingValue = "false")
    @Configuration
    public static class NoAuthConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and().cors().and().csrf().disable();
            http.headers()
                    .httpStrictTransportSecurity()
                    .includeSubDomains(true);
            http.authorizeRequests()
                    .antMatchers("/").permitAll();
        }
    }

    @ConditionalOnProperty(name = "difi.move.feature.enable-auth", havingValue = "true")
    @Configuration
    public static class AuthConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and().cors().and().csrf().disable();
            http.headers()
                    .httpStrictTransportSecurity()
                    .includeSubDomains(true);
            http.authorizeRequests()
                    .antMatchers("/manage/health", "/health").permitAll()
                    .anyRequest().authenticated()
                    .and().httpBasic();

        }
    }

}
