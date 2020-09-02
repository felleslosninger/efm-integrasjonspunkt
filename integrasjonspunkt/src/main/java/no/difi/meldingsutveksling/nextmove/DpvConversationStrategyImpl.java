package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.api.DpvConversationStrategy;
import no.difi.meldingsutveksling.bestedu.PutMessageRequestFactory;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.receive.BestEduAppReceiptService;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.status.ConversationService;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;
import static no.difi.meldingsutveksling.ptv.WithLogstashMarker.withLogstashMarker;

@Component
@Slf4j
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
@Order
@RequiredArgsConstructor
public class DpvConversationStrategyImpl implements DpvConversationStrategy {

    private final CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory;
    private final CorrespondenceAgencyClient client;
    private final ConversationService conversationService;
    private final IntegrasjonspunktProperties props;
    private final PromiseMaker promiseMaker;
    private final BestEduAppReceiptService bestEduAppReceiptService;

    @Override
    @Transactional
    public void send(@NotNull NextMoveOutMessage message) {

        promiseMaker.promise(reject -> {
            InsertCorrespondenceV2 correspondence = correspondenceAgencyMessageFactory.create(message, reject);

            Object response = withLogstashMarker(markerFrom(message))
                    .execute(() -> client.sendCorrespondence(correspondence));

            if (response == null) {
                throw new NextMoveRuntimeException("Failed to create Correspondence Agency Request");
            }

            String serviceCode = correspondence.getCorrespondence().getServiceCode().getValue();
            String serviceEditionCode = correspondence.getCorrespondence().getServiceEdition().getValue();
            conversationService.findConversation(message.getMessageId())
                    .ifPresent(conversation -> conversationService.save(conversation
                            .setServiceCode(serviceCode)
                            .setServiceEditionCode(serviceEditionCode)));
            return null;
        }).await();

        if (!isNullOrEmpty(props.getNoarkSystem().getType())) {
            bestEduAppReceiptService.sendAppReceiptToLocalNoark(message);
        }
    }

}
