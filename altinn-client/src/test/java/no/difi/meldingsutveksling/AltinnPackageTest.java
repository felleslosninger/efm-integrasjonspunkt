package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.move.common.io.WritableByteArrayResource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class AltinnPackageTest {

    @Test
    void testFrom() throws Exception {

        UploadRequest uploadRequest = new MockRequest();
        AltinnPackage altinnPackage = AltinnPackage.from(uploadRequest);
        WritableByteArrayResource output = new WritableByteArrayResource();
        altinnPackage.write(output, null);

        byte[] bytes = output.toByteArray();

        AltinnPackage.from(new ByteArrayResource(bytes));
    }

    @Test
    void testWrite() throws Exception {

    }

    @Test
    void testFrom1() throws Exception {

    }
}