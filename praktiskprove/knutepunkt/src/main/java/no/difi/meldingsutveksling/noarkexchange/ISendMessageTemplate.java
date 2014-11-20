package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

/**
 * Created with IntelliJ IDEA.
 * User: glennbech
 * Date: 20.11.14
 * Time: 13:22
 * To change this template use File | Settings | File Templates.
 */
public interface ISendMessageTemplate {
    PutMessageResponseType sendMessage(PutMessageRequestType message);
}
