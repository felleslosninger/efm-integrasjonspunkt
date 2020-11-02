package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.AltinnTransport;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.api.DpoConversationStrategy;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@Slf4j
@Order
public class DpoConversationStrategyImpl implements DpoConversationStrategy {

    private final AltinnTransport transport;
    private final AsicHandler asicHandler;
    private final PromiseMaker promiseMaker;
    private final SBDUtil sbdUtil;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final OptionalCryptoMessagePersister cryptoMessagePersister;

    @Override
    @Transactional
    public void send(NextMoveOutMessage message) {
        if (message.getFiles() == null || message.getFiles().isEmpty()) {
            transport.send(message.getSbd());
            return;
        }

        // Temporary hack to support old "arkivmelding" beta receivers - will be removed in a future update.
        // Integrasjonspunktet will in this case downgrade "arkivmelding" to beta format before sending.
        // See https://difino.atlassian.net/browse/MOVE-1952
        if (sbdUtil.isArkivmelding(message.getSbd())) {
            ServiceRecord record;
            try {
                record = serviceRegistryLookup.getReceiverServiceRecord(message.getSbd());
            } catch (ServiceRegistryLookupException e) {
                throw new NextMoveRuntimeException(e);
            }

            if (message.getSbd().getProcess().contains("ver5.5") && record.getProcess().contains("ver1.0")) {
                downgradeArkivmelding(message, record);
            }
        }


        try {
            promiseMaker.promise(reject -> {
                try (InputStream is = asicHandler.createEncryptedAsic(message, reject)) {
                    transport.send(message.getSbd(), is);
                    return null;
                } catch (IOException e) {
                    throw new NextMoveRuntimeException(String.format("Error sending message with messageId=%s to Altinn", message.getMessageId()), e);
                }
            }).await();
        } catch (Exception e) {
            Audit.error(String.format("Error sending message with messageId=%s to Altinn", message.getMessageId()), markerFrom(message), e);
            throw e;
        }

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to altinn",
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));
    }

    private void downgradeArkivmelding(NextMoveOutMessage message, ServiceRecord record) {
        message.getFiles().stream()
                .filter(f -> NextMoveConsts.ARKIVMELDING_FILE.equals(f.getFilename()))
                .findFirst()
                .ifPresent(f -> {
                    byte[] arkivmeldingBytes;
                    try {
                        arkivmeldingBytes = cryptoMessagePersister.read(message.getMessageId(), f.getIdentifier());
                        cryptoMessagePersister.write(message.getMessageId(), f.getIdentifier(), ArkivmeldingUtil.convertToBetaBytes(arkivmeldingBytes));
                    } catch (IOException e) {
                        throw new NextMoveRuntimeException(e);
                    }
                });
        message.getSbd().getScope(ScopeType.CONVERSATION_ID).setIdentifier(record.getProcess());
        record.getDocumentTypes().stream()
                .filter(DocumentType.ARKIVMELDING::fitsDocumentIdentifier)
                .findFirst()
                .ifPresent(s -> message.getSbd().getStandardBusinessDocumentHeader().getDocumentIdentification().setStandard(s));
    }

}
