package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Forsendelse {
    private MetadataFraAvleverendeSystem metadataFraAvleverendeSystem;
    private MetadataForImport metadataForImport;
    private String downloadUrl;
    private List<Map<String, String>> filmetadata;
    private SvarSendesTil svarSendesTil;
    private Mottaker mottaker;

    @Data
    public static class Mottaker {
        private String orgnr;
    }

    @Data
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
        private List<String> ekstraMetadata;
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
        private String orgnr;
    }
}
