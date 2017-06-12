package no.difi.meldingsutveksling.ks.svarinn;

import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SvarInnFileFactory {

    /**
     * Combines decompressed zip file with metadata to create SvarInnFiles
     * @param filmetadata metadata for the files
     * @param unzippedFile the unzipped content from SvarInn
     * @return list of SvarInnFile
     */
    public List<SvarInnFile> createFiles(List<Map<String, String>> filmetadata, Map<String, byte[]> unzippedFile) {
        final ArrayList<SvarInnFile> files = new ArrayList<>();
        for (Map<String, String> metadata : filmetadata) {
            MediaType mediaType = MediaType.parseMediaType(metadata.get("mimetype"));

            final String filnavn = metadata.get("filnavn");
            files.add(new SvarInnFile(filnavn, mediaType, unzippedFile.get(filnavn)));
        }
        return files;
    }
}
