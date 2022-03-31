package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DpiConversationStrategy;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.jetbrains.annotations.NotNull;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Slf4j
@RequiredArgsConstructor
public class DpiConversationStrategyImpl implements DpiConversationStrategy {

    private final ServiceRegistryLookup sr;
    private final MeldingsformidlerRequestFactory meldingsformidlerRequestFactory;
    private final MeldingsformidlerClient meldingsformidlerClient;
    private final ConversationService conversationService;
    private final PrintService printService;
    private final PromiseMaker promiseMaker;

    @Override
    @Timed
    public void send(@NotNull NextMoveOutMessage message) throws NextMoveException {
        ServiceRecord serviceRecord = getServiceRecord(message);

        message.getBusinessMessage(DpiDigitalMessage.class).ifPresent(bmsg ->
                conversationService.findConversation(message.getMessageId()).ifPresent(c -> {
                    c.setMessageTitle(bmsg.getTittel());
                    conversationService.save(c);
                })
        );

        try {
            promiseMaker.promise(reject -> {
                MeldingsformidlerRequest request = meldingsformidlerRequestFactory.getMeldingsformidlerRequest(message, serviceRecord, reject);

                try {
                    meldingsformidlerClient.sendMelding(request);
                } catch (MeldingsformidlerException e) {
                    Audit.error("Failed to send message to DPI", markerFrom(message), e);
                    reject.reject(e);
                }

                return null;
            }).await();
        } catch (Exception e) {
            throw new NextMoveException(e);
        }
    }

    private ServiceRecord getServiceRecord(NextMoveOutMessage message) {
        if (message.getReceiver() != null) {
            try {
                return sr.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                                .conversationId(message.getConversationId())
                                .process(message.getProcessIdentifier())
                                .build(),
                        message.getSbd().getDocumentType());
            } catch (ServiceRegistryLookupException e) {
                throw new MeldingsUtvekslingRuntimeException(e);
            }
        } else {
            // Null receiver only allowed for print receiver
            KrrPrintResponse printDetails = printService.getPrintDetails();
            ServiceRecord serviceRecord = new ServiceRecord(DPI, printDetails.getPostkasseleverandoerAdresse(),
                    printDetails.getX509Sertifikat(), null);
            serviceRecord.setOrgnrPostkasse(printDetails.getPostkasseleverandoerAdresse());
            return serviceRecord;
        }
    }
}
