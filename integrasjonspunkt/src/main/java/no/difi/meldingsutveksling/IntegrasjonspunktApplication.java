package no.difi.meldingsutveksling;

import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

/**
 * @author Dervis M, 13/08/15.
 */

@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
public class IntegrasjonspunktApplication extends SpringBootServletInitializer {
    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktApplication.class);

    @Bean
    public ServletRegistrationBean servletNoArk() {
        WSSpringServlet servlet = new WSSpringServlet();
        ServletRegistrationBean reg = new ServletRegistrationBean(servlet, "/noarkExchange", "/receive");
        reg.setLoadOnStartup(1);
        return reg;
    }

    public static void main(String[] args) {
        Marker myMarker = MarkerFactory.getMarker("MyMarker");
        log.error(Markers.append("My second marker", "with value"), "yoyoyo");
        log.error(myMarker, "hello using myMarker");

        try {
            SpringApplication.run(IntegrasjonspunktApplication.class, args);
        }
        catch (SecurityException se) {
            String message =
                    "Failed startup. Possibly unlimited security policy files that is not updated." +
                            "/r/nTo fix this, download and replace policy files for the apropriate java version (found in ${java.home}/jre/lib/security/)" +
                            "/r/n- Java7: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html" +
                            "/r/n- Java8: http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html";

            System.out.println(message);
            log.error(message);
            log.error(se.getMessage());
        }
    }
}
