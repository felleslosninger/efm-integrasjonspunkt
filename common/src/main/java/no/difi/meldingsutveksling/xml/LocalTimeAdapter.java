package no.difi.meldingsutveksling.xml;

import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalTime;

public class LocalTimeAdapter extends XmlAdapter<String, LocalTime> {
    /**
     * Convert a value type to a bound type.
     *
     * @param v The value to be converted. Can be null.
     * @throws Exception if there's an error during the conversion. The caller is responsible for
     *                   reporting the error to the user through {@link ValidationEventHandler}.
     */
    @Override
    public LocalTime unmarshal(String v) throws Exception {
        return LocalTime.parse(v);
    }

    /**
     * Convert a bound type to a value type.
     *
     * @param v The value to be convereted. Can be null.
     * @throws Exception if there's an error during the conversion. The caller is responsible for
     *                   reporting the error to the user through {@link ValidationEventHandler}.
     */
    @Override
    public String marshal(LocalTime v) throws Exception {
        return v.toString();
    }
}
