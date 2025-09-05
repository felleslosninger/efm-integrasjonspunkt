package no.difi.meldingsutveksling.status.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Order
public class DphStatusStrategy implements StatusStrategy {


    @Override
    public void checkStatus(Set<Conversation> conversations) {

    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPH;
    }

    @Override
    public boolean isStartPolling(MessageStatus status) {
        return  ReceiptStatus.SENDT.toString().equals(status.getStatus());
    }

    @Override
    public boolean isStopPolling(MessageStatus status) {
        return ReceiptStatus.MOTTATT.toString().equals(status.getStatus());
    }
}
