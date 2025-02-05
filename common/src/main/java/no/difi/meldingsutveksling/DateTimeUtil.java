package no.difi.meldingsutveksling;

import com.google.common.base.Strings;
import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.*;
import java.util.GregorianCalendar;
import java.util.TimeZone;

@UtilityClass
public class DateTimeUtil {

    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Oslo");
    public static final ZoneId DEFAULT_ZONE_ID = DEFAULT_TIME_ZONE.toZoneId();

    public static OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        return OffsetDateTime.ofInstant(localDateTime.atZone(DEFAULT_ZONE_ID).toInstant(), DEFAULT_ZONE_ID);
    }

    public static XMLGregorianCalendar atStartOfDay(XMLGregorianCalendar in) {
        if (in == null) {
            return null;
        }

        XMLGregorianCalendar out = (XMLGregorianCalendar) in.clone();
        out.setTime(0, 0, 0, null);
        return out;
    }

    public static XMLGregorianCalendar toXMLGregorianCalendar(String in) {
        if (Strings.isNullOrEmpty(in)) {
            return null;
        }

        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(in);
        } catch (DatatypeConfigurationException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not convert '%s' to XMLGregorianCalendar".formatted(in), e);
        }
    }

    public static XMLGregorianCalendar toXMLGregorianCalendar(long millis) {
        if (millis < BugfixXsdYearZeroOrBefore.REPLACEMENT_MILLIS) {
            return toXMLGregorianCalendar(toZonedDateTime(BugfixXsdYearZeroOrBefore.REPLACEMENT_MILLIS));
        }
        return toXMLGregorianCalendar(toZonedDateTime(millis));
    }

    private static class BugfixXsdYearZeroOrBefore {
        public static long REPLACEMENT_MILLIS = -62135596800000L;
    }

    private static ZonedDateTime toZonedDateTime(long millis) {
        return Instant.ofEpochMilli(millis).atZone(DEFAULT_ZONE_ID);
    }

    public static XMLGregorianCalendar toXMLGregorianCalendar(OffsetDateTime in) {
        return in != null ? toXMLGregorianCalendar(in.toZonedDateTime()) : null;
    }

    private static XMLGregorianCalendar toXMLGregorianCalendar(ZonedDateTime in) {
        return in != null ? toXMLGregorianCalendar(GregorianCalendar.from(in)) : null;
    }

    public static XMLGregorianCalendar toXMLGregorianCalendar(GregorianCalendar gcal) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
        } catch (DatatypeConfigurationException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public static String toString(XMLGregorianCalendar in) {
        return in != null ? in.toString() : null;
    }
}
