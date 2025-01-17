package no.difi.meldingsutveksling.manifest.xml;

import no.difi.meldingsutveksling.domain.Iso6523;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;


class MarshalManifestTest {

    @Test
    void testMarshalling() throws JAXBException {
        Manifest original = new Manifest(
                new Mottaker(new Organisasjon(Iso6523.parse("0192:12345678"))),
                new Avsender(new Organisasjon(Iso6523.parse("0192:12345678"))),
                new HovedDokument("lol.pdf", "application/pdf", "Hoveddokument", "no"));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MarshalManifest.marshal(original, os);

        InputStream is = new ByteArrayInputStream(os.toByteArray());

        JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Manifest.class}, null);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        Manifest kopi = (Manifest) jaxbUnmarshaller.unmarshal(is);
        assertEquals(kopi.getHoveddokument().getTittel().getTittel(), original.getHoveddokument().getTittel().getTittel());
        assertEquals(kopi.getAvsender().getOrganisasjon().getOrgNummer(), original.getAvsender().getOrganisasjon().getOrgNummer());
    }
}
