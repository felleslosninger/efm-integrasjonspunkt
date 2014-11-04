package no.difi.meldingsutveksling.dokumentpakking.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class CreateZipTest {

	CreateZip createZip = new CreateZip();

	@Test
	public void testZipIt() throws Exception {

		List<AsicEAttachable> files = new ArrayList<>();

		files.add(new AsicEAttachable() {
			@Override
			public String getMimeType() {
				return "text/xml";
			}

			@Override
			public String getFileName() {
				return "xml.xml";
			}

			@Override
			public byte[] getBytes() {
				return "<root><body></body></root>".getBytes();
			}
		});

		Archive zip = createZip.zipIt(files);

		ZipArchiveInputStream zis = new ZipArchiveInputStream(new ByteArrayInputStream(zip.getBytes()));
		zis.getNextEntry();

		assertThat(IOUtils.toByteArray(zis), is(equalTo(files.get(0).getBytes())));

	}

}
