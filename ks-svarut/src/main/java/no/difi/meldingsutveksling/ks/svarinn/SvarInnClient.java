package no.difi.meldingsutveksling.ks.svarinn;

import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

public class SvarInnClient {
    private RestTemplate restTemplate;

    public SvarInnClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Forsendelse> checkForNewMessages() {
        final List<Forsendelse> forsendelser = Arrays.asList(restTemplate.getForObject("https://test.svarut.ks.no/tjenester/svarinn/mottaker/hentNyeForsendelser", Forsendelse[].class));
//        final String forsendelser = restTemplate.getForObject("https://test.svarut.ks.no/tjenester/svarinn/mottaker/hentNyeForsendelser", String.class);

        return forsendelser;

    }

    public void receive() {

    }


}
