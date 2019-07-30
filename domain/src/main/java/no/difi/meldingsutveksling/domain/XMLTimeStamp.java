package no.difi.meldingsutveksling.domain;

import lombok.experimental.UtilityClass;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

/**
 * Util class for creating an XMLGregorianCalendar timestamp
 *
 * @author Glenn Bech
 */
@UtilityClass
public class XMLTimeStamp {

    public static XMLGregorianCalendar createTimeStamp() {
        try {
            GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance();
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
        } catch (DatatypeConfigurationException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}