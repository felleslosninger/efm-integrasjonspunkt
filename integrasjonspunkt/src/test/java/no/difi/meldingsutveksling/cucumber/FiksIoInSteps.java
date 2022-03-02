package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.ks.fiksio.FiksIoSubscriber;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.move.common.cert.KeystoreHelper;
import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.SvarSender;
import no.ks.fiks.io.client.model.AmqpChannelFeedbackHandler;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.io.client.model.MeldingId;
import no.ks.fiks.io.client.model.MottattMelding;
import no.ks.fiks.io.client.send.FiksIOSender;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.ObjectProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RequiredArgsConstructor
public class FiksIoInSteps {

    private final Holder<Message> messageInHolder;
    private final FiksIOKlient fiksIOKlient;
    private final AsicFactory asicFactory;
    private final Plumber plumber;
    private final PromiseMaker promiseMaker;
    private final ObjectProvider<CmsUtil> cmsUtilProvider;
    private final KeystoreHelper keystoreHelper;
    private final FiksIoSubscriber fiksIoSubscriber;

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
                                .addSender(new Partner().setIdentifier(new PartnerIdentification().setValue("0192:123123123")))
                                .addReceiver(new Partner().setIdentifier(new PartnerIdentification().setValue("0192:321321321")))
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

        byte[] asic = getAsic(message);
        MottattMelding mottattMelding = MottattMelding.builder()
                .writeDekryptertZip(w -> {
                })
                .writeKryptertZip(w -> {
                })
                .getKryptertStream(() -> new ByteArrayInputStream(asic))
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
    }

    @And("^the FIKS IO subscriber is registered$")
    public void registerSubscriber() {
        fiksIoSubscriber.registerSubscriber();
    }

    private byte[] getAsic(Message message) {
        if (cmsUtilProvider.getIfAvailable() == null) {
            throw new NextMoveRuntimeException("CmsUtil unavailable");
        }
        return promiseMaker.promise(reject -> {
            try (PipedInputStream is = plumber.pipe("create asic", inlet -> asicFactory.createAsic(message, inlet), reject)
                    .andThen("CMS encrypt", (outlet, inlet) -> cmsUtilProvider.getIfAvailable().createCMSStreamed(outlet, inlet, keystoreHelper.getX509Certificate())).outlet()) {
                return IOUtils.toByteArray(is);
            } catch (IOException e) {
                throw new NextMoveRuntimeException("Couldn't get ASIC", e);
            }
        }).await();
    }

}
