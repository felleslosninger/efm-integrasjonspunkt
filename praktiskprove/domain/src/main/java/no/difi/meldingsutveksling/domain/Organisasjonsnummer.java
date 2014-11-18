/**
 * Copyright (C) Posten Norge AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.difi.meldingsutveksling.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Organisasjonsnummer {
	public static final Pattern ISO6523_PATTERN = Pattern.compile("^([0-9]{4}:)?([0-9]{9})$");

	private final String orgNummer;
	public static final String PARTY_ID_TYPE = "urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908";
	public static final String ISO6523_ACTORID = PARTY_ID_TYPE;
	public static final String ISO6523_ACTORID_OLD = "iso6523-actorid-upis";

	public static final Organisasjonsnummer NULL = new Organisasjonsnummer("");

	public Organisasjonsnummer(final String orgNummer) {
		this.orgNummer = orgNummer;
	}

	public String asIso6523() {
		return "9908:" + orgNummer;
	}

	@Override
	public String toString() {
		return orgNummer;
	}

	public static Organisasjonsnummer fromIso6523(final String iso6523Orgnr) {
		Matcher matcher = ISO6523_PATTERN.matcher(iso6523Orgnr);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid organizational number. " +
					"Expected format is ISO 6523, got following organizational number: " + iso6523Orgnr);
		}
		return new Organisasjonsnummer(matcher.group(2));
	}
	public static boolean isIso6523(final String iso6523Orgnr) {
		return ISO6523_PATTERN.matcher(iso6523Orgnr).matches();
	}

    @Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof Organisasjonsnummer) {
			return orgNummer.equals(((Organisasjonsnummer)obj).orgNummer);
		}
		return false;
	}

    @Override
	public int hashCode() {
		return orgNummer.hashCode();
	}
}
