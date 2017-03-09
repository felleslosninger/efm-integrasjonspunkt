package no.difi.meldingsutveksling.noarkexchange;

import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCoreService;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.logging.MoveLogMarkers;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.services.Adresseregister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.noarkexchange.PutMessageMarker.markerFrom;

/**
 * This is the implementation of the wenbservice that case managenent systems supporting the BEST/EDU stadard communicates with.
 * The responsibility of this component is to create, sign and encrypt a SBD message for delivery to a PEPPOL access point
 * <p/>
 * The access point for the recipient is looked up through ELMA and SMK, the certificates are retrived through a MOCKED adress
 * register component not yet imolemented in any infrastructure.
 * <p/>
 * <p/>
 * User: glennbech Date: 31.10.14 Time: 15:26
 */
@org.springframework.stereotype.Component("noarkExchangeService")
@WebService(portName = "NoarkExchangePort", serviceName = "noarkExchange", targetNamespace = "http://www.arkivverket.no/Noark/Exchange", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.SOAPport")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class IntegrasjonspunktImpl implements SOAPport {

    private static final Logger log = LoggerFactory.getLogger(IntegrasjonspunktImpl.class);

    @Autowired
    private NoarkClient mshClient;

    @Autowired
    private IntegrasjonspunktProperties properties;

    @Autowired
    private Adresseregister adresseRegister;

    @Autowired
    private EDUCoreService coreService;

    @Autowired
    private ServiceRegistryLookup serviceRegistryLookup;

    @Autowired
    private StrategyFactory strategyFactory;

    @Override
    public GetCanReceiveMessageResponseType getCanReceiveMessage(@WebParam(name = "GetCanReceiveMessageRequest", targetNamespace = "http://www.arkivverket.no/Noark/Exchange/types", partName = "getCanReceiveMessageRequest") GetCanReceiveMessageRequestType getCanReceiveMessageRequest) {

        String organisasjonsnummer = getCanReceiveMessageRequest.getReceiver().getOrgnr();

        GetCanReceiveMessageResponseType response = new GetCanReceiveMessageResponseType();
        boolean certificateAvailable;

        final ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(organisasjonsnummer);
        } catch (Exception e) {
            log.error("Exception during service registry lookup: ", e);
            response.setResult(false);
            return response;
        }

        certificateAvailable = adresseRegister.hasAdresseregisterCertificate(serviceRecord);

        final LogstashMarker marker = MarkerFactory.receiverMarker(organisasjonsnummer);
        boolean mshCanReceive = false;
        boolean isDpv = false;
        if (certificateAvailable) {
            Audit.info("CanReceive = true", marker);
        } else if (hasMshEndpoint()) {
            mshCanReceive = mshClient.canRecieveMessage(organisasjonsnummer);
            Audit.info(String.format("MSH canReceive = %s", mshCanReceive), marker);
        } else if (DPV.fullname().equals(serviceRecord.getServiceIdentifier())) {
            isDpv = true;
        }

        if (!mshCanReceive && !certificateAvailable && !isDpv) {
            Audit.error("CanReceive = false", marker);
        }

        if (!strategyFactory.hasFactory(serviceRecord)) {
            Audit.warn(String.format("StrategyFactory for %s not found. Feature toggle?", serviceRecord.getServiceIdentifier()), marker);
        }

        response.setResult((certificateAvailable || mshCanReceive || isDpv ) && strategyFactory.hasFactory(serviceRecord));
        return response;
    }

    private boolean hasMshEndpoint() {
        return !StringUtils.isBlank(properties.getMsh().getEndpointURL());
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        MDC.put(MoveLogMarkers.KEY_ORGANISATION_NUMBER, properties.getOrg().getNumber());
        PutMessageRequestWrapper message = new PutMessageRequestWrapper(request);
        if (!message.hasSenderPartyNumber()) {
            message.setSenderPartyNumber(properties.getOrg().getNumber());
        }

        Audit.info("Received EDU message", markerFrom(message));

        if (PayloadUtil.isEmpty(message.getPayload())) {
            Audit.error("Payload is missing", markerFrom(message));
            if (properties.getFeature().isReturnOkOnEmptyPayload()) {
                return PutMessageResponseFactory.createOkResponse();
            } else {
                return PutMessageResponseFactory.createErrorResponse(new MessageException(StatusMessage.MISSING_PAYLOAD));
            }
        }

        return coreService.queueMessage(message);
    }

    public void setMshClient(NoarkClient mshClient) {
        this.mshClient = mshClient;
    }

    public NoarkClient getMshClient() {
        return mshClient;
    }

    public void setAdresseRegister(Adresseregister adresseRegister) {
        this.adresseRegister = adresseRegister;
    }

    public EDUCoreService getCoreService() {
        return coreService;
    }

    void setCoreService(EDUCoreService coreService) {
        this.coreService = coreService;
    }

}
