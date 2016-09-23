package no.difi.meldingsutveksling.ptp;

import no.difi.sdp.client2.KlientKonfigurasjon;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.*;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.exceptions.SendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MeldingsformidlerClient {
    static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Config config;


    public MeldingsformidlerClient(Config config) {
        this.config = config;
    }

    public void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        Mottaker mottaker = Mottaker.builder(request.getMottakerPid(), request.getPostkasseAdresse(), Sertifikat.fraByteArray(request.getCertificate()), request.getOrgnrPostkasse()).build();
        DigitalPost digitalPost = DigitalPost.builder(mottaker, request.getSubject()).virkningsdato(new Date()).build();
        Dokument dokument = Dokument.builder(request.getDocumentTitle(), request.getDocumentName(), request.getDocument().getInputStream()).mimeType(request.getMimeType()).build();
        Dokumentpakke dokumentpakke = Dokumentpakke.builder(dokument)/*TODO.vedlegg(Dokument.builder())*/.build(); // skal dokumentpakke ha vedlegg?
        Behandlingsansvarlig behandlingsansvarlig = Behandlingsansvarlig.builder(request.getSenderOrgnumber()).build();

        Forsendelse forsendelse = Forsendelse.digital(behandlingsansvarlig, digitalPost, dokumentpakke)
                .konversasjonsId(request.getConversationId()) // fra integrasjonspunkt?
//                .mpcId(request.getQueueId()) // køid? unik for å unngå kollisjon med andre avsendere
//                .prioritet(Prioritet.NORMAL) // eller Prioritet.Prioritert?
//                .spraakkode(request.getSpraakKode())
                .build();

        KlientKonfigurasjon klientKonfigurasjon = KlientKonfigurasjon.builder().meldingsformidlerRoot(config.getUrl()).connectionTimeout(20, TimeUnit.SECONDS).build();


        TekniskAvsender tekniskAvsender = TekniskAvsender.builder(request.getSenderOrgnumber(), Noekkelpar.fraKeyStoreUtenTrustStore(config.getKeyStore(), config.getKeystoreAlias(), config.getKeystorePassword())).build();

        SikkerDigitalPostKlient klient = new SikkerDigitalPostKlient(tekniskAvsender, klientKonfigurasjon);
        try {
            klient.send(forsendelse);
        } catch (SendException e) {
            throw new MeldingsformidlerException("Unable to send message to SDP", e);
        }
    }

    public static class Config {
        private final String url;
        private KeyStore keyStore;
        private String keystoreAlias;
        private String keystorePassword;

        public Config(String url, KeyStore keyStore, String keystoreAlias, String keystorePassword) {
            this.url = url;
            this.keyStore = keyStore;
            this.keystoreAlias = keystoreAlias;
            this.keystorePassword = keystorePassword;
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
    }

}
