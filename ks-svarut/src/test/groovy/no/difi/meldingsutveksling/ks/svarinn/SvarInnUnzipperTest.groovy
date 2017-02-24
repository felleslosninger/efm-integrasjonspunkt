package no.difi.meldingsutveksling.ks.svarinn

import org.junit.Before
import org.junit.Test

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream;

public class SvarInnUnzipperTest {

    byte[] content = [1, 2, 3]


    @Before
    void setup() {
    }

    @Test
    void givenSvarInnZipFileWithTwoFilesThenUnzipperShouldReturnTwoSvarInnFiles() {
        def zipFile = createZipFileWithEmptyFiles("file1.txt", "file2.txt")

        SvarInnFile svarInnFile = new SvarInnFile(SvarInnClient.APPLICATION_ZIP, zipFile)

        def files = new SvarInnUnzipper().unzip(svarInnFile)

        assert files.size() == 2
        assert files.contents == Collections.nCopies(2, content)
    }

    def createZipFileWithEmptyFiles(String... filenames) {
        def outStream = new ByteArrayOutputStream()
        ZipOutputStream zipOutputStream = new ZipOutputStream(outStream)

        filenames.each {
            zipOutputStream.putNextEntry(new ZipEntry(it))
            zipOutputStream.write(content)
            zipOutputStream.closeEntry()
        }
        def zipFile = outStream.toByteArray()
        zipOutputStream.finish()
        zipOutputStream.flush()
        zipOutputStream.close()
        return zipFile
    }
}