package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.time.Clock;

@RequiredArgsConstructor
public class MeldingsformidlerRequestFactory {

    private final IntegrasjonspunktProperties properties;
    private final Clock clock;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;

    public MeldingsformidlerRequest getMeldingsformidlerRequest(NextMoveMessage nextMoveMessage, ServiceRecord serviceRecord, Reject reject) {
        return MeldingsformidlerRequest.builder().build();
    }
}
