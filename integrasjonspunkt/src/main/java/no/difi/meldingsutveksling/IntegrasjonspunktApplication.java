package no.difi.meldingsutveksling;

import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
public class IntegrasjonspunktApplication extends SpringBootServletInitializer {

    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktApplication.class);
    private static final String MISSING_JCE_MESSAGE = "Failed startup. Possibly unlimited security policy files that is not updated."
            + "/r/nTo fix this, download and replace policy files for the apropriate java version (found in ${java.home}/jre/lib/security/)"
            + "/r/n- Java7: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html"
            + "/r/n- Java8: http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html";

    @Bean
    public ServletRegistrationBean servletNoArk() {
        WSSpringServlet servlet = new WSSpringServlet();
        ServletRegistrationBean reg = new ServletRegistrationBean(servlet, "/noarkExchange", "/receive", "/mxa");
        reg.setLoadOnStartup(1);
        return reg;
    }

    public static void main(String[] args) {
        try {
            if (!validateJCE()) {
                logMissingJCE();
                return;
            }
        } catch (SecurityException se) {
            logMissingJCE(se);
        }
        try (ConfigurableApplicationContext context = SpringApplication.run(IntegrasjonspunktApplication.class, args)) {
        };
    }

    private static void logMissingJCE(Exception e) {
        System.out.println(MISSING_JCE_MESSAGE);
        log.error(MISSING_JCE_MESSAGE);
        log.error(e.getMessage());
    }

    private static void logMissingJCE() {
        System.out.println(MISSING_JCE_MESSAGE);
        log.error(MISSING_JCE_MESSAGE);
    }

    private static boolean validateJCE() {
        try {
            int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
            if (maxKeyLen > 128) {
                return true;
            }
        } catch (NoSuchAlgorithmException ex) {
        }
        return false;

    }
}
