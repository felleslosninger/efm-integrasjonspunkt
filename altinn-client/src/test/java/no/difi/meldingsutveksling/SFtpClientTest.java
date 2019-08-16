package no.difi.meldingsutveksling;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@Ignore("Kept for documentation purposes. To run tests successfully you need to setup a SFTP server running at localhost with a ssh key files")
public class SFtpClientTest {

    private SFtpClient sFtpClient;
    private String sshPrivateKeyFileName;

    @Before
    public void setUp() {
        sshPrivateKeyFileName = "test_key";
        sFtpClient = new SFtpClient("localhost");
    }

    @Test
    public void canConnect() {
        SFtpClient.Connection connection = sFtpClient.connect(sshPrivateKeyFileName);


        assertThat(connection).isNotNull();
    }

    @Test(expected = SFtpClient.ConnectException.class)
    public void keyFileNotFound() {
        sFtpClient.connect("asdfasdfasdfadsf");
    }

    @Test
    public void canUploadFile() {
        File file = new File("test.txt");
        SFtpClient.Connection connection = sFtpClient.connect(sshPrivateKeyFileName);

        connection.remoteDirectory("temp").upload(file);
    }

    @Test(expected = SFtpClient.Connection.UploadException.class)
    public void cannotUploadFile() {
        File file = new File("adsfasdfasdf.txt");

        try (SFtpClient.Connection connection = sFtpClient.connect(sshPrivateKeyFileName)) {
            connection.remoteDirectory("temp").upload(file);
        }
    }

    @Test
    public void canDownloadFile() {
        SFtpClient.Connection connection = sFtpClient.connect(sshPrivateKeyFileName);

        Path file = connection.localDirectory("./").remoteDirectory("temp").download("test.txt");

        assertThat(file.toFile().exists()).isTrue();
    }

    @Test(expected = SFtpClient.Connection.DownloadException.class)
    public void cannotDownloadFile() {
        SFtpClient.Connection connection = sFtpClient.connect(sshPrivateKeyFileName);

        connection.download("adsfdfsdkjfksjdfkjsd.txt");

        fail();
    }
}