package no.difi.meldingsutveksling.mxa;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.core.EDUCoreMarker;
import no.difi.meldingsutveksling.core.EDUCoreSender;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.mxa.schema.MXADelegate;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.BindingType;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.mxa.MessageMarker.markerFrom;


@Component("mxaService")
@WebService(portName = "MXAPort", serviceName = "MXA", targetNamespace = "http://webservice.ws.altut.patent.siriusit.com/", endpointInterface = "no.difi.meldingsutveksling.mxa.schema.MXADelegate")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class MXAImpl implements MXADelegate {

    @Autowired
    private IntegrasjonspunktConfiguration configuration;

    @Autowired
    private Environment environment;

    @Autowired
    private InternalQueue internalQueue;

    @Autowired
    private ServiceRegistryLookup serviceRegistryLookup;

    @Autowired
    private IntegrasjonspunktConfiguration config;

    @Autowired
    private StrategyFactory strategyFactory;

    @Autowired
    private IntegrasjonspunktImpl integrasjonspunktImpl;

    @Autowired
    private EDUCoreSender eduCoreSender;

    private static final Logger log = LoggerFactory.getLogger(MXAImpl.class);
    private static final int SUCCESS= 0;
    private static final int INTERNAL_ERROR= 1;
    private static final int XML_PARSE_ERROR= 2;

    @Override
    public int submitMessage(@WebParam(name = "submitMessage") String arg0) {
        JAXBContext jaxbContext = null;
        Message msg = null;
        try {
            jaxbContext = JAXBContext.newInstance(Message.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            msg = (Message) unmarshaller.unmarshal(new StringReader(arg0));
        } catch (JAXBException e) {
            e.printStackTrace();
            Audit.error("XML parse error", markerFrom(msg));
            return XML_PARSE_ERROR;
        }
        Audit.info("MXA message received", markerFrom(msg));

        if (isNullOrEmpty(configuration.getOrganisationNumber())) {
            Audit.error("Senders orgnr missing", markerFrom(msg));
            throw new MeldingsUtvekslingRuntimeException("Missing senders orgnumber. Please configure orgnumber= in the integrasjonspunkt-local.properties");
        }
        if (isNullOrEmpty(msg.getParticipantId())) {
            Audit.error("Receiver identifier missing", markerFrom(msg));
            throw new MeldingsUtvekslingRuntimeException("Missing receiver identifier.");
        }


        // TODO: temp. fix for personal id number
        if (msg.getParticipantId().length() > 9) {
            String fileName = "MXA-" + Instant.now().toEpochMilli() + ".xml";
            File mxaMsgFile = new File(fileName);
            try {
                FileUtils.writeStringToFile(mxaMsgFile, arg0);
            } catch (IOException e) {
                e.printStackTrace();
                return INTERNAL_ERROR;
            }
            return SUCCESS;
        }

        EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);
        EDUCore message = eduCoreFactory.create(msg, configuration.getOrganisationNumber());

        try {
            if (configuration.isQueueEnabled()) {
                internalQueue.enqueueExternal(message);
                Audit.info("MXA message enqueued", EDUCoreMarker.markerFrom(message));
            } else {
                Audit.info("Queue is disabled", EDUCoreMarker.markerFrom(message));
                eduCoreSender.sendMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Audit.error("Internal error", EDUCoreMarker.markerFrom(message));
            return INTERNAL_ERROR;
        }

        return SUCCESS;
    }

}
