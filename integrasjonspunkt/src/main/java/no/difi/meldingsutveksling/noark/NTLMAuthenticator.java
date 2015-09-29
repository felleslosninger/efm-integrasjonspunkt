
package no.difi.meldingsutveksling.noark;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Custom HTTP Authentocator that can be used against an NTLM security enabled WebService
 * the username must be in the format domain\\username (please note the escaping \)
 * @author Glenn Bech
 */
public class NTLMAuthenticator extends Authenticator {

    private String userName, password;

    public NTLMAuthenticator(String username, String password) {
        this.userName = username;
        this.password = password;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {

        if (password == null) {
            throw new MeldingsUtvekslingRuntimeException();
        }
       return new PasswordAuthentication(userName , password.toCharArray() );
    }
}
