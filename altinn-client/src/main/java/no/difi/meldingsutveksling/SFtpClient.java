package no.difi.meldingsutveksling;

import com.jcraft.jsch.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.lang.NonNull;

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

    public static class ConnectException extends RuntimeException {

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
        private final Session localhost;
        private final Channel channel;

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
                altinnPackage.write(new SFTPResource("test.zip"), null);
                sftp.quit();
            } catch (IOException e) {
                throw new UploadException(e);
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

        public Resource getResource(String filename) {
            return new SFTPResource(filename);
        }

        public class DownloadException extends RuntimeException {
            public DownloadException(Exception e) {
                super(e);
            }
        }

        @RequiredArgsConstructor
        private class SFTPResource extends AbstractResource implements WritableResource {

            private final String filename;

            @NonNull
            @Override
            public OutputStream getOutputStream() throws IOException {
                try {
                    return sftp.put(filename);
                } catch (SftpException e) {
                    throw new UploadException(e);
                }
            }

            @NonNull
            @Override
            public String getDescription() {
                return "SFTPResource";
            }

            @NonNull
            @Override
            public InputStream getInputStream() throws IOException {
                try {
                    return sftp.get(Paths.get(remoteDirectory, filename).toString());
                } catch (SftpException e) {
                    throw new UploadException(e);
                }
            }
        }

    }

}
