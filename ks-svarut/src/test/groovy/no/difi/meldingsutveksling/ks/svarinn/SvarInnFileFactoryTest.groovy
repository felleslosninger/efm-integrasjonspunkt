package no.difi.meldingsutveksling.ks.svarinn;

import org.junit.Test;

public class SvarInnFileFactoryTest {
    String filnavn1 = "text1.txt"
    String filnavn2 = "text2.txt"
    byte[] content = [1, 2, 3, 4]

    @Test
    public void svarInnFileFactoryShouldCreateSvarInnFilesWithCorrectMimetypes() {
        def filmetadata = [[filnavn: filnavn1, mimetype: "application/text"],
                           [filnavn: filnavn2, mimetype: "application/pdf"]]

        Map<String, byte[]> unzipped = "corresponding unzipped files"(filmetadata*.filnavn)

        List<SvarInnFile> files = new SvarInnFileFactory().createFiles(filmetadata, unzipped)


        assert files.size() == 2
        assert files*.mediaType.toString() == filmetadata*.mimetype.toString()
    }

    def "corresponding unzipped files"(List filnavn) {
        Map<String, byte[]> unzipped = [:]
        for (String filename : filnavn) {
            unzipped[filename] = content
        }
        return unzipped
    }
}