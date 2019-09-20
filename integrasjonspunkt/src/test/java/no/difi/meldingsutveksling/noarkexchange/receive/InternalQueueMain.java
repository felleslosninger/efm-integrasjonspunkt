package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.config.JmsConfiguration;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * This class can be used to explore the JMS queue.
 */
@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
public class InternalQueueMain extends SpringBootServletInitializer {

    @Autowired
    InternalQueue queue;

    public void testPut() {
        StandardBusinessDocument sbd = new StandardBusinessDocument();
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        header.setHeaderVersion("some header version");
        sbd.setStandardBusinessDocumentHeader(header);
        queue.enqueueNoark(sbd);
    }

    public static void main(String... args) {
        ConfigurableApplicationContext context = SpringApplication.run(new Class[]{InternalQueueMain.class, JmsConfiguration.class}, args);
        InternalQueueMain bean = context.getBean(InternalQueueMain.class);
        bean.testPut();
    }
}