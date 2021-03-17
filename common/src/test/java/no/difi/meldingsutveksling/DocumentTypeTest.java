package no.difi.meldingsutveksling;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class DocumentTypeTest {

    @Test
    public void valueOfDocumentTypeTest() {
        Optional<DocumentType> arkivmeldingDoctype = DocumentType.valueOfDocumentType("urn:no:difi:arkivmelding:xsd::arkivmelding");
        Assert.assertTrue(arkivmeldingDoctype.isPresent());
        Assert.assertEquals(DocumentType.ARKIVMELDING, arkivmeldingDoctype.get());

        Optional<DocumentType> innsynskravDoctype = DocumentType.valueOfDocumentType("urn:no:difi:einnsyn:xsd::innsynskrav");
        Assert.assertTrue(innsynskravDoctype.isPresent());
        Assert.assertEquals(DocumentType.INNSYNSKRAV, innsynskravDoctype.get());
    }

}

