package no.difi.meldingsutveksling.ptp;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.receipt.ExternalReceipt;
import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.sdp.client2.KlientKonfigurasjon;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.*;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.exceptions.SendException;
import no.difi.sdp.client2.domain.kvittering.ForretningsKvittering;
import no.difi.sdp.client2.domain.kvittering.KvitteringForespoersel;
import no.digipost.api.representations.Organisasjonsnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Transient;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static no.difi.meldingsutveksling.logging.MarkerFactory.conversationIdMarker;

public class MeldingsformidlerClient {

    static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final EmptyKvittering EMPTY_KVITTERING = new EmptyKvittering();
    private final Config config;

    public MeldingsformidlerClient(Config config) {
        this.config = config;
    }

    public void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        Mottaker mottaker = Mottaker.builder(
                request.getMottakerPid(),
                request.getPostkasseAdresse(),
                Sertifikat.fraByteArray(request.getCertificate()),
                Organisasjonsnummer.of(request.getOrgnrPostkasse())
        ).build();
        DigitalPost digitalPost = DigitalPost.builder(mottaker, request.getSubject())
                .virkningsdato(new Date())
                .build();
        Dokument dokument = Dokument.builder(
                request.getDocument().getTitle(),
                request.getDocument().getFileName(),
                request.getDocument().getContents()
        ).mimeType(request.getDocument().getMimeType())
                .build();
        Dokumentpakke dokumentpakke = Dokumentpakke.builder(dokument).build();
        final AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer = AktoerOrganisasjonsnummer.of(request.getSenderOrgnumber());
        Avsender behandlingsansvarlig = Avsender.builder(aktoerOrganisasjonsnummer.forfremTilAvsender()).build();

        Forsendelse forsendelse = Forsendelse.digital(behandlingsansvarlig, digitalPost, dokumentpakke)
                .konversasjonsId(request.getConversationId())
                .mpcId(config.getMpcId())
                .spraakkode(request.getSpraakKode())
                .build();

        SikkerDigitalPostKlient klient = createSikkerDigitalPostKlient(aktoerOrganisasjonsnummer);
        try {
            klient.send(forsendelse);
        } catch (SendException e) {
            throw new MeldingsformidlerException("Unable to send message to SDP", e);
        }
    }

    private SikkerDigitalPostKlient createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer) {
        KlientKonfigurasjon klientKonfigurasjon = KlientKonfigurasjon.builder(config.getUrl()).connectionTimeout(20, TimeUnit.SECONDS).build();


        Databehandler tekniskAvsender = Databehandler.builder(aktoerOrganisasjonsnummer.forfremTilDatabehandler(), Noekkelpar.fraKeyStoreUtenTrustStore(config.getKeyStore(), config.getKeystoreAlias(), config.getKeystorePassword())).build();

        return new SikkerDigitalPostKlient(tekniskAvsender, klientKonfigurasjon);
    }

    public ExternalReceipt sjekkEtterKvittering(String orgnr) {
        SikkerDigitalPostKlient klient = createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer.of(orgnr));
        final ForretningsKvittering forretningsKvittering = klient.hentKvittering(KvitteringForespoersel.builder(Prioritet.NORMAL).mpcId(config.getMpcId()).build());
        if (forretningsKvittering == null) {
            return EMPTY_KVITTERING;
        }
        return Kvittering.from(forretningsKvittering).withCallback(klient::bekreft);
    }

    public static class Config {
        private final String url;
        private KeyStore keyStore;
        private String keystoreAlias;
        private String keystorePassword;
        private final String mpcId;

        public Config(String url, KeyStore keyStore, String keystoreAlias, String keystorePassword, String mpcId) {
            this.url = url;
            this.keyStore = keyStore;
            this.keystoreAlias = keystoreAlias;
            this.keystorePassword = keystorePassword;
            this.mpcId = mpcId;
        }

        public String getUrl() {
            return url;
        }

        public KeyStore getKeyStore() {
            return keyStore;
        }

        public String getKeystoreAlias() {
            return keystoreAlias;
        }

        public String getKeystorePassword() {
            return keystorePassword;
        }

        public String getMpcId() {
            return mpcId;
        }

        public static Config from(DigitalPostInnbyggerConfig config, KeyStore keyStore) {
            final String url = config.getEndpoint();
            final String keystorePassword = config.getKeystore().getPassword();
            final String keystoreAlias = config.getKeystore().getAlias();
            final String mpcId = config.getMpcId();
            return new Config(url, keyStore, keystoreAlias, keystorePassword, mpcId);
        }

    }

    public static class Kvittering implements ExternalReceipt {
        @Transient
        private ForretningsKvittering eksternKvittering;
        private Consumer<ForretningsKvittering> callback;
        private final ServiceIdentifier serviceIdentifier = ServiceIdentifier.DPI;

        public Kvittering(ForretningsKvittering forretningsKvittering) {
            this.eksternKvittering = forretningsKvittering;
        }

        public Kvittering withCallback(Consumer<ForretningsKvittering> callback) {
            this.callback = callback;
            return this;
        }

        public void executeCallback() {
            callback.accept(eksternKvittering);
        }

        @Override
        public MessageReceipt update(final MessageReceipt messageReceipt) {
            MessageReceipt receipt = messageReceipt;
            if (messageReceipt == null) {
                receipt = MessageReceipt.of(eksternKvittering.getKonversasjonsId(), eksternKvittering.getReferanseTilMeldingId(), " kvittering fra DPI uten tilhørende melding?", ServiceIdentifier.DPI);
            }
            receipt.setLastUpdate(LocalDateTime.ofInstant(eksternKvittering.getTidspunkt(), ZoneId.systemDefault()));
            receipt.setReceived(true);
            return messageReceipt;
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
            return conversationIdMarker(getId()).and(Markers.append("receiptType", serviceIdentifier));
        }

        public static Kvittering from(ForretningsKvittering forretningsKvittering) {
            return new Kvittering(forretningsKvittering);
        }
    }


}