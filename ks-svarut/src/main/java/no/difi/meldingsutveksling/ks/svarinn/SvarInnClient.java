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
        // TODO property
        final List<Forsendelse> forsendelser = Arrays.asList(restTemplate.getForObject("/mottaker/hentNyeForsendelser", Forsendelse[].class));

        return forsendelser;

    }

    public SvarInnFile downloadFile(String url) {
        final ResponseEntity<byte[]> forEntity = restTemplate.getForEntity(url, byte[].class);

        SvarInnFile svarInnFile = new SvarInnFile("dokumenter.zip", forEntity.getHeaders().getContentType(), forEntity.getBody());
        svarInnFile.setMediaType(forEntity.getHeaders().getContentType());

        return svarInnFile;
    }

    // TODO property
    public void confirmMessage(String forsendelseId) {
        restTemplate.postForLocation("/kvitterMottak/forsendelse/{forsendelseId}", null, forsendelseId);
    }
}
