package no.difi.meldingsutveksling.nextmove;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.capabilities.Capabilities;
import no.difi.meldingsutveksling.domain.capabilities.Capability;
import no.difi.meldingsutveksling.domain.capabilities.DocumentType;
import no.difi.meldingsutveksling.domain.capabilities.PostalAddress;

import java.util.Arrays;
import java.util.Collections;

@UtilityClass
class CapabilityTestData {

    static Capabilities capabilitiesDPO() {
        return new Capabilities()
                .setCapabilities(Arrays.asList(
                        new Capability()
                                .setProcess("urn:no:difi:profile:arkivmelding:planByggOgGeodata:ver1.0")
                                .setServiceIdentifier(ServiceIdentifier.DPO)
                                .setDocumentTypes(Collections.singletonList(
                                        new DocumentType()
                                                .setType("arkivmelding")
                                                .setStandard("urn:no:difi:arkivmelding:xsd::arkivmelding")
                                )),
                        new Capability()
                                .setProcess("urn:no:difi:profile:arkivmelding:response:ver1.0")
                                .setServiceIdentifier(ServiceIdentifier.DPO)
                                .setDocumentTypes(Arrays.asList(
                                        new DocumentType()
                                                .setType("arkivmelding_kvittering")
                                                .setStandard("urn:no:difi:arkivmelding:xsd::arkivmelding_kvittering"),
                                        new DocumentType()
                                                .setType("status")
                                                .setStandard("urn:no:difi:arkivmelding:xsd::status"),
                                        new DocumentType()
                                                .setType("feil")
                                                .setStandard("urn:no:difi:arkivmelding:xsd::feil")
                                ))
                ));
    }

    static Capabilities capabilitiesDPI() {
        return new Capabilities()
                .setCapabilities(Arrays.asList(
                        new Capability()
                                .setProcess("urn:no:difi:profile:digitalpost:info:ver1.0")
                                .setServiceIdentifier(ServiceIdentifier.DPI)
                                .setPostAddress(getPostAddress())
                                .setReturnAddress(getPostAddress())
                                .setDocumentTypes(Collections.singletonList(
                                        new DocumentType()
                                                .setType("digital")
                                                .setStandard("urn:no:difi:digitalpost:xsd:digital::digital")
                                )),
                        new Capability()
                                .setProcess("urn:no:difi:profile:digitalpost:vedtak:ver1.0")
                                .setServiceIdentifier(ServiceIdentifier.DPI)
                                .setPostAddress(getPostAddress())
                                .setReturnAddress(getPostAddress())
                                .setDocumentTypes(Arrays.asList(
                                        new DocumentType()
                                                .setType("digital")
                                                .setStandard("urn:no:difi:digitalpost:xsd:digital::digital")
                                ))
                ));
    }

    private static PostalAddress getPostAddress() {
        return new PostalAddress()
                .setName("Kari Nordmann")
                .setStreet("Utsikten 1")
                .setPostalCode("2900")
                .setPostalArea("FJELLHEIMEN")
                .setCountry("Norway");
    }
}
