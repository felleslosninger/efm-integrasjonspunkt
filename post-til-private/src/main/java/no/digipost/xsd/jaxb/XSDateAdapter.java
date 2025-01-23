
package no.digipost.xsd.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDate;

public class XSDateAdapter extends XmlAdapter<String, LocalDate> {

	@Override
	public LocalDate unmarshal(String value) {
		return XSDateCustomBinder.parseDate(value);
	}

	@Override
	public String marshal(LocalDate date) {
	    return XSDateCustomBinder.printDate(date);
	}

}
