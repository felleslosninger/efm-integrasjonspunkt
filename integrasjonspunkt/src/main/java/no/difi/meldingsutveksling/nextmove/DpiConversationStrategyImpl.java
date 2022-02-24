package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DpiConversationStrategy;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@Slf4j
@Order
@RequiredArgsConstructor
public class DpiConversationStrategyImpl implements DpiConversationStrategy {

    private final IntegrasjonspunktProperties props;
    private final ServiceRegistryLookup sr;
    private final Clock clock;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final MeldingsformidlerClient meldingsformidlerClient;
    private final ConversationService conversationService;
    private final PrintService printService;

    @Override
    @Timed
    public void send(NextMoveOutMessage message) throws NextMoveException {
        ServiceRecord serviceRecord;
        if (message.getReceiver() == null) {
            // Null receiver only allowed for print receiver
            KrrPrintResponse printDetails = printService.getPrintDetails();
            serviceRecord = new ServiceRecord(DPI, printDetails.getPostkasseleverandoerAdresse(),
                printDetails.getX509Sertifikat(), null);
            serviceRecord.setOrgnrPostkasse(printDetails.getPostkasseleverandoerAdresse());
        } else {
            try {
                serviceRecord = sr.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                        .conversationId(message.getConversationId())
                        .process(message.getProcessIdentifier())
                        .build(),
                    message.getSbd().getDocumentType());
            } catch (ServiceRegistryLookupException e) {
                throw new MeldingsUtvekslingRuntimeException(e);
            }
        }

        if (message.getSbd().getBusinessMessage() instanceof DpiDigitalMessage) {
            DpiDigitalMessage bmsg = (DpiDigitalMessage) message.getSbd().getBusinessMessage();
            conversationService.findConversation(message.getMessageId()).ifPresent(c -> {
                c.setMessageTitle(bmsg.getTittel());
                conversationService.save(c);
            });
        }

        NextMoveDpiRequest request = new NextMoveDpiRequest(props, clock, message, serviceRecord, optionalCryptoMessagePersister);

        try {
            meldingsformidlerClient.sendMelding(request);
        } catch (MeldingsformidlerException e) {
            Audit.error("Failed to send message to DPI", markerFrom(message), e);
            throw new NextMoveException(e);
        }
    }
}
