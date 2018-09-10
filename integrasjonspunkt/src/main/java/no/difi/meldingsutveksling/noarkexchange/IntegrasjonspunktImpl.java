package no.difi.meldingsutveksling.noarkexchange;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCoreService;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecordWrapper;
import no.difi.meldingsutveksling.services.Adresseregister;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ServiceIdentifier.*;
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
    @Qualifier("mshClient")
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

        Predicate<String> personnrPredicate = Pattern.compile(String.format("\\d{%d}", 11)).asPredicate();
        if (personnrPredicate.test(organisasjonsnummer) && !strategyFactory.hasFactory(ServiceIdentifier.DPI)) {
            response.setResult(false);
            return response;
        }

        final ServiceRecordWrapper serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(organisasjonsnummer);
        } catch (Exception e) {
            log.error("Exception during service registry lookup: ", e);
            response.setResult(false);
            return response;
        }

        if (!serviceRecord.getFailedServiceIdentifiers().isEmpty()) {
            String failed = StringUtils.join(serviceRecord.getFailedServiceIdentifiers(), ", ");
            log.error("Service registry failed to look up one or more potential service identifiers - getCanReceive returning false (failed: {})", failed);
            response.setResult(false);
            return response;
        }

        final LogstashMarker marker = MarkerFactory.receiverMarker(organisasjonsnummer);
        boolean validServiceIdentifier = false;
        boolean mshCanReceive = false;
        boolean isDpv = false;
        if (asList(DPO, DPI, DPF).contains(serviceRecord.getServiceRecord().getServiceIdentifier()) &&
                strategyFactory.hasFactory(serviceRecord.getServiceRecord().getServiceIdentifier())) {
            validServiceIdentifier = true;
            Audit.info("CanReceive = true", marker);
        } else if (mshClient.canRecieveMessage(organisasjonsnummer)) {
            mshCanReceive = true;
            Audit.info("MSH canReceive = true", marker);
        } else {
            isDpv = true;
        }

        boolean strategyFactoryAvailable;
        if (isDpv) {
            strategyFactoryAvailable = strategyFactory.hasFactory(DPV);
        } else {
            strategyFactoryAvailable = validServiceIdentifier;
        }
        if (!strategyFactoryAvailable && !mshCanReceive) {
            Audit.error("CanReceive = false. Either feature toggle for DPV is disabled, or MSH cannot receive", marker);
            response.setResult(false);
            return response;
        }

        response.setResult(true);
        return response;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        PutMessageRequestWrapper message = new PutMessageRequestWrapper(request);
        if (PayloadUtil.isAppReceipt(message.getPayload())) {
            if ("p360".equalsIgnoreCase(properties.getNoarkSystem().getType())) {
                message.swapSenderAndReceiver();
            }

            if (!isNullOrEmpty(properties.getFiks().getInn().getFallbackSenderOrgNr()) &&
                    message.getRecieverPartyNumber().equals(properties.getFiks().getInn().getFallbackSenderOrgNr())) {
                Audit.info(String.format("Message is AppReceipt, but receiver (%s) is the configured fallback sender organization number. Discarding message.",
                        message.getRecieverPartyNumber()), markerFrom(message));
                return PutMessageResponseFactory.createOkResponse();
            }
        }

        ServiceRecordWrapper receiverRecord = serviceRegistryLookup.getServiceRecord(message.getRecieverPartyNumber());

        if (!receiverRecord.getFailedServiceIdentifiers().isEmpty()) {
            String failed = StringUtils.join(receiverRecord.getFailedServiceIdentifiers(), ", ");
            log.error("Service registry failed to look up one or more potential service identifiers - getCanReceive returning false (failed: {})", failed);
            return PutMessageResponseFactory.createErrorResponse(new MessageException(StatusMessage.FAILED_SERVICEIDENTIFIERS));
        }

        if (PayloadUtil.isAppReceipt(message.getPayload()) &&
                receiverRecord.getServiceRecord().getServiceIdentifier() != ServiceIdentifier.DPO) {
            Audit.info(String.format("Message is AppReceipt, but receiver (%s) is not DPO. Discarding message.",
                    message.getRecieverPartyNumber()), markerFrom(message));
            return PutMessageResponseFactory.createOkResponse();
        }

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
