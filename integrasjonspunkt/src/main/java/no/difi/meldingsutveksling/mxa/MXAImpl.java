package no.difi.meldingsutveksling.mxa;

import net.logstash.logback.marker.LogstashMarker;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.mxa.schema.MXADelegate;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.noarkexchange.putmessage.MessageStrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;
import no.difi.meldingsutveksling.ptv.mapping.CorrespondenceAgencyValues;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
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
    ServiceRegistryLookup serviceRegistryLookup;

    @Autowired
    IntegrasjonspunktConfiguration config;

    @Autowired
    private StrategyFactory strategyFactory;

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
            return XML_PARSE_ERROR;
        }

        Audit.info("MXA message received", markerFrom(msg));

        try {
            if (configuration.isQueueEnabled()) {
                internalQueue.enqueueMXA(msg);
                Audit.info("MXA message enqueued", markerFrom(msg));
            } else {
                Audit.info("Queue is disabled", markerFrom(msg));
                sendMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return INTERNAL_ERROR;
        }

        return SUCCESS;
    }

    public void sendMessage(Message msg) {
        final InfoRecord senderInfo = serviceRegistryLookup.getInfoRecord(configuration.getOrganisationNumber());
        final InfoRecord receiverInfo = serviceRegistryLookup.getInfoRecord(msg.getParticipantId());

        CorrespondenceAgencyConfiguration config = CorrespondenceAgencyConfiguration.configurationFrom(environment);

        CorrespondenceAgencyValues values = CorrespondenceAgencyValues.from(msg, senderInfo, receiverInfo);
        final InsertCorrespondenceV2 message = CorrespondenceAgencyMessageFactory.create(config, values);
        CorrespondenceAgencyClient client = new CorrespondenceAgencyClient(markerFrom(msg));
        final CorrespondenceRequest request = new CorrespondenceRequest.Builder().withUsername(config.getSystemUserCode()).withPassword(config.getPassword()).withPayload(message).build();
        client.send(request);
        Audit.info("MXA message sent", markerFrom(msg));
    }
}
