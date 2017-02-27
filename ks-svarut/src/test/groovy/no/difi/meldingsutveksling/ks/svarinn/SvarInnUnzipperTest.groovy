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

        def files = new SvarInnUnzipper().unzip(zipFile)

        assert files.size() == 2
        assert files == ["file1.txt": content, "file2.txt": content]
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