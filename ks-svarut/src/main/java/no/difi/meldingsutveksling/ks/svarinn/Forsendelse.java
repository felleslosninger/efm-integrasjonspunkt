package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Forsendelse {

    private MetadataForImport metadataForImport;
    private String downloadUrl;
    private List<Map<String, String>> filmetadata;

    @Data
    public static class MetadataForImport {
        private int sakssekvensnummer;
        private int saksaar;
        private String journalposttype;
        private String journalstatus;
        private String dokumentetsDato;
        private String tittel;
    }

    public static class MetaDataFraAvleverendeSystem {

    }
}
