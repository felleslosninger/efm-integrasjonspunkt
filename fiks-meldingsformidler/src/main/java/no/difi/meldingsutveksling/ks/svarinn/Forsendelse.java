package no.difi.meldingsutveksling.ks.svarinn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Forsendelse {
    private String id;
    private MetadataFraAvleverendeSystem metadataFraAvleverendeSystem;
    private MetadataForImport metadataForImport;
    private String downloadUrl;
    private List<Filmetadata> filmetadata;
    private SvarSendesTil svarSendesTil;
    private String svarPaForsendelse;
    private Mottaker mottaker;
    private String tittel;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Filmetadata {
        private String filnavn;
        private String mimetype;
        private String sha256hash;
        private String dokumentType;
        private Long size;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetadataForImport {
        private int sakssekvensnummer;
        private int saksaar;
        private String journalposttype;
        private String journalstatus;
        private String dokumentetsDato;
        private String tittel;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SvarSendesTil {
        private String adresse1;
        private String postnr;
        private String poststed;
        private String navn;
        private String land;
        private String orgnr;
        private String fnr;
    }
}
