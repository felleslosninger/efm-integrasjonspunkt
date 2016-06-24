package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.config.JmsConfiguration;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * This class can be used to explore the JMS queue.
 */
@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
public class InternalQueueMain extends SpringBootServletInitializer {

    @Autowired
    InternalQueue queue;

    public void testPut() {
        EduDocument eduDocument = new EduDocument();
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        header.setHeaderVersion("some header version");
        eduDocument.setStandardBusinessDocumentHeader(header);
        queue.enqueueNoark(eduDocument);
    }

    public static void main(String... args) {
        ConfigurableApplicationContext context = SpringApplication.run(new Object[]{InternalQueueMain.class, JmsConfiguration.class}, args);
        InternalQueueMain bean = context.getBean(InternalQueueMain.class);
        bean.testPut();
    }
}