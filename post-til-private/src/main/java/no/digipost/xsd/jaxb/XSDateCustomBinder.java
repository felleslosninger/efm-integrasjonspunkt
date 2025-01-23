
package no.digipost.xsd.jaxb;

import jakarta.xml.bind.DatatypeConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

public final class XSDateCustomBinder {

	public static LocalDate parseDate(final String value) {
	    if (value == null) {
            return null;
        }
        Calendar parsed = DatatypeConverter.parseDate(value);
        return ZonedDateTime.ofInstant(parsed.toInstant(), parsed.getTimeZone().toZoneId()).toLocalDate();
	}

	public static String printDate(LocalDate date) {
	    if (date == null) {
            return null;
        }
        GregorianCalendar convertedDate = GregorianCalendar.from(ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.systemDefault()));
        return DatatypeConverter.printDate(convertedDate);
	}

	private XSDateCustomBinder() {}

}
