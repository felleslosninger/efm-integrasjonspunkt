package no.difi.meldingsutveksling.ks.svarut;

import lombok.Data;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Base64;

@Data
public class PreauthMessageSender extends HttpUrlConnectionMessageSender {

    private String user;
    private String pass;

    @Override
    protected void prepareConnection(HttpURLConnection connection) throws IOException {
        String userAndPass = user+":"+pass;
        byte[] encode = Base64.getEncoder().encode(userAndPass.getBytes());
        connection.setRequestProperty("Authorization", "Basic "+new String(encode));

        super.prepareConnection(connection);
    }
}
