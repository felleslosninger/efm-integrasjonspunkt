package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.exception.QueueException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Creates a SHA1 digest from the contents of a file that is
 * equivalent of doing openssl sha1 <filename> on a command line
 *
 * @author glennbech on 29.12.2015.
 */
class FileSHA1Digest {

    /**
     * @return a Hex encoded represention of the SHA1 hash from the contents of the file identified by the filename
     */
    public static String getHexEncodedSHA1DigestOf(String fileNname) {
        String hexDigest;
        try (FileInputStream fileInput = new FileInputStream(fileNname)) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            DigestInputStream digestInputStream = new DigestInputStream(fileInput, messageDigest);
            // just makes sure we read the stream to EOF
            IOUtils.toByteArray(digestInputStream);
            hexDigest = new String(Hex.encodeHex(messageDigest.digest()));
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new QueueException("Error while creating checksum", e);
        }
        return hexDigest;
    }
}
