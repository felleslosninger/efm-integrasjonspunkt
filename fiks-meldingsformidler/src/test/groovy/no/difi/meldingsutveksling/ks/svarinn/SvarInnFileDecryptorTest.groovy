package no.difi.meldingsutveksling.ks.svarinn

import no.difi.meldingsutveksling.Decryptor
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel
import no.difi.meldingsutveksling.config.KeyStoreProperties
import org.junit.Test
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource

import java.util.zip.ZipInputStream

public class SvarInnFileDecryptorTest {
    @Test
    public void shouldBeAbleToDecryptGivenBytesFromSvarInn() {
        final Resource path = new UrlResource(getClass().getResource("/somdalen.jks"));
        final KeyStoreProperties keystore = new KeyStoreProperties(path: path, password: "changeit", alias: "somdalen");
        def bytes = getClass().getResource("/somdalen-dokumenter-ae68b33d.zip").bytes

        SvarInnFileDecryptor decryptor = new SvarInnFileDecryptor(new Decryptor(new IntegrasjonspunktNokkel(keystore)))
        def decrypt = decryptor.decrypt(bytes)

        def expected = getClass().getResource("/decrypted-dokumenter-ae68b33d.zip").bytes

        assert decrypt.length == expected.length

        getFilenamesFromZip(decrypt).sort() == getFilenamesFromZip(expected).sort()

    }

    def getFilenamesFromZip(byte[] file) {
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(file))

        def filenames = []

        def entry
        while ((entry = zipInputStream.nextEntry) != null) {
            filenames.add(entry.name)
        }

        return filenames
    }
}