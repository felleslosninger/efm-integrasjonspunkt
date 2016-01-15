package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.exception.QueueException;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.FileInputStream;
import java.io.IOException;

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
    public static String getHash(String fileNname) {
        try (FileInputStream fis = new FileInputStream(fileNname)) {
            return DigestUtils.sha1Hex(fis);
        } catch (IOException e) {
            throw new QueueException(e);
        }
    }
}