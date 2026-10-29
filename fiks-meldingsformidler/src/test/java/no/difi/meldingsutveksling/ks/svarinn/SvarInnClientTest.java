package no.difi.meldingsutveksling.ks.svarinn;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.Reject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.restclient.RestTemplateBuilder;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class SvarInnClientTest {

    private static final String BASE_URL = "https://svarut.ks.no/tjenester/svarinn";

    private Plumber plumber;
    private SvarInnClient client;

    @BeforeEach
    void setUp() {
        IntegrasjonspunktProperties props = new IntegrasjonspunktProperties();
        props.getFiks().getInn()
                .setBaseUrl(BASE_URL)
                .setConnectTimeout(1000)
                .setReadTimeout(1000);
        plumber = mock(Plumber.class);
        client = new SvarInnClient(plumber, props, new RestTemplateBuilder());
    }

    private static Forsendelse forsendelse(String downloadUrl) {
        return new Forsendelse().setId("ae68b33d").setDownloadUrl(downloadUrl);
    }

    @Test
    void absoluteDownloadUrlUnderBaseUrlIsAccepted() {
        String url = BASE_URL + "/forsendelse/9b45ab9e-e13a-489e-832d-56e836c9a8bc";
        assertThat(client.toDownloadUri(forsendelse(url))).isEqualTo(URI.create(url));
    }

    @Test
    void encodedCharactersThatAreNotDotSegmentsAreAccepted() {
        String url = BASE_URL + "/forsendelse/fil%2Ezip?navn=a%20b";
        assertThat(client.toDownloadUri(forsendelse(url))).isEqualTo(URI.create(url));
    }

    @Test
    void relativeDownloadUrlIsResolvedAgainstBaseUrl() {
        assertThat(client.toDownloadUri(forsendelse("/mottaker/forsendelse/81264cfa")))
                .isEqualTo(URI.create(BASE_URL + "/mottaker/forsendelse/81264cfa"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.example.com/tjenester/svarinn/forsendelse/1",
            "http://svarut.ks.no/tjenester/svarinn/forsendelse/1",
            "https://svarut.ks.no:8443/tjenester/svarinn/forsendelse/1",
            "https://svarut.ks.no.evil.example.com/tjenester/svarinn/forsendelse/1",
            "https://svarut.ks.no@evil.example.com/tjenester/svarinn/forsendelse/1",
            "https://user@svarut.ks.no/tjenester/svarinn/forsendelse/1",
            "https://svarut.ks.no/tjenester/svarinn/../admin",
            "https://svarut.ks.no/tjenester/svarinn/%2e%2e/admin",
            "https://svarut.ks.no/tjenester/svarinn/%2E%2E/admin",
            "https://svarut.ks.no/tjenester/svarinn/.%2e/admin",
            "https://svarut.ks.no/tjenester/svarinn/%2e%2e%2fadmin",
            "https://svarut.ks.no/tjenester/svarinn/%2e/forsendelse/1",
            "https://svarut.ks.no/tjenester/svarinn/%5c..%5cadmin",
            "https://svarut.ks.no/tjenester/svarinn%2fadmin",
            "/mottaker/%2e%2e/%2e%2e/admin",
            "https://svarut.ks.no/tjenester/svarinnX/forsendelse/1",
            "https://svarut.ks.no/annet",
            "//evil.example.com/forsendelse/1",
            "forsendelse/1",
            "file:///etc/passwd",
            "https://svarut.ks.no/tjenester/svarinn/ forsendelse"
    })
    void downloadUrlOutsideBaseUrlIsRejected(String url) {
        assertThatThrownBy(() -> client.toDownloadUri(forsendelse(url)))
                .isInstanceOf(SvarInnForsendelseException.class)
                .hasMessageContaining("ae68b33d");
    }

    @Test
    void missingDownloadUrlIsRejected() {
        assertThatThrownBy(() -> client.toDownloadUri(forsendelse(null)))
                .isInstanceOf(SvarInnForsendelseException.class);
    }

    @Test
    void downloadZipFileDoesNotStartDownloadForInvalidUrl() {
        Forsendelse forsendelse = forsendelse("https://evil.example.com/forsendelse/1");
        Reject reject = mock(Reject.class);

        assertThatThrownBy(() -> client.downloadZipFile(forsendelse, reject))
                .isInstanceOf(SvarInnForsendelseException.class);
        verifyNoInteractions(plumber);
    }
}
