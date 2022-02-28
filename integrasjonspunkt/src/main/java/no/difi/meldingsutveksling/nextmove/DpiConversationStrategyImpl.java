package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DpiConversationStrategy;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Slf4j
@RequiredArgsConstructor
public class DpiConversationStrategyImpl implements DpiConversationStrategy {

    private final ServiceRegistryLookup sr;
    private final MeldingsformidlerRequestFactory meldingsformidlerRequestFactory;
    private final MeldingsformidlerClient meldingsformidlerClient;
    private final ConversationService conversationService;
    private final PromiseMaker promiseMaker;

    @Override
    @Timed
    public void send(NextMoveOutMessage message) throws NextMoveException {
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
        try {
            return sr.getServiceRecord(SRParameter.builder(message.getReceiverIdentifier())
                            .conversationId(message.getConversationId())
                            .process(message.getProcessIdentifier())
                            .build(),
                    SBDUtil.getDocumentType(message.getSbd()));
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}
