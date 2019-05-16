package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.ApplicationContextHolder;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.*;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.time.LocalDateTime;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.domain.sbdh.SBDUtil.isReceipt;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class AltinnConversationMessageHandler implements AltinnMessageHandler {

    private final InternalQueue internalQueue;
    private final IntegrasjonspunktNokkel keyInfo;
    private final TransportFactory transportFactory;
    private final ConversationService conversationService;
    private final ApplicationContextHolder applicationContextHolder;
    private final SBDReceiptFactory sbdReceiptFactory;
    private final MessageStatusFactory messageStatusFactory;

    @Override
    public void handleStandardBusinessDocument(StandardBusinessDocument sbd) {
        if (isReceipt(sbd)) {
            handleReceipt(sbd);
        } else {
            sendReceipt(sbd.getMessageInfo());
            log.debug(sbd.createLogstashMarkers(), "Delivery receipt sent");
            Conversation c = conversationService.registerConversation(
                    NextMoveInMessage.of(sbd, ServiceIdentifier.DPO)
            );
            internalQueue.enqueueNoark(sbd);
            conversationService.registerStatus(c, messageStatusFactory.getMessageStatus(ReceiptStatus.INNKOMMENDE_MOTTATT));
        }
    }

    private void handleReceipt(StandardBusinessDocument sbd) {
        JAXBElement<Kvittering> jaxbKvit = (JAXBElement<Kvittering>) sbd.getAny();
        Audit.info(format("Message id=%s is a receipt", sbd.getConversationId()),
                sbd.createLogstashMarkers().and(getReceiptTypeMarker(jaxbKvit.getValue())));
        MessageStatus status = statusFromKvittering(jaxbKvit.getValue());
        conversationService.registerStatus(sbd.getConversationId(), status);
    }

    private LogstashMarker getReceiptTypeMarker(Kvittering kvittering) {
        final String field = "receipt-type";
        if (kvittering.getLevering() != null) {
            return Markers.append(field, "levering");
        }
        if (kvittering.getAapning() != null) {
            return Markers.append(field, "åpning");
        }
        return Markers.append(field, "unkown");
    }

    private MessageStatus statusFromKvittering(Kvittering kvittering) {
        ReceiptStatus status = DpoReceiptMapper.from(kvittering);
        LocalDateTime tidspunkt = kvittering.getTidspunkt().toGregorianCalendar().toZonedDateTime().toLocalDateTime();
        return MessageStatus.of(status, tidspunkt);
    }

    private void sendReceipt(MessageInfo messageInfo) {
        StandardBusinessDocument doc = sbdReceiptFactory.createLeveringsKvittering(messageInfo, keyInfo);
        Transport t = transportFactory.createTransport(doc);
        t.send(applicationContextHolder.getApplicationContext(), doc);
    }
}
