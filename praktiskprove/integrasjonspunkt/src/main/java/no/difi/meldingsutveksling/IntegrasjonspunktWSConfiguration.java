package no.difi.meldingsutveksling;

import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;

import javax.xml.ws.Endpoint;


/**
 * @author Dervis M, 13/08/15.
 */

@Configuration
@ImportResource({"classpath*:rest-servlet.xml"})
@EnableAutoConfiguration
@ComponentScan("no.difi")
@EnableWs
public class IntegrasjonspunktWSConfiguration extends WsConfigurerAdapter{

    @Bean
    public ServletRegistrationBean servletNoArk() {
        ServletRegistrationBean reg = new ServletRegistrationBean(new WSSpringServlet(),"/noarkExchange", "/receive");
        reg.setLoadOnStartup(1);
        return reg;
    }

}
