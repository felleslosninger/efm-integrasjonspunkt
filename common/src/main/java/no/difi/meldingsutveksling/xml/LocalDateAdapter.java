package no.difi.meldingsutveksling.xml;

import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;

public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {
    /**
     * Convert a value type to a bound type.
     *
     * @param v The value to be converted. Can be null.
     * @throws Exception if there's an error during the conversion. The caller is responsible for
     *                   reporting the error to the user through {@link ValidationEventHandler}.
     */
    @Override
    public LocalDate unmarshal(String v) throws Exception {
        return LocalDate.parse(v);
    }

    /**
     * Convert a bound type to a value type.
     *
     * @param v The value to be convereted. Can be null.
     * @throws Exception if there's an error during the conversion. The caller is responsible for
     *                   reporting the error to the user through {@link ValidationEventHandler}.
     */
    @Override
    public String marshal(LocalDate v) throws Exception {
        return v.toString();
    }
}
