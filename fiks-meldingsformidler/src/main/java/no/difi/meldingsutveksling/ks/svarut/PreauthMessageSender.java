package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Base64;

@RequiredArgsConstructor
public class PreauthMessageSender extends HttpUrlConnectionMessageSender {

    private final String user;
    private final String pass;

    @Override
    @SuppressWarnings("squid:S2647")
    protected void prepareConnection(HttpURLConnection connection) throws IOException {
        String userAndPass = user + ":" + pass;
        byte[] encode = Base64.getEncoder().encode(userAndPass.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + new String(encode));

        super.prepareConnection(connection);
    }
}
