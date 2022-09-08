package no.difi.meldingsutveksling;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateTimeUtilTest {

    @Test
    public void testAtStartOfDay() {
        assertEquals(DateTimeUtil.atStartOfDay(DateTimeUtil.toXMLGregorianCalendar("2019-03-12")).toString(), "2019-03-12T00:00:00");
        assertEquals(DateTimeUtil.atStartOfDay(DateTimeUtil.toXMLGregorianCalendar("2019-05-01")).toString(), "2019-05-01T00:00:00");
        assertEquals(DateTimeUtil.atStartOfDay(DateTimeUtil.toXMLGregorianCalendar("2019-05-01+02:00")).toString(), "2019-05-01T00:00:00+02:00");
    }

    @Test
    public void testNegativeValues() {
        assertEquals("0001-01-01T01:00:00.000+01:00", DateTimeUtil.toXMLGregorianCalendar(Long.parseLong("-62135769600000")).toString());
        assertEquals("0001-01-01T01:00:00.000+01:00", DateTimeUtil.toXMLGregorianCalendar(Long.parseLong("-62177289687000")).toString());
    }

    @Test
    public void testStringToXMLGregorianCalendar() {
        assertEquals(DateTimeUtil.toXMLGregorianCalendar("2019-03-12").toString(), "2019-03-12");
        assertEquals(DateTimeUtil.toXMLGregorianCalendar("2019-03-12Z").toString(), "2019-03-12Z");
        assertEquals(DateTimeUtil.toXMLGregorianCalendar("2019-03-12+01:00").toString(), "2019-03-12+01:00");
        assertEquals(DateTimeUtil.toXMLGregorianCalendar("2019-03-12T15:31:24.123").toString(), "2019-03-12T15:31:24.123");
        assertEquals(DateTimeUtil.toXMLGregorianCalendar("2019-05-01T15:31:24.123").toString(), "2019-05-01T15:31:24.123");
        assertEquals(DateTimeUtil.toXMLGregorianCalendar("2019-03-12T15:31:24.123+01:00").toString(), "2019-03-12T15:31:24.123+01:00");
    }
}