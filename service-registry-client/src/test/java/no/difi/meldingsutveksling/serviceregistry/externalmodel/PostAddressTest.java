package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PostAddressTest {
    
    @Test
    public void isNorge() throws Exception {
        PostAddress address = new PostAddress("", "", "", "", "norge");

        assertThat(address.isNorge(), is(true));
    }

    @Test
    public void CountryNoIsNorgeShouldBeTrue() {
        PostAddress address = new PostAddress("", "", "", "", "no");

        assertThat(address.isNorge(), is(true));
    }

    @Test
    public void countryNorIsNorgeShouldBeTrue() {
        PostAddress address = new PostAddress("", "", "", "", "nor");

        assertThat(address.isNorge(), is(true));
    }

    @Test
    public void countryNorwayIsNorgeShouldBeTrue() {
        PostAddress address = new PostAddress("", "", "", "", "norway");

        assertThat(address.isNorge(), is(true));
    }

    @Test
    public void countryNullIsNorgeShouldBeTrue() {
        PostAddress address = new PostAddress("", "", "", "", null);

        assertThat(address.isNorge(), is(true));
    }

    @Test
    public void countryBlankIsNorgeShouldBeTrue() {
        PostAddress address = new PostAddress("", "", "", "", "");

        assertThat(address.isNorge(), is(true));
    }

    @Test
    public void countrySwedenIsNorgeShouldBeFalse() {
        PostAddress address = new PostAddress("", "", "", "", "Sweden");

        assertThat(address.isNorge(), is(false));
    }

}