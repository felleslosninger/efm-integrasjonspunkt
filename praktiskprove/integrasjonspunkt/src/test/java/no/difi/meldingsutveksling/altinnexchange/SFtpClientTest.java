package no.difi.meldingsutveksling.altinnexchange;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Ignore("Kept for documentation purposes. To run tests successfully you need to setup a SFTP server running at localhost with a ssh key files")
public class SFtpClientTest {

    private SFtpClient sFtpClient;
    private String sshPrivateKeyFileName;

    @Before
    public void setUp() throws Exception {
        sshPrivateKeyFileName = "test_key";
        sFtpClient = new SFtpClient("localhost");
    }

    @Test
    public void canConnect() throws JSchException {
        SFtpClient.Connection connection = sFtpClient.connect(sshPrivateKeyFileName);

        assertNotNull(connection);
    }

    @Test(expected = SFtpClient.ConnectException.class)
    public void keyFileNotFound() throws JSchException {
        sFtpClient.connect("asdfasdfasdfadsf");
    }

    @Test
    public void canUploadFile() throws JSchException, IOException, SftpException {
        File file = new File("test.txt");
        SFtpClient.Connection connection = sFtpClient.connect(sshPrivateKeyFileName);

        connection.remoteDirectory("temp").upload(file);
    }

    @Test(expected = SFtpClient.Connection.UploadException.class)
    public void cannotUploadFile() {
        File file = new File("adsfasdfasdf.txt");
        SFtpClient.Connection connection = sFtpClient.connect(sshPrivateKeyFileName);

        connection.remoteDirectory("temp").upload(file);
    }

    @Test
    public void canDownloadFile() throws JSchException, SftpException, IOException {
        SFtpClient.Connection connection = sFtpClient.connect(sshPrivateKeyFileName);

        Path file = connection.localDirectory("./").remoteDirectory("temp").download("test.txt");

        assertTrue(file.toFile().exists());
    }

    @Test(expected = SFtpClient.Connection.DownloadException.class)
    public void cannotDownloadFile() {
        SFtpClient.Connection connection = sFtpClient.connect(sshPrivateKeyFileName);

        connection.download("adsfdfsdkjfksjdfkjsd.txt");

        fail();
    }
}