package no.difi.meldingsutveksling.receipt.service;

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

    static Capabilities capabilities() {
        return new Capabilities()
                .setCapabilities(Arrays.asList(
                        new Capability()
                                .setProcess("urn:no:difi:profile:arkivmelding:planByggOgGeodata:ver1.0")
                                .setServiceIdentifier(ServiceIdentifier.DPO)
                                .setPostAddress(getPostAddress())
                                .setReturnAddress(getPostAddress())
                                .setDocumentTypes(Collections.singletonList(
                                        new DocumentType()
                                                .setType("arkivmelding")
                                                .setStandard("urn:no:difi:arkivmelding:xsd::arkivmelding")
                                )),
                        new Capability()
                                .setProcess("urn:no:difi:profile:arkivmelding:response:ver1.0")
                                .setServiceIdentifier(ServiceIdentifier.DPO)
                                .setPostAddress(getPostAddress())
                                .setReturnAddress(getPostAddress())
                                .setDocumentTypes(Arrays.asList(
                                        new DocumentType()
                                                .setType("arkivmelding_kvittering")
                                                .setStandard("urn:no:difi:arkivmelding:xsd::arkivmelding_kvittering"),
                                        new DocumentType()
                                                .setType("status")
                                                .setStandard("urn:no:difi:eformidling:xsd::status"),
                                        new DocumentType()
                                                .setType("feil")
                                                .setStandard("urn:no:difi:eformidling:xsd::feil")
                                ))
                ));
    }

    private static PostalAddress getPostAddress() {
        return new PostalAddress()
                .setName("Fjellheimen kommune")
                .setStreet("Rådhusgaten 1")
                .setPostalCode("2900")
                .setPostalArea("FJELLHEIMEN")
                .setCountry("Norway");
    }
}
