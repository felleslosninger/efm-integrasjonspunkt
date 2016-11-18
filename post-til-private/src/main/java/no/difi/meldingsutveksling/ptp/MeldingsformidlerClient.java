package no.difi.meldingsutveksling.ptp;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ExternalReceipt;
import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.sdp.client2.KlientKonfigurasjon;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.AktoerOrganisasjonsnummer;
import no.difi.sdp.client2.domain.Avsender;
import no.difi.sdp.client2.domain.Databehandler;
import no.difi.sdp.client2.domain.Dokument;
import no.difi.sdp.client2.domain.Dokumentpakke;
import no.difi.sdp.client2.domain.Forsendelse;
import no.difi.sdp.client2.domain.Mottaker;
import no.difi.sdp.client2.domain.Noekkelpar;
import no.difi.sdp.client2.domain.Sertifikat;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.exceptions.SendException;
import no.difi.sdp.client2.domain.kvittering.ForretningsKvittering;
import no.difi.sdp.client2.domain.kvittering.KvitteringForespoersel;
import no.digipost.api.representations.Organisasjonsnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.logging.MarkerFactory.conversationIdMarker;

public class MeldingsformidlerClient {

    static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final EmptyKvittering EMPTY_KVITTERING = new EmptyKvittering();
    private SmsNotificationDigitalPostBuilderHandler smsNotificationHandler;
    private EmailNotificationDigitalPostBuilderHandler emailNotificationHandler;
    private final DigitalPostInnbyggerConfig config;
    private KeyStore keyStore;

    public MeldingsformidlerClient(DigitalPostInnbyggerConfig config, KeyStore keyStore) {
        this.config = config;
        this.keyStore = keyStore;
        smsNotificationHandler = new SmsNotificationDigitalPostBuilderHandler(config);
        emailNotificationHandler = new EmailNotificationDigitalPostBuilderHandler(config);
    }

    public void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        Mottaker mottaker = Mottaker.builder(
                request.getMottakerPid(),
                request.getPostkasseAdresse(),
                Sertifikat.fraByteArray(request.getCertificate()),
                Organisasjonsnummer.of(request.getOrgnrPostkasse())
        ).build();
        DigitalPost.Builder digitalPost = DigitalPost.builder(mottaker, request.getSubject())
                .virkningsdato(new Date())
                .sikkerhetsnivaa(config.getSecurityLevel());
        digitalPost = smsNotificationHandler.handle(request, digitalPost);
        digitalPost = emailNotificationHandler.handle(request, digitalPost);

        Dokument dokument = dokumentFromDocument(request.getDocument());
        Dokumentpakke dokumentpakke = Dokumentpakke.builder(dokument).vedlegg(toVedlegg(request.getAttachments())).build();

        final AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer = AktoerOrganisasjonsnummer.of(request.getSenderOrgnumber());
        Avsender behandlingsansvarlig = Avsender.builder(aktoerOrganisasjonsnummer.forfremTilAvsender()).build();
        Forsendelse forsendelse = Forsendelse.digital(behandlingsansvarlig, digitalPost.build(), dokumentpakke)
                .konversasjonsId(request.getConversationId())
                .mpcId(config.getMpcId())
                .spraakkode(config.getLanguage())
                .prioritet(config.getPriority())
                .build();

        SikkerDigitalPostKlient klient = createSikkerDigitalPostKlient(aktoerOrganisasjonsnummer);
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

    private SikkerDigitalPostKlient createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer, ClientInterceptor clientInterceptor) {
        KlientKonfigurasjon klientKonfigurasjon = createKlientKonfigurasjonBuilder().soapInterceptors(clientInterceptor).build();

        return createSikkerDigitalPostKlient(klientKonfigurasjon, aktoerOrganisasjonsnummer);
    }

    private SikkerDigitalPostKlient createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer) {
        KlientKonfigurasjon klientKonfigurasjon = createKlientKonfigurasjonBuilder().build();

        return createSikkerDigitalPostKlient(klientKonfigurasjon, aktoerOrganisasjonsnummer);
    }

    private SikkerDigitalPostKlient createSikkerDigitalPostKlient(KlientKonfigurasjon klientKonfigurasjon, AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer) {
        Databehandler tekniskAvsender = Databehandler.builder(aktoerOrganisasjonsnummer.forfremTilDatabehandler(), Noekkelpar.fraKeyStoreUtenTrustStore(keyStore, config.getKeystore().getAlias(), config.getKeystore().getPassword())).build();

        return new SikkerDigitalPostKlient(tekniskAvsender, klientKonfigurasjon);
    }

    private KlientKonfigurasjon.Builder createKlientKonfigurasjonBuilder() {
        return KlientKonfigurasjon.builder(config.getEndpoint()).connectionTimeout(20, TimeUnit.SECONDS);
    }


    public ExternalReceipt sjekkEtterKvittering(String orgnr) {
        Kvittering kvittering = new Kvittering();
        PayloadInterceptor payloadInterceptor = new PayloadInterceptor(kvittering::setRawReceipt);
        SikkerDigitalPostKlient klient = createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer.of(orgnr), payloadInterceptor);
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
        private final ServiceIdentifier serviceIdentifier = ServiceIdentifier.DPI;

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
            return conversationIdMarker(getId()).and(Markers.append("serviceIdentifier", serviceIdentifier));
        }

        @Override
        public MessageReceipt toMessageReceipt() {
            MessageReceipt domainReceipt = MessageReceipt.of(receiptStatus(), LocalDateTime.ofInstant(eksternKvittering.getTidspunkt(), ZoneId.systemDefault()));
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

        @Override
        public Conversation createConversation() {
            Conversation conv = Conversation.of(getId(), "unknown message reference", "unknown receiver", "unknown message title", ServiceIdentifier.DPI);
            conv.setMessageReceipts(new ArrayList<>(1));
            return conv;
        }

        public ReceiptType getReceiptType() {
            return ReceiptType.from(eksternKvittering);
        }

        private ReceiptStatus receiptStatus() {
            switch (getReceiptType()) {
                case DELIEVERED:
                    return ReceiptStatus.DELIVERED;
                case READ:
                    return ReceiptStatus.READ;
                case FEIL:
                case NOTIFICATION_FAILED:
                case POST_RETURNED:
                    return ReceiptStatus.FAIL;
                default:
                    return ReceiptStatus.OTHER;
            }
        }
    }


}