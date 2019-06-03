package no.difi.meldingsutveksling;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.springframework.util.StringUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.TimeZone;

@UtilityClass
public class DateTimeUtil {

    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Oslo");
    public static final ZoneId DEFAULT_ZONE_ID = DEFAULT_TIME_ZONE.toZoneId();

    public static XMLGregorianCalendar atStartOfDay(XMLGregorianCalendar in) {
        if (in == null) {
            return null;
        }

        XMLGregorianCalendar out = (XMLGregorianCalendar) in.clone();
        out.setTime(0, 0, 0, null);
        return out;
    }

    public static XMLGregorianCalendar toXMLGregorianCalendar(String in) {
        if (StringUtils.isEmpty(in)) {
            return null;
        }

        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(in);
        } catch (DatatypeConfigurationException e) {
            throw new MeldingsUtvekslingRuntimeException(String.format("Could not convert '%s' to XMLGregorianCalendar", in), e);
        }
    }

    public static XMLGregorianCalendar toXMLGregorianCalendar(long millis) {
        return toXMLGregorianCalendar(toZonedDateTime(millis));
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

    private static XMLGregorianCalendar toXMLGregorianCalendar(GregorianCalendar gcal) {
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
