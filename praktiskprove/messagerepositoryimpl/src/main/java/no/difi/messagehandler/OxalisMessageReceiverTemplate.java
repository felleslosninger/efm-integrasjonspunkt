package no.difi.messagehandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: glennbech
 * Date: 10.11.14
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */
public class OxalisMessageReceiverTemplate extends MessageReceieverTemplate {

    private List<Object> removeAfterImpl= new ArrayList<Object>();
    @Override
    void sendLeveringskvittering() {
        removeAfterImpl.add("This will be removed after implementation of this method,thnx to Sonar");
    }

    @Override
    void sendApningskvittering() {
        removeAfterImpl.add("This will be removed after implementation of this method,thnx to Sonar");
    }

}
