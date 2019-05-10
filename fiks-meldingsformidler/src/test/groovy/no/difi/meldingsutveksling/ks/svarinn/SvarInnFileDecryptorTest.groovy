package no.difi.meldingsutveksling.ks.svarinn

import no.difi.meldingsutveksling.config.FiksConfig
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.config.KeyStoreProperties
import org.junit.Test
import org.mockito.Mockito
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource

import java.util.zip.ZipInputStream

import static org.mockito.Mockito.when

public class SvarInnFileDecryptorTest {

    @Test
    public void shouldBeAbleToDecryptGivenBytesFromSvarInn() {
        final Resource path = new UrlResource(getClass().getResource("/somdalen.jks"));
        final KeyStoreProperties keystore = new KeyStoreProperties(path: path, password: "changeit", alias: "somdalen");
        def bytes = getClass().getResource("/somdalen-dokumenter-ae68b33d.zip").bytes
        def props = Mockito.mock(IntegrasjonspunktProperties)
        def fiksMock = Mockito.mock(FiksConfig)
        when(fiksMock.getKeystore()).thenReturn(keystore)
        when(props.getFiks()).thenReturn(fiksMock)

        SvarInnFileDecryptor decryptor = new SvarInnFileDecryptor(props)
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