package no.difi.meldingsutveksling;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DateTimeUtilTest {

    @Test
    public void testAtStartOfDay() {
        assertThat(DateTimeUtil.atStartOfDay(DateTimeUtil.toXMLGregorianCalendar("2019-03-12")).toString())
                .isEqualTo("2019-03-12T00:00:00");
        assertThat(DateTimeUtil.atStartOfDay(DateTimeUtil.toXMLGregorianCalendar("2019-05-01")).toString())
                .isEqualTo("2019-05-01T00:00:00");
        assertThat(DateTimeUtil.atStartOfDay(DateTimeUtil.toXMLGregorianCalendar("2019-05-01+02:00")).toString())
                .isEqualTo("2019-05-01T00:00:00+02:00");
    }

    @Test
    public void testStringToXMLGregorianCalendar() {
        assertThat(DateTimeUtil.toXMLGregorianCalendar("2019-03-12").toString()).isEqualTo("2019-03-12");
        assertThat(DateTimeUtil.toXMLGregorianCalendar("2019-03-12Z").toString()).isEqualTo("2019-03-12Z");
        assertThat(DateTimeUtil.toXMLGregorianCalendar("2019-03-12+01:00").toString()).isEqualTo("2019-03-12+01:00");
        assertThat(DateTimeUtil.toXMLGregorianCalendar("2019-03-12T15:31:24.123").toString()).isEqualTo("2019-03-12T15:31:24.123");
        assertThat(DateTimeUtil.toXMLGregorianCalendar("2019-05-01T15:31:24.123").toString()).isEqualTo("2019-05-01T15:31:24.123");
        assertThat(DateTimeUtil.toXMLGregorianCalendar("2019-03-12T15:31:24.123+01:00").toString()).isEqualTo("2019-03-12T15:31:24.123+01:00");
    }
}