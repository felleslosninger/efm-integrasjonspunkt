package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.DownloadRequest;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.StandardBusinessDocumentConverter;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Runnable wrapper for web service calls to the integrasjonspunkt receiving documents from Altinn
 * Enables thread pooling / paralell execution
 *
 * @author Glenn Bech
 */
class CallReceiveRunnable implements Runnable {

    private static JAXBContext jaxbContext, jaxbContextdomain;
    private Logger logger = LoggerFactory.getLogger(CallReceiveRunnable.class.getName());

    static {
        try {
            jaxbContext = JAXBContext.newInstance(no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class, Payload.class);
            jaxbContextdomain = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class);

        } catch (JAXBException e) {
            throw new RuntimeException("Could not initialize " + StandardBusinessDocumentConverter.class, e);
        }
    }

    private ReceiveClientContext ctx;
    private FileReference file;

    public CallReceiveRunnable(ReceiveClientContext ctx, FileReference file) {
        this.ctx = ctx;
        this.file = file;
    }

    @Override
    public void run() {
        DownloadRequest request = new DownloadRequest(file.getValue(), ctx.getOrgNr());
        StandardBusinessDocument doc = ctx.getAltinnWsClient().download(request);

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JAXBElement<StandardBusinessDocument>  d = new ObjectFactory().createStandardBusinessDocument(doc);

            jaxbContextdomain.createMarshaller().marshal(d, os);
            byte[] tmp = os.toByteArray();

            JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument> toDocument
                    = (JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument>)
                    jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(tmp));

            ctx.getReceiveClient().callReceive(toDocument.getValue());
            logger.info("successfully wrote " + toDocument.getValue().getStandardBusinessDocumentHeader().getDocumentIdentification().getInstanceIdentifier());

        } catch (JAXBException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
        }

    }
}
