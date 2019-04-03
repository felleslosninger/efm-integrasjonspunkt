package no.difi.meldingsutveksling.dpi;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.sdp.client2.KlientKonfigurasjon;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.AktoerOrganisasjonsnummer;
import no.difi.sdp.client2.domain.Databehandler;
import no.difi.sdp.client2.domain.Miljo;
import no.difi.sdp.client2.domain.Noekkelpar;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

import java.net.URI;
import java.security.KeyStore;
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
        Databehandler tekniskAvsender = Databehandler.builder(aktoerOrganisasjonsnummer.forfremTilDatabehandler(), Noekkelpar.fraKeyStoreUtenTrustStore(keyStore, config.getKeystore().getAlias(), config.getKeystore().getPassword())).build();
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
