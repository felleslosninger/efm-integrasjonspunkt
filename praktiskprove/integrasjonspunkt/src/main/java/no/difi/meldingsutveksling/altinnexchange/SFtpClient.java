package no.difi.meldingsutveksling.altinnexchange;

import com.jcraft.jsch.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SFtpClient {

    private final JSch jSch = new JSch();
    private final String url;

    public SFtpClient(String url) {
        this.url = url;
    }

    /**
     * Attempts to connect via SFTP. The keyFileName is the name of the SSH key that must be located on the classpath.
     *
     * The keyfile is a SSH private key file
     *
     * @param keyFileName filename for the private key
     */
    public Connection connect(String keyFileName) {
        URL resource = this.getClass().getResource("/" + keyFileName);
        if(resource == null) {
            throw new ConnectException(String.format("SSH key file '%s' cannot be found on the classpath", keyFileName));
        }
        String keyfile = resource.getPath();

        System.out.println("--> " + keyfile);

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");

        try {
            jSch.addIdentity(keyfile);
            Session localhost = jSch.getSession(url);
            localhost.setConfig(config);
            localhost.connect();
            Channel channel = localhost.openChannel("sftp");
            channel.connect();
            ChannelSftp sftp = (ChannelSftp) channel;
            return new Connection(sftp);
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

    public class Connection {
        private final ChannelSftp sftp;
        private String remoteDirectory = "";
        private String localDirectory = "";

        public Connection(ChannelSftp sftp) {
            this.sftp = sftp;
        }

        /**
         * Sets the remote directory to download/upload from
         *
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
         *
         * Default ""
         *
         * @param path on localhost
         * @return this
         */
        public Connection localDirectory(String path) {
            localDirectory = path;
            return this;
        }

        public void upload(File file) {
            try(InputStream inputStream = Files.newInputStream(file.toPath())) {
                sftp.put(inputStream, Paths.get(remoteDirectory, file.getName()).toString());
            } catch(SftpException | IOException exception) {
                throw new UploadException(exception);
            }
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
            try(OutputStream outputStream = Files.newOutputStream(path)) {
                sftp.get(Paths.get(remoteDirectory, fileName).toString(), outputStream);
            } catch (SftpException | IOException e) {
                throw new DownloadException(e);
            }
            return path;
        }

        public class DownloadException extends RuntimeException {
            public DownloadException(Exception e) {
                super(e);
            }
        }
    }
}
