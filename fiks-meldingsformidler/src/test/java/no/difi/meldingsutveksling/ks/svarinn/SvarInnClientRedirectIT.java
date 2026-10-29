package no.difi.meldingsutveksling.ks.svarinn;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.io.pipe.Plumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.io.InputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tester at Fiks-credentials ikke sendes videre når SvarInn svarer med redirect til en annen vert.
 * Redirects er eksplisitt slått på, slik at testen dekker det verste tilfellet uavhengig av default-oppsettet.
 */
class SvarInnClientRedirectIT {

    @RegisterExtension
    static WireMockExtension svarInn = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @RegisterExtension
    static WireMockExtension otherHost = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private SvarInnClient client;

    @BeforeEach
    void setUp() {
        IntegrasjonspunktProperties props = new IntegrasjonspunktProperties();
        props.getFiks().getInn()
                .setBaseUrl(svarInn.baseUrl() + "/tjenester/svarinn")
                .setUsername("svarinn-user")
                .setPassword("svarinn-password")
                .setConnectTimeout(5000)
                .setReadTimeout(5000);
        client = new SvarInnClient(new Plumber(new SimpleAsyncTaskExecutor()), props, new RestTemplateBuilder().redirects(HttpRedirects.FOLLOW));
    }

    private Forsendelse forsendelse(String id) {
        return new Forsendelse()
                .setId(id)
                .setDownloadUrl(svarInn.baseUrl() + "/tjenester/svarinn/forsendelse/" + id)
                .setMottaker(new Forsendelse.Mottaker().setOrgnr("910075918"));
    }

    private byte[] download(Forsendelse forsendelse) throws Exception {
        try (InputStream is = client.downloadZipFile(forsendelse, e -> {
        }).getInputStream()) {
            return is.readAllBytes();
        }
    }

    @Test
    void credentialsAreNotSentToRedirectTargetOnOtherHost() throws Exception {
        svarInn.stubFor(get(urlEqualTo("/tjenester/svarinn/forsendelse/1"))
                .willReturn(aResponse().withStatus(302).withHeader("Location", otherHost.baseUrl() + "/steal")));
        otherHost.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody("zip")));

        download(forsendelse("1"));

        svarInn.verify(getRequestedFor(urlEqualTo("/tjenester/svarinn/forsendelse/1")));
        assertThat(otherHost.findAll(anyRequestedFor(anyUrl())))
                .allSatisfy(request -> assertThat(request.containsHeader("Authorization")).isFalse());
    }

    @Test
    void redirectOnSameHostIsFollowed() throws Exception {
        svarInn.stubFor(get(urlEqualTo("/tjenester/svarinn/forsendelse/2"))
                .willReturn(aResponse().withStatus(302).withHeader("Location", svarInn.baseUrl() + "/tjenester/svarinn/fil/2")));
        svarInn.stubFor(get(urlEqualTo("/tjenester/svarinn/fil/2"))
                .willReturn(aResponse().withStatus(200).withBody("zip")));

        assertThat(download(forsendelse("2"))).asString().isEqualTo("zip");
        svarInn.verify(getRequestedFor(urlEqualTo("/tjenester/svarinn/fil/2")));
    }
}
