package no.difi.meldingsutveksling.dokumentpakking.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.meldingsutveksling.dokumentpakking.domain.Signature;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateAsiceTest {
	@Mock
	private CreateManifest createManifest;

	@Mock
	private CreateSignature createSignature;

	@Mock
	private CreateZip createZip;

	@InjectMocks
	private CreateAsice createAsice;

	@Mock
	private Mottaker mottaker;

	@Mock
	private Avsender avsender;

	@Mock
	private ByteArrayFile hoveddokument;

	@Mock
	private Manifest manifest;

	@Mock
	private Signature signature;

	@Mock
	private Archive archive;

	@Mock
	private Noekkelpar noekkelpar;

	@Mock
	private Organisasjonsnummer orgNrAvsender, orgNrMottaker;

	@Captor
	private ArgumentCaptor<List<ByteArrayFile>> zipItCaptor, createSignatureFileCaptor;

	@Captor
	private ArgumentCaptor<Noekkelpar> noekkelparCaptor;

	@Captor
	private ArgumentCaptor<Organisasjonsnummer> orgNrCaptor1, orgNrCaptor2;

	@Captor
	private ArgumentCaptor<ByteArrayFile> fileCaptor;

	@Test
	public void testCreateAsice() throws Exception {

		when(createManifest.createManifest(any(Organisasjonsnummer.class), any(Organisasjonsnummer.class), any(ByteArrayFile.class))).thenReturn(manifest);
		when(createSignature.createSignature(any(Noekkelpar.class), anyListOf(ByteArrayFile.class))).thenReturn(signature);
		when(createZip.zipIt(anyListOf(ByteArrayFile.class))).thenReturn(archive);
		when(mottaker.getOrgNummer()).thenReturn(orgNrMottaker);
		when(avsender.getOrgNummer()).thenReturn(orgNrAvsender);
		when(avsender.getNoekkelpar()).thenReturn(noekkelpar);

		Archive returnedArchive = createAsice.createAsice(hoveddokument, avsender, mottaker);

		verify(createZip, times(1)).zipIt(zipItCaptor.capture());
		verify(createSignature, times(1)).createSignature(noekkelparCaptor.capture(), createSignatureFileCaptor.capture());
		verify(createManifest, times(1)).createManifest(orgNrCaptor1.capture(), orgNrCaptor2.capture(), fileCaptor.capture());

		assertThat(zipItCaptor.getValue(), contains(hoveddokument, manifest, signature));
		assertThat(createSignatureFileCaptor.getValue(), contains(hoveddokument, manifest));
		assertThat(noekkelparCaptor.getValue(), is(sameInstance(noekkelpar)));
		assertThat(returnedArchive, is(sameInstance(archive)));
		assertThat(orgNrCaptor1.getValue(), is(sameInstance(orgNrAvsender)));
		assertThat(orgNrCaptor2.getValue(), is(sameInstance(orgNrMottaker)));

	}

}
