package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.kvittering.xsd.Aapning;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.kvittering.xsd.Levering;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

/**
 * Factory clas for Kvittering instances
 *
 * @author Glenn bech
 */
public class KvitteringFactory {

    public static Kvittering createAapningskvittering() {
        Kvittering k = new Kvittering();
        k.setAapning(new Aapning());
        k.setTidspunkt(createTimeStamp());
        return k;
    }

    public static Kvittering createLeveringsKvittering() {
        Kvittering k = new Kvittering();
        k.setLevering(new Levering());
        k.setTidspunkt(createTimeStamp());
        return k;
    }

    private static XMLGregorianCalendar createTimeStamp() {
        try {
            GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance();
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
        } catch (DatatypeConfigurationException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }


}
