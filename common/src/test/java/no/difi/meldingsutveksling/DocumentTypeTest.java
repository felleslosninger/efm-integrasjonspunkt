package no.difi.meldingsutveksling;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentTypeTest {

    @Test
    public void testValuesByApi() {
        assertThat(DocumentType.values(ApiType.NEXTMOVE)).containsOnly(
                DocumentType.ARKIVMELDING,
                DocumentType.ARKIVMELDING_KVITTERING,
                DocumentType.DIGITAL,
                DocumentType.PRINT,
                DocumentType.INNSYNSKRAV,
                DocumentType.PUBLISERING,
                DocumentType.EINNSYN_KVITTERING
        );
    }
}

