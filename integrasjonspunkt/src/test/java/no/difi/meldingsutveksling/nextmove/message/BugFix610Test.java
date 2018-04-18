package no.difi.meldingsutveksling.nextmove.message;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.DpoConversationResource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BugFix610Test {

    private final static String FILE_NAME = "bar";

    private IntegrasjonspunktProperties props;
    private DpoConversationResource cr;
    private FileMessagePersister messagePersister;

    @Before
    public void setup() throws IOException {
        props = new IntegrasjonspunktProperties();
        IntegrasjonspunktProperties.NextMove nextMoveProps = new IntegrasjonspunktProperties.NextMove();
        nextMoveProps.setFiledir("target/filepersister_testdir");
        nextMoveProps.setApplyZipHeaderPatch(true);
        nextMoveProps.setAsicfile(FILE_NAME);
        props.setNextmove(nextMoveProps);

        messagePersister = new FileMessagePersister(props);

        cr = DpoConversationResource.of("42", "2", "1");
    }

    @Test
    public void testPatch() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/buggyAsic.asic");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024 * 6];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        messagePersister.write(cr, FILE_NAME, buffer.toByteArray());
        File resultFile = new File(props.getNextmove().getFiledir() + "/" + cr.getConversationId() + "/" + FILE_NAME);

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(resultFile));
        ZipEntry zipEntry = zipInputStream.getNextEntry();

        Assert.assertEquals("mimetype", zipEntry.getName());

        byte[] readBuffer = new byte[ (int) zipEntry.getSize() ];
        zipInputStream.read(readBuffer, 0, readBuffer.length);
        Assert.assertEquals("application/vnd.etsi.asic-e+zip", new String(readBuffer));

        zipInputStream.close();
    }
}
