package no.difi.meldingsutveksling;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class SFtpClient {

    private final JSch jSch = new JSch();
    private final String url;

    public SFtpClient(String url) {
        this.url = url;
    }

    /**
     * Attempts to connect via SFTP. The keyFileName is the name of the SSH key that must be located on the classpath.
     *
     * @param keyFileName filename for the private key
     */
    public Connection connect(String keyFileName) {
        URL resource = this.getClass().getResource("/" + keyFileName);
        if (resource == null) {
            throw new ConnectException(String.format("SSH key file '%s' cannot be found on the classpath", keyFileName));
        }
        String keyfile = resource.getPath();

        log.info("--> " + keyfile);


        try {
            jSch.addIdentity(keyfile);

            return new Connection(jSch);
        } catch (JSchException e) {
            throw new ConnectException(e);
        }
    }

    public class ConnectException extends RuntimeException {

        public ConnectException(String message) {
            super(message);
        }

        public ConnectException(JSchException e) {
            super(e);
        }
    }

    public class Connection implements AutoCloseable {
        private final ChannelSftp sftp;
        private String remoteDirectory = "";
        private String localDirectory = "";
        private Session localhost;
        private Channel channel;

        public Connection(JSch jsch) throws JSchException {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            localhost = jsch.getSession(url);
            localhost.setConfig(config);
            localhost.connect();
            channel = localhost.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;
        }

        /**
         * Sets the remote directory to download/upload from
         * <p>
         * Default ""
         *
         * @param path on remote host
         * @return this
         */
        public Connection remoteDirectory(String path) {
            remoteDirectory = path;
            return this;
        }

        /**
         * Sets the local directory to download/upload from
         * <p>
         * Default ""
         *
         * @param path on localhost
         * @return this
         */
        public Connection localDirectory(String path) {
            localDirectory = path;
            return this;
        }

        public void upload(AltinnPackage altinnPackage) {
            try {
                OutputStream outputStream = sftp.put("test.zip"); // filename generator
                altinnPackage.write(outputStream, null);
                sftp.quit();
            } catch (SftpException | IOException e) {
                log.error("FTP put failed", e);
            }
        }

        public void upload(File file) {
            try (InputStream inputStream = Files.newInputStream(file.toPath())) {
                sftp.put(inputStream, Paths.get(remoteDirectory, file.getName()).toString());
            } catch (SftpException | IOException exception) {
                throw new UploadException(exception);
            }
        }

        @Override
        public void close() {
            channel.disconnect();
            localhost.disconnect();
        }

        public class UploadException extends RuntimeException {
            public UploadException(Exception e) {
                super(e);
            }
        }

        public Path download(String fileName) {
            Path path = Paths.get(fileName);
            try {
                Files.createDirectories(Paths.get(localDirectory));
            } catch (IOException e) {
                throw new DownloadException(e);
            }
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                sftp.get(Paths.get(remoteDirectory, fileName).toString(), outputStream);
            } catch (SftpException | IOException e) {
                throw new DownloadException(e);
            }
            return path;
        }

        public InputStream getInputStream(String fileName) {
            try {
                return sftp.get(Paths.get(remoteDirectory, fileName).toString());
            } catch (SftpException e) {
                throw new UploadException(e);
            }
        }

        public class DownloadException extends RuntimeException {
            public DownloadException(Exception e) {
                super(e);
            }
        }
    }
}
