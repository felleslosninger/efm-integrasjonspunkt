package no.difi.meldingsutveksling.ks.svarinn;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

public class SvarInnClient {
    public static final MediaType APPLICATION_ZIP = MediaType.parseMediaType("application/zip;charset=UTF-8");
    private RestTemplate restTemplate;

    public SvarInnClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Forsendelse> checkForNewMessages() {
        final List<Forsendelse> forsendelser = Arrays.asList(restTemplate.getForObject("https://test.svarut.ks.no/tjenester/svarinn/mottaker/hentNyeForsendelser", Forsendelse[].class));

        return forsendelser;

    }

    public SvarInnFile downloadFile(String url) {
        final ResponseEntity<byte[]> forEntity = restTemplate.getForEntity(url, byte[].class);

        System.out.println("Statuscode: " + forEntity.getStatusCode());
        System.out.println("Content type: " + forEntity.getHeaders().getContentType());

        SvarInnFile svarInnFile = new SvarInnFile(forEntity.getHeaders().getContentType(), forEntity.getBody());

        return svarInnFile;
    }


}
