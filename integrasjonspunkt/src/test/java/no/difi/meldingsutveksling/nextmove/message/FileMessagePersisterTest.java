package no.difi.meldingsutveksling.nextmove.message;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.DpoConversationResource;
import no.difi.meldingsutveksling.nextmove.Receiver;
import no.difi.meldingsutveksling.nextmove.Sender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FileMessagePersisterTest {

    private IntegrasjonspunktProperties props;
    private DpoConversationResource cr;
    private FileMessagePersister messagePersister;

    @Before
    public void setup() throws IOException {
        props = new IntegrasjonspunktProperties();
        IntegrasjonspunktProperties.NextMove nextMoveProps = new IntegrasjonspunktProperties.NextMove();
        nextMoveProps.setFiledir("target/filepersister_testdir");
        props.setNextmove(nextMoveProps);

        messagePersister = new FileMessagePersister(props);

        cr = DpoConversationResource.of("42", Sender.of("2", "bar"), Receiver.of("1", "foo"));
    }

    @Test
    public void testFileMessagePersister() throws Exception {
        String filename = "foo";
        byte[] content = "bar".getBytes(UTF_8);

        messagePersister.write(cr, filename, content);

        byte[] read = messagePersister.read(cr, filename);
        Assert.assertArrayEquals(content, read);

        messagePersister.delete(cr);
        File crDir = new File(props.getNextmove().getFiledir() + "/" + cr.getConversationId());
        Assert.assertFalse(crDir.exists());
    }


}