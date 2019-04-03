package no.difi.meldingsutveksling.dpi;

import lombok.RequiredArgsConstructor;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.receipt.ExternalReceipt;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.AktoerOrganisasjonsnummer;
import no.difi.sdp.client2.domain.Dokument;
import no.difi.sdp.client2.domain.Dokumentpakke;
import no.difi.sdp.client2.domain.Forsendelse;
import no.difi.sdp.client2.domain.exceptions.SendException;
import no.difi.sdp.client2.domain.kvittering.ForretningsKvittering;
import no.difi.sdp.client2.domain.kvittering.KvitteringForespoersel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.logging.MarkerFactory.conversationIdMarker;

@RequiredArgsConstructor
public class MeldingsformidlerClient {

    public static final EmptyKvittering EMPTY_KVITTERING = new EmptyKvittering();

    private final DigitalPostInnbyggerConfig config;
    private final SikkerDigitalPostKlientFactory sikkerDigitalPostKlientFactory;
    private final ForsendelseHandlerFactory forsendelseHandlerFactory;

    public void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        Dokument dokument = dokumentFromDocument(request.getDocument());
        Dokumentpakke dokumentpakke = Dokumentpakke.builder(dokument).vedlegg(toVedlegg(request.getAttachments())).build();

        ForsendelseBuilderHandler forsendelseBuilderHandler = forsendelseHandlerFactory.create(request);
        Forsendelse.Builder forsendelseBuilder = forsendelseBuilderHandler.handle(request, dokumentpakke);

        Forsendelse forsendelse = forsendelseBuilder.konversasjonsId(request.getConversationId())
                .mpcId(config.getMpcId())
                .spraakkode(config.getLanguage())
                .prioritet(config.getPriority()).build();

        SikkerDigitalPostKlient klient = sikkerDigitalPostKlientFactory.createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer.of(request.getSenderOrgnumber()));
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
        return Dokument.builder(
                document.getTitle(),
                document.getFileName(),
                document.getContents()
        ).mimeType(document.getMimeType())
                .build();
    }

    public ExternalReceipt sjekkEtterKvittering(String orgnr) {
        Kvittering kvittering = new Kvittering();
        PayloadInterceptor payloadInterceptor = new PayloadInterceptor(kvittering::setRawReceipt);
        SikkerDigitalPostKlient klient = sikkerDigitalPostKlientFactory.createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer.of(orgnr), payloadInterceptor);
        final ForretningsKvittering forretningsKvittering = klient.hentKvittering(KvitteringForespoersel.builder(config.getPriority()).mpcId(config.getMpcId()).build());
        if (forretningsKvittering == null) {
            return EMPTY_KVITTERING;
        }
        return kvittering.setEksternKvittering(forretningsKvittering).withCallback(klient::bekreft);
    }


    public static class Kvittering implements ExternalReceipt {
        private ForretningsKvittering eksternKvittering;
        private Consumer<ForretningsKvittering> callback;
        private String rawReceipt;

        public Kvittering() {
            /* This is empty because we need an instance of Kvittering before we have all the values provided for Kvittering.
             * But it could dropped empty constructor if we used a Builder */

        }

        public void setRawReceipt(String rawReceipt) {
            this.rawReceipt = rawReceipt;
        }

        public String getRawReceipt() {
            return rawReceipt;
        }

        public Kvittering withCallback(Consumer<ForretningsKvittering> callback) {
            this.callback = callback;
            return this;
        }

        public Kvittering setEksternKvittering(ForretningsKvittering eksternKvittering) {
            this.eksternKvittering = eksternKvittering;
            return this;
        }

        public void executeCallback() {
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
            MessageStatus domainReceipt = MessageStatus.of(getReceiptType(),
                    LocalDateTime.ofInstant(eksternKvittering.getTidspunkt(), ZoneId.systemDefault()));
            domainReceipt.setRawReceipt(getRawReceipt());
            return domainReceipt;
        }

        /**
         * Audit logs the receipt description
         */
        @Override
        public void auditLog() {
            getReceiptType().invokeLoggerMethod(logMarkers());
        }

        public DpiReceiptStatus getReceiptType() {
            return DpiReceiptStatus.from(eksternKvittering);
        }

    }
}