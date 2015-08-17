package no.difi.meldingsutveksling;

import com.sun.xml.ws.transport.http.servlet.WSServletContextListener;
import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * @author Dervis M, 13/08/15.
 */

@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
public class IntegrasjonspunktApplication extends SpringBootServletInitializer {

    @Override
    public void onStartup(final ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext);
        servletContext.addListener(new WSServletContextListener());
    }

    public static void main(String[] args) {
        SpringApplication.run(new Object[] {
                IntegrasjonspunktApplication.class,
                new ClassPathResource("rest-servlet.xml")
        }, args);
    }

}
