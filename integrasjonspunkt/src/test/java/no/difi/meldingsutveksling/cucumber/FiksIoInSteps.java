package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.BusinessScope;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.ks.fiksio.FiksIoSubscriber;
import no.difi.move.common.io.WritableByteArrayResource;
import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.SvarSender;
import no.ks.fiks.io.client.model.AmqpChannelFeedbackHandler;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.model.MeldingId;
import no.ks.fiks.io.client.model.MottattMelding;
import no.ks.fiks.io.client.send.FiksIOSender;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class FiksIoInSteps {

    private final Holder<Message> messageInHolder;
    private final FiksIOKlient fiksIOKlient;
    private final FiksIoSubscriber fiksIoSubscriber;
    private final CreateAsic createAsic;

    @Before
    public void before() {
        messageInHolder.reset();
    }

    @And("^FIKS IO prepares a message with messageId \"([^\"]+)\"$")
    public void prepareFiksIoMessage(String messageId) {
        // only needed for generating manifest through asicFactory
        Message message = new Message()
            .setServiceIdentifier(ServiceIdentifier.DPFIO)
            .setMessageId(messageId)
            .setSbd(new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                    .setSenderIdentifier(Iso6523.parse("0192:123123123"))
                    .setReceiverIdentifier(Iso6523.parse("0192:321321321"))
                    .setDocumentIdentification(new DocumentIdentification()
                        .setStandard("")
                        .setInstanceIdentifier(messageId)
                    )
                    .setBusinessScope(new BusinessScope()
                        .addScope(new Scope()
                            .setType(ScopeType.CONVERSATION_ID.getFullname())
                            .setIdentifier("bar")
                            .setInstanceIdentifier(messageId)
                        )
                    )
                ));
        messageInHolder.set(message);
    }

    @And("^FIKS IO has the message available for kontoId \"([^\"]+)\" with protocol \"([^\"]+)\"$")
    @SuppressWarnings("unchecked")
    public void fiksIoHasTheMessageAvailable(String mottakerKontoId, String protocol) {
        Message message = messageInHolder.get();

        WritableByteArrayResource asic = new WritableByteArrayResource();
        createAsic.createAsic(message, asic);

        try (InputStream inputStream = asic.getInputStream()) {
            MottattMelding mottattMelding = MottattMelding.builder()
                .writeDekryptertZip(w -> {
                })
                .writeKryptertZip(w -> {
                })
                .getKryptertStream(() -> inputStream)
                .getDekryptertZipStream(() -> null)
                .meldingId(new MeldingId(UUID.fromString(message.getMessageId())))
                .meldingType(protocol)
                .mottakerKontoId(new KontoId(UUID.fromString(mottakerKontoId)))
                .avsenderKontoId(new KontoId(UUID.randomUUID()))
                .ttl(Duration.ofHours(1))
                .harPayload(true)
                .build();

            AmqpChannelFeedbackHandler amqpHandler = mock(AmqpChannelFeedbackHandler.class);
            when(amqpHandler.getHandleAck()).thenReturn(mock(Runnable.class));
            SvarSender svarSender = SvarSender.builder()
                .meldingSomSkalKvitteres(mottattMelding)
                .utsendingKlient(mock(FiksIOSender.class))
                .encrypt(e -> null)
                .amqpChannelFeedbackHandler(amqpHandler)
                .build();

            doAnswer(ans -> {
                BiConsumer<MottattMelding, SvarSender> consumer = ans.getArgument(0);
                consumer.accept(mottattMelding, svarSender);
                return null;
            }).when(fiksIOKlient).newSubscription(any(BiConsumer.class));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read ASIC!", e);
        }
    }

    @And("^the FIKS IO subscriber is registered$")
    public void registerSubscriber() {
        fiksIoSubscriber.registerSubscriber();
    }
}
