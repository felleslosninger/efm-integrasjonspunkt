package no.difi.meldingsutveksling.serviceregistry.externalmodel;

public class StreetAddress {
    private final String street1;
    private final String street2;
    private final String street3;
    private final String street4;

    public StreetAddress(String street1, String street2, String street3, String street4) {
        this.street1 = street1;
        this.street2 = street2;
        this.street3 = street3;
        this.street4 = street4;
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
}
