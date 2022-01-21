/**
 * Copyright (C) Posten Norge AS
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.difi.meldingsutveksling.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

@Getter
@Setter
@EqualsAndHashCode
public class Organisasjonsnummer {
    private static final Pattern ISO6523_PATTERN = Pattern.compile("^([0-9]{4}:)?([0-9]{9})?(?::)?([0-9]{9})?$");
    private static final String ISO6523_AUTHORITY = "iso6523-actorid-upis";

    private final String orgNummer;
    private final String paaVegneAvOrgnr;

    private Organisasjonsnummer(final String orgNummer) {
        this.orgNummer = orgNummer;
        this.paaVegneAvOrgnr = null;
    }

    private Organisasjonsnummer(final String orgNummer, final String paaVegneAvOrgnr) {
        this.orgNummer = orgNummer;
        this.paaVegneAvOrgnr = paaVegneAvOrgnr;
    }

    public Optional<String> getPaaVegneAvOrgnr() {
        if (hasOnBehalfOf()) {
            return Optional.of(paaVegneAvOrgnr);
        }
        return Optional.empty();
    }

    public boolean hasOnBehalfOf() {
        return StringUtils.hasLength(paaVegneAvOrgnr);
    }

    public String getOrgNummer() {
        return this.orgNummer;
    }

    public String asIso6523() {
        String iso6523 = "0192:" + orgNummer;
        if (!isNullOrEmpty(paaVegneAvOrgnr)) {
            return iso6523 + ":" + paaVegneAvOrgnr;
        }
        return iso6523;
    }

    public String authority() {
        return ISO6523_AUTHORITY;
    }

    public static Organisasjonsnummer parse(final String value) {
        return Organisasjonsnummer.isIso6523(value) ? Organisasjonsnummer.fromIso6523(value) : Organisasjonsnummer.from(value);
    }

    public static Organisasjonsnummer from(final String orgnr) {
        return new Organisasjonsnummer(orgnr);
    }

    public static Organisasjonsnummer from(final String orgnr, final String paaVegneAvOrgnr) {
        return new Organisasjonsnummer(orgnr, paaVegneAvOrgnr);
    }

    public static Organisasjonsnummer fromIso6523(final String iso6523Orgnr) {
        Matcher matcher = ISO6523_PATTERN.matcher(iso6523Orgnr);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid organization number. " +
                    "Expected format is ISO 6523, got following organization number: " + iso6523Orgnr);
        }
        if (!isNullOrEmpty(matcher.group(3))) {
            return new Organisasjonsnummer(matcher.group(2), matcher.group(3));
        }
        return new Organisasjonsnummer(matcher.group(2));
    }

    public static boolean isIso6523(final String iso6523Orgnr) {
        return ISO6523_PATTERN.matcher(iso6523Orgnr).matches();
    }

    @Override
    public String toString() {
        return this.asIso6523();
    }

}
