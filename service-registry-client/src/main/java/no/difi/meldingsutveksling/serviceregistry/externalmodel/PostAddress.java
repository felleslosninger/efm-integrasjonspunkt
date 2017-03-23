package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.google.common.collect.Sets;
import lombok.Data;

@Data
public class PostAddress {
    public static final PostAddress EMPTY = new PostAddress();
    private String name;
    private String street;
    private String postalCode;
    private String postalArea;
    private String country;

    public PostAddress() {
        name = "";
        street = "";
        postalCode = "";
        postalArea = "";
        country = "";
    }

    public PostAddress(String name, String street, String postalCode, String postalArea, String country) {
        this.name = name;
        this.street = street;
        this.postalCode = postalCode;
        this.postalArea = postalArea;
        this.country = country;
    }

    public boolean isNorge() {
        return (country == null || "".equals(country)) || Sets.newHashSet("NORGE", "NO", "NOR").contains(country.toUpperCase());
    }

}