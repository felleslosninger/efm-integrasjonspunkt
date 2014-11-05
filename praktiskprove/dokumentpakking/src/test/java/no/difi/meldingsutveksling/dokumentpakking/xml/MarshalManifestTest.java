package no.difi.meldingsutveksling.dokumentpakking.xml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import no.difi.meldingsutveksling.dokumentpakking.domain.Organisasjonsnummer;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;
public class MarshalManifestTest {
	@Test
	public void testMarshalling() throws JAXBException {

		Manifest original = new Manifest(new Mottaker(new Organisasjon(new Organisasjonsnummer("12345678"))), new Avsender(new Organisasjon(
				new Organisasjonsnummer("12345678"))), new HovedDokument("lol.pdf", "application/pdf", "Hoveddokument", "no"));

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalManifest.marshal(original, os);

		InputStream is = new ByteArrayInputStream(os.toByteArray());

		JAXBContext jaxbContext = JAXBContext.newInstance(Manifest.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Manifest kopi = (Manifest) jaxbUnmarshaller.unmarshal(is);
		assertThat(kopi.hoveddokument.tittel.tittel, is(original.hoveddokument.tittel.tittel));
		assertThat(kopi.avsender.organisasjon.orgNummer, is(original.avsender.organisasjon.orgNummer));

	}
}
