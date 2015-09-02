package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.asic.SignatureHelper;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.meldingsutveksling.dokumentpakking.domain.Signature;
import no.difi.meldingsutveksling.domain.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CreateAsiceTest {

    @Mock
    private ManifestFactory createManifest;

    @InjectMocks
    private CreateAsice createAsice;

    @Mock
    private Mottaker mottaker;

    @Mock
    private Avsender avsender;


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
    private BestEduMessage hoveddokument;

    @Test
    public void testCreateAsice() throws Exception {

        manifest = new Manifest("<manifest></manifest>".getBytes());
        hoveddokument = new BestEduMessage("<best></best>".getBytes());

        when(createManifest.createManifest(
                any(Organisasjonsnummer.class),
                any(Organisasjonsnummer.class),
                any(ByteArrayFile.class))).thenReturn(manifest);
        when(mottaker.getOrgNummer()).thenReturn(orgNrMottaker);
        when(avsender.getOrgNummer()).thenReturn(orgNrAvsender);
        when(avsender.getNoekkelpar()).thenReturn(noekkelpar);

        SignatureHelper helper = new SignatureHelper(getClass().getResourceAsStream("/testkeystore.jks"), "changeit", "mykey", "changeit");
        Archive returnedArchive = createAsice.createAsice(hoveddokument, helper, avsender, mottaker);
        verify(createManifest, times(1)).createManifest(orgNrCaptor1.capture(), orgNrCaptor2.capture(), fileCaptor.capture());

    }

}
