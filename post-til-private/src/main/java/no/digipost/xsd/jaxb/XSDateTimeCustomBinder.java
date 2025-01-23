
package no.digipost.xsd.jaxb;

import jakarta.xml.bind.DatatypeConverter;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

public final class XSDateTimeCustomBinder {

	public static ZonedDateTime parseDateTime(String s) {
	    if (s == null) {
            return null;
        }
        Calendar parsed = DatatypeConverter.parseDate(s);
        return ZonedDateTime.ofInstant(parsed.toInstant(), parsed.getTimeZone().toZoneId());
	}

	public static String printDateTime(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return DatatypeConverter.printDateTime(GregorianCalendar.from(dateTime));
	}

	private XSDateTimeCustomBinder() {}

}
