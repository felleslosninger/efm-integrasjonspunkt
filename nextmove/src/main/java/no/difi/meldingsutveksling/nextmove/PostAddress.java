package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Sets;
import lombok.*;

import javax.persistence.Embeddable;
import javax.persistence.Entity;

@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class PostAddress {

    private String name;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String addressLine4;
    private String postalCode;
    private String postalArea;
    private String countryCode;
    private String country;

    public boolean isNorge() {
        return (country == null || "".equals(country)) || Sets.newHashSet("NORGE", "NORWAY", "NO", "NOR").contains(country.toUpperCase());
    }
}
