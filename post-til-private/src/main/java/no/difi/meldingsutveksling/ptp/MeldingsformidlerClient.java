package no.difi.meldingsutveksling.ptp;

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

import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MeldingsformidlerClient {

    static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Config config;

    public MeldingsformidlerClient(Config config) {
        this.config = config;
    }

    public void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        Mottaker mottaker = Mottaker.builder(request.getMottakerPid(), request.getPostkasseAdresse(), Sertifikat.fraByteArray(request.getCertificate()), Organisasjonsnummer.of(request.getOrgnrPostkasse())).build();
        DigitalPost digitalPost = DigitalPost.builder(mottaker, request.getSubject()).virkningsdato(new Date()).build();
        Dokument dokument = Dokument.builder(request.getDocument().getTitle(), request.getDocument().getFileName(), request.getDocument().getContents()).mimeType(request.getDocument().getMimeType()).build();
        Dokumentpakke dokumentpakke = Dokumentpakke.builder(dokument)/*TODO.vedlegg(Dokument.builder())*/.build(); // skal dokumentpakke ha vedlegg?
        final AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer = AktoerOrganisasjonsnummer.of(request.getSenderOrgnumber());
        Avsender behandlingsansvarlig = Avsender.builder(aktoerOrganisasjonsnummer.forfremTilAvsender()).build();

        Forsendelse forsendelse = Forsendelse.digital(behandlingsansvarlig, digitalPost, dokumentpakke)
                .konversasjonsId(request.getConversationId()) // fra integrasjonspunkt?
                .mpcId(config.getMpcId())
//                .prioritet(Prioritet.NORMAL) // eller Prioritet.Prioritert?
//                .spraakkode(request.getSpraakKode())
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

    public Kvittering sjekkEtterKvittering(String orgnr) {
        SikkerDigitalPostKlient klient = createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer.of(orgnr));
        final ForretningsKvittering forretningsKvittering = klient.hentKvittering(KvitteringForespoersel.builder(Prioritet.NORMAL).mpcId(config.getMpcId()).build());
        return new Kvittering(forretningsKvittering).withCallback(klient::bekreft);
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
            final String keystoreAlias = config.getKeystore().getPassword();
            final String mpcId = config.getMpcId();
            return new Config(url, keyStore, keystoreAlias, keystorePassword, mpcId);
        }

    }

    public class Kvittering {
        private transient final ForretningsKvittering eksternKvittering;
        private Consumer<ForretningsKvittering> callback;

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
    }


}