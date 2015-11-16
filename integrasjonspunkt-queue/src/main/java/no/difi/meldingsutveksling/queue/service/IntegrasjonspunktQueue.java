package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.Rule;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

public class IntegrasjonspunktQueue {
    private final Rule rule;
    private final PrivateKey privateKey;

    public IntegrasjonspunktQueue(Rule rule, PrivateKey privateKey) {
        this.rule = rule;
        this.privateKey = privateKey;
    }

    /***
     * Used for existing messages when trying to resend failed messages or remove messages from queue that is sent.
     *
     * @param key Unique key for request to update
     */
    public void put(String key, int i) {
        //Krypter
        //Valider om ny melding
        //Lagre melding
        //Oppdater status
    }

    /***
     * Get messages with a certain status.
     *
     * @param statusToGet Type messages to check for in queue
     */
    public void get(Status statusToGet) {
        //Oppdater status (aktiv melding)
        //Dekrypter
        //returner melding
    }

    /***
     * Used for new messages that is to be put on queue.
     *
     * @param request Request to be put on queue
     */
    public byte[] put(Object request)  {
        try {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            ObjectOutput output = new ObjectOutputStream(byteOutputStream);
            output.writeObject(request);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(byteOutputStream.toByteArray());
        } catch (NoSuchAlgorithmException | InvalidKeyException
                | NoSuchPaddingException | BadPaddingException
                | IllegalBlockSizeException | IOException e) {
            //TODO: Better logging
            e.printStackTrace();
        }

        //Ny melding
        //Krypter
        //Lagre request til disk
        //Lagre status i database
        return new byte[0];
    }
}
