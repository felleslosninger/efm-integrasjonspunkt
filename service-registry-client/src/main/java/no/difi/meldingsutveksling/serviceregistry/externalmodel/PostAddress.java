package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

public class PostAddress {
    public static final PostAddress EMPTY = new PostAddress();
    private String name;
    private String street1;
    private String street2;
    private String street3;
    private String street4;
    private String postalCode;
    private String postalArea;
    private String country;

    public PostAddress() {
        name = "";
        street1 = "";
        street2 = "";
        street3 = "";
        street4 = "";
        postalCode = "";
        postalArea = "";
        country = "";
    }

    public PostAddress(String name, StreetAddress streetAddress, String postalCode, String postalArea, String country) {
        this.name = name;
        this.street1 = streetAddress.getStreet1();
        this.street2 = streetAddress.getStreet2();
        this.street3 = streetAddress.getStreet3();
        this.street4 = streetAddress.getStreet4();
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

    public void setName(String name) {
        this.name = name;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public void setStreet3(String street3) {
        this.street3 = street3;
    }

    public void setStreet4(String street4) {
        this.street4 = street4;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public void setPostalArea(String postalArea) {
        this.postalArea = postalArea;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isNorge() {
        return (country == null || "".equals(country)) || Sets.newHashSet("NORGE", "NO", "NOR").contains(country.toUpperCase());
    }

    @Override
    @SuppressWarnings({"squid:S00122", "squid:S1067"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostAddress address = (PostAddress) o;
        return Objects.equal(name, address.name) &&
                Objects.equal(street1, address.street1) &&
                Objects.equal(street2, address.street2) &&
                Objects.equal(street3, address.street3) &&
                Objects.equal(street4, address.street4) &&
                Objects.equal(postalCode, address.postalCode) &&
                Objects.equal(postalArea, address.postalArea) &&
                Objects.equal(country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, street1, street2, street3, street4, postalCode, postalArea, country);
    }
}