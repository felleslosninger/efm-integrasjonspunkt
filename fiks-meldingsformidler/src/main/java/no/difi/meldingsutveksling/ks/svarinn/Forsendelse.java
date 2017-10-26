package no.difi.meldingsutveksling.ks.svarinn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Forsendelse {
    private String id;
    private MetadataFraAvleverendeSystem metadataFraAvleverendeSystem;
    private MetadataForImport metadataForImport;
    private String downloadUrl;
    private List<Map<String, String>> filmetadata;
    private SvarSendesTil svarSendesTil;
    private Mottaker mottaker;

    @Data
    public static class Mottaker {
        private String orgnr;
        private String navn;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetadataFraAvleverendeSystem {
        private int sakssekvensnummer;
        private int saksaar;
        private String journalaar;
        private String journalsekvensnummer;
        private String journalpostnummer;
        private String journalposttype;
        private String journalstatus;
        private String journaldato;
        private String dokumentetsDato;
        private String tittel;
        private String saksBehandler;
    }

    @Data
    public static class MetadataForImport {
        private int sakssekvensnummer;
        private int saksaar;
        private String journalposttype;
        private String journalstatus;
        private String dokumentetsDato;
        private String tittel;
    }

    @Data
    public static class SvarSendesTil {
        private String adresse1;
        private String postnr;
        private String poststed;
        private String navn;
        private String land;
        private String orgnr;
    }
}
