package no.difi.meldingsutveksling.dpi.xmlsoap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.ResourceUtils;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.status.ExternalReceipt;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.AktoerOrganisasjonsnummer;
import no.difi.sdp.client2.domain.Dokument;
import no.difi.sdp.client2.domain.Dokumentpakke;
import no.difi.sdp.client2.domain.Forsendelse;
import no.difi.sdp.client2.domain.exceptions.SendException;
import no.difi.sdp.client2.domain.kvittering.ForretningsKvittering;
import no.difi.sdp.client2.domain.kvittering.KvitteringForespoersel;
import no.digdir.dpi.client.domain.Document;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.logging.MarkerFactory.conversationIdMarker;

@Slf4j
@RequiredArgsConstructor
public class XmlSoapMeldingsformidlerClient implements MeldingsformidlerClient {

    private final DigitalPostInnbyggerConfig config;
    private final SikkerDigitalPostKlientFactory sikkerDigitalPostKlientFactory;
    private final ForsendelseHandlerFactory forsendelseHandlerFactory;
    private final DpiReceiptMapper dpiReceiptMapper;
    private final ClientInterceptor metricsEndpointInterceptor;
    private final MetadataDocumentConverter metadataDocumentConverter;

    private String nextMpcId() {
        if (config.getMpcConcurrency() > 1) {
            int i = ThreadLocalRandom
                    .current()
                    .nextInt(0, config.getMpcConcurrency());
            return config.getMpcId() + "-" + i;
        }
        return config.getMpcId();
    }

    @Override
    public void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        Dokument dokument = dokumentFromDocument(request.getDocument());
        Dokumentpakke dokumentpakke = Dokumentpakke.builder(dokument).vedlegg(toVedlegg(request.getAttachments())).build();

        ForsendelseBuilderHandler forsendelseBuilderHandler = forsendelseHandlerFactory.create(request);
        Forsendelse.Builder forsendelseBuilder = forsendelseBuilderHandler.handle(request, dokumentpakke);

        Forsendelse forsendelse = forsendelseBuilder.konversasjonsId(request.getConversationId())
                .mpcId(nextMpcId())
                .spraakkode(request.getLanguage())
                .prioritet(config.getPriority()).build();

        SikkerDigitalPostKlient klient = sikkerDigitalPostKlientFactory.createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer.of(request.getSenderOrgnumber()), metricsEndpointInterceptor);
        try {
            klient.send(forsendelse);
        } catch (SendException e) {
            throw new MeldingsformidlerException("Unable to send message to SDP", e);
        }
    }

    private List<Dokument> toVedlegg(List<Document> attachements) {
        return attachements.stream().map(this::dokumentFromDocument).collect(Collectors.toList());
    }

    private Dokument dokumentFromDocument(Document document) {
        Dokument.Builder builder = Dokument.builder(
                document.getTitle(),
                document.getFilename(),
                ResourceUtils.toByteArray(document.getResource()))
                .mimeType(document.getMimeType());
        if (document.getMetadataDocument() != null) {
            builder.metadataDocument(metadataDocumentConverter.toMetadataDokument(document.getMetadataDocument()));
        }
        return builder.build();
    }

    @Override
    public Flux<ExternalReceipt> sjekkEtterKvitteringer(String orgnr, String mpcId) {

        AtomicReference<String> rawReceipt = new AtomicReference<>();
        PayloadInterceptor payloadInterceptor = new PayloadInterceptor(rawReceipt::set);
        SikkerDigitalPostKlient klient = sikkerDigitalPostKlientFactory.createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer.of(orgnr), payloadInterceptor, metricsEndpointInterceptor);

        return Flux.create(fluxSink -> {
            try {
                for (Optional<Kvittering> item = getKvittering(klient, mpcId); item.isPresent(); item = getKvittering(klient, mpcId)) {
                    item.map(p -> p.setRawReceipt(rawReceipt.getAndSet(null)))
                            .ifPresent(fluxSink::next);
                }

                fluxSink.complete();
            } catch (Exception t) {
                fluxSink.error(t);
            }
        });
    }

    private Optional<Kvittering> getKvittering(SikkerDigitalPostKlient klient, String mpcId) {
        return getForretningsKvittering(klient, mpcId)
                .map(forretningsKvittering -> new Kvittering().setEksternKvittering(forretningsKvittering)
                        .withCallback(klient::bekreft)
                );
    }

    private Optional<ForretningsKvittering> getForretningsKvittering(SikkerDigitalPostKlient klient, String mpcId) {
        try {
            return Optional.ofNullable(klient.hentKvittering(KvitteringForespoersel.builder(config.getPriority()).mpcId(mpcId).build()));
        } catch (SendException e) {
            log.warn("Polling of DPI receipts failed with: {}", e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    public class Kvittering implements ExternalReceipt {
        private ForretningsKvittering eksternKvittering;
        private Consumer<ForretningsKvittering> callback;
        private String rawReceipt;

        Kvittering() {
            /* This is empty because we need an instance of Kvittering before we have all the values provided for Kvittering.
             * But it could dropped empty constructor if we used a Builder */

        }

        Kvittering setRawReceipt(String rawReceipt) {
            this.rawReceipt = rawReceipt;
            return this;
        }

        String getRawReceipt() {
            return rawReceipt;
        }

        Kvittering withCallback(Consumer<ForretningsKvittering> callback) {
            this.callback = callback;
            return this;
        }

        Kvittering setEksternKvittering(ForretningsKvittering eksternKvittering) {
            this.eksternKvittering = eksternKvittering;
            return this;
        }

        void executeCallback() {
            callback.accept(eksternKvittering);
        }

        @Override
        public void confirmReceipt() {
            executeCallback();
        }

        @Override
        public String getId() {
            return eksternKvittering.getKonversasjonsId();
        }

        @Override
        public LogstashMarker logMarkers() {
            return conversationIdMarker(getId());
        }

        @Override
        public MessageStatus toMessageStatus() {
            MessageStatus ms = dpiReceiptMapper.from(eksternKvittering);
            ms.setRawReceipt(getRawReceipt());
            return ms;
        }
    }
}