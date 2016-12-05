package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PostAddressTest {
    @Test
    public void isNorge() throws Exception {
        PostAddress address = new PostAddress("", new StreetAddress("", "", "", ""), "", "", "norge");

        assertThat(address.isNorge(), is(true));
    }

    @Test
    public void CountryNoIsNorgeShouldBeTrue() {
        PostAddress address = new PostAddress("", new StreetAddress("", "", "", ""), "", "", "no");

        assertThat(address.isNorge(), is(true));
    }

    @Test
    public void countryNorIsNorgeShouldBeTrue() {
        PostAddress address = new PostAddress("", new StreetAddress("", "", "", ""), "", "", "nor");

        assertThat(address.isNorge(), is(true));
    }

    @Test
    public void countryNullIsNorgeShouldBeTrue() {
        PostAddress address = new PostAddress("", new StreetAddress("", "", "", ""), "", "", null);

        assertThat(address.isNorge(), is(true));
    }

    @Test
    public void countryBlankIsNorgeShouldBeTrue() {
        String country = "";
        PostAddress address = new PostAddress("", new StreetAddress("", "", "", ""), "", "", country);

        assertThat(address.isNorge(), is(true));
    }

    @Test
    public void countrySwedenIsNorgeShouldBeFalse() {
        String country = "Sweden";

        PostAddress address = new PostAddress("", new StreetAddress("", "", "", ""), "", "", "Sweden");

        assertThat(address.isNorge(), is(false));
    }

}