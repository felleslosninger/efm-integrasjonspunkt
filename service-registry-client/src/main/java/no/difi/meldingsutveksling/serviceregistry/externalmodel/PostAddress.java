package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.google.common.collect.Sets;

public class PostAddress {
    private final String name;
    private final String street1;
    private final String street2;
    private final String street3;
    private final String street4;
    private final String postalCode;
    private final String postalArea;
    private final String country;

    public PostAddress(String name, String street1, String street2, String street3, String street4, String postalCode, String postalArea, String country) {
        this.name = name;
        this.street1 = street1;
        this.street2 = street2;
        this.street3 = street3;
        this.street4 = street4;
        this.postalCode = postalCode;
        this.postalArea = postalArea;
        this.country = country;
    }


    public String getPostalCode() {
        return postalCode;
    }

    public String getPostalArea() {
        return postalArea;
    }

    public String getCountry() {
        return country;
    }

    public String getName() {
        return name;
    }

    public String getStreet1() {
        return street1;
    }

    public String getStreet2() {
        return street2;
    }

    public String getStreet3() {
        return street3;
    }

    public String getStreet4() {
        return street4;
    }

    public boolean isNorge() {
        return (country == null || "".equals(country)) || Sets.newHashSet("NORGE", "NO", "NOR").contains(country.toUpperCase());
    }
}