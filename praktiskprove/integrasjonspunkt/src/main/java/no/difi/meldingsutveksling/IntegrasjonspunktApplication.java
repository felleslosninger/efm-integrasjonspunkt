package no.difi.meldingsutveksling;

import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Dervis M, 13/08/15.
 */

@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
@EnableAutoConfiguration
public class IntegrasjonspunktApplication extends SpringBootServletInitializer {

    @Bean
    public ServletRegistrationBean servletNoArk() {
        WSSpringServlet servlet = new WSSpringServlet();
        ServletRegistrationBean reg = new ServletRegistrationBean(servlet, "/noarkExchange", "/receive");
        reg.setLoadOnStartup(1);
        return reg;
    }

    @Bean
    public DispatcherServlet dispatcherServlet() {
        return new DispatcherServlet();
    }

    @Bean
    public ServletRegistrationBean dispatcherServletRegistration() {
        ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet(), "/api/*");
        registration.setName(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
        registration.setLoadOnStartup(1);
        return registration;
    }

    public static void main(String[] args) {
        SpringApplication.run(IntegrasjonspunktApplication.class, args);
    }

}
