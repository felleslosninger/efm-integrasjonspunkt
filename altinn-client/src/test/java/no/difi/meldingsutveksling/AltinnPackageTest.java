package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class AltinnPackageTest {

    @Test
    public void testFrom() throws Exception {

        UploadRequest uploadRequest = new MockRequest();
        AltinnPackage altinnPackage = AltinnPackage.from(uploadRequest);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        altinnPackage.write(outputStream, null);

        byte[] bytes = outputStream.toByteArray();


        AltinnPackage.from(new ByteArrayInputStream(bytes));
    }

    @Test
    public void testWrite() throws Exception {

    }

    @Test
    public void testFrom1() throws Exception {

    }
}