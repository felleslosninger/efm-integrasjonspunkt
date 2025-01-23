
package no.digipost.xsd.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.ZonedDateTime;

public class XSDateTimeAdapter extends XmlAdapter<String, ZonedDateTime> {

	@Override
	public ZonedDateTime unmarshal(String value) {
		return XSDateTimeCustomBinder.parseDateTime(value);
	}

	@Override
	public String marshal(ZonedDateTime value) {
		return XSDateTimeCustomBinder.printDateTime(value);
	}

}
