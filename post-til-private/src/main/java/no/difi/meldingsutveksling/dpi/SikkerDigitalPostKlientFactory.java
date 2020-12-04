package no.difi.meldingsutveksling.dpi;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.move.common.cert.KeystoreProvider;
import no.difi.move.common.cert.KeystoreProviderException;
import no.difi.sdp.client2.KlientKonfigurasjon;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.*;
import no.difi.sdp.client2.internal.TrustedCertificates;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class SikkerDigitalPostKlientFactory {

    private final DigitalPostInnbyggerConfig config;
    private final KeyStore keyStore;

    public SikkerDigitalPostKlient createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer) {
        KlientKonfigurasjon klientKonfigurasjon = createKlientKonfigurasjonBuilder().build();
        return createSikkerDigitalPostKlient(klientKonfigurasjon, aktoerOrganisasjonsnummer);
    }

    public SikkerDigitalPostKlient createSikkerDigitalPostKlient(AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer, ClientInterceptor clientInterceptor) {
        KlientKonfigurasjon klientKonfigurasjon = createKlientKonfigurasjonBuilder().soapInterceptors(clientInterceptor).build();
        return createSikkerDigitalPostKlient(klientKonfigurasjon, aktoerOrganisasjonsnummer);
    }

    private SikkerDigitalPostKlient createSikkerDigitalPostKlient(KlientKonfigurasjon klientKonfigurasjon, AktoerOrganisasjonsnummer aktoerOrganisasjonsnummer) {
        Databehandler tekniskAvsender;
        if (this.config.getTrustStore() != null) {
            KeyStore trustStore;
            try {
                trustStore = KeystoreProvider.loadKeyStore(this.config.getTrustStore());
            } catch (KeystoreProviderException e) {
                throw new MeldingsUtvekslingRuntimeException("Cannot load DPI trust store", e);
            }
            KeyStore trustedSDP = TrustedCertificates.getTrustStore();
            Enumeration<String> aliases;
            try {
                aliases = trustedSDP.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Certificate certificate = trustedSDP.getCertificate(alias);
                    trustStore.setCertificateEntry(alias, certificate);
                }
            } catch (KeyStoreException e) {
                throw new MeldingsUtvekslingRuntimeException("Could not get SDP truststore aliases", e);
            }

            NoekkelparOverride noekkelparOverride = new NoekkelparOverride(keyStore, trustStore, config.getKeystore().getAlias(), config.getKeystore().getPassword(), false);
            tekniskAvsender = Databehandler.builder(aktoerOrganisasjonsnummer.forfremTilDatabehandler(), noekkelparOverride).build();
        } else {
            tekniskAvsender = Databehandler.builder(aktoerOrganisasjonsnummer.forfremTilDatabehandler(), Noekkelpar.fraKeyStoreUtenTrustStore(keyStore, config.getKeystore().getAlias(), config.getKeystore().getPassword())).build();
        }
        return new SikkerDigitalPostKlient(tekniskAvsender, klientKonfigurasjon);
    }

    private KlientKonfigurasjon.Builder createKlientKonfigurasjonBuilder() {
        return KlientKonfigurasjon.builder(getMiljo())
                .connectionTimeout(20, TimeUnit.SECONDS);
    }

    private Miljo getMiljo() {
        return new Miljo(null, URI.create(config.getEndpoint()));
    }
}
