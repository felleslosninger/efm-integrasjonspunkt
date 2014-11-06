package no.difi.meldingsutveksling.adresseregmock;

import java.security.PublicKey;

/**
 * Created with IntelliJ IDEA.
 * User: glennbech
 * Date: 03.11.14
 * Time: 16:16
 * To change this template use File | Settings | File Templates.
 */
public interface AddressRegister {

    PublicKey getPublicKey(String orgNumber);

    Object getCertificate(String s) ;


}
