package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.receive.BestEduAppReceiptService;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class DpfConversationStrategy implements ConversationStrategy {

    private final SvarUtService svarUtService;
    private final IntegrasjonspunktProperties props;
    private final BestEduAppReceiptService bestEduAppReceiptService;

    @Override
    public void send(NextMoveOutMessage message) throws NextMoveException {
        svarUtService.send(message);

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to SvarUt",
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));

        if (!isNullOrEmpty(props.getNoarkSystem().getType())) {
            bestEduAppReceiptService.sendAppReceiptToLocalNoark(message);
        }
    }

}
