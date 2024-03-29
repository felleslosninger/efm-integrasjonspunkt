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
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.jetbrains.annotations.NotNull;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Slf4j
@RequiredArgsConstructor
public class DpiConversationStrategyImpl implements DpiConversationStrategy {

    private static final String PRINT_DOCUMENT_TYPE = "urn:no:difi:digitalpost:xsd:fysisk::print";
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
        ServiceRecord serviceRecord;
        KrrPrintResponse printDetails = printService.getPrintDetails();
        String documentType = message.getSbd().getDocumentType();
        boolean isPrint = documentType.equals(PRINT_DOCUMENT_TYPE);
        if (message.getReceiver() != null) {
            try {
                serviceRecord = sr.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                                .conversationId(message.getConversationId())
                                .process(message.getProcessIdentifier())
                                .build(),
                        documentType);
                if (isPrint) {
                    serviceRecord.setPemCertificate(printDetails.getX509Sertifikat());
                }
            } catch (ServiceRegistryLookupException e) {
                throw new MeldingsUtvekslingRuntimeException(e);
            }
        } else {
            // Null receiver only allowed for print receiver
            serviceRecord = new ServiceRecord(DPI, printDetails.getPostkasseleverandoerAdresse(),
                    printDetails.getX509Sertifikat(), null);
        }
        if (isPrint) {
            serviceRecord.setOrgnrPostkasse(printDetails.getPostkasseleverandoerAdresse());
        }
        return serviceRecord;
    }
}
