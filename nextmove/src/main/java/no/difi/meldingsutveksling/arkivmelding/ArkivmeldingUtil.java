package no.difi.meldingsutveksling.arkivmelding;

import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArkivmeldingUtil {

    private ArkivmeldingUtil() {
    }

    public static List<String> getFilenames(Arkivmelding am) {
        return getJournalpost(am).getDokumentbeskrivelseAndDokumentobjekt().stream()
                .filter(Dokumentbeskrivelse.class::isInstance)
                .map(Dokumentbeskrivelse.class::cast)
                .sorted(Comparator.comparing(Dokumentbeskrivelse::getDokumentnummer))
                .flatMap(d -> d.getDokumentobjekt().stream())
                .map(Dokumentobjekt::getReferanseDokumentfil)
                .collect(Collectors.toList());
    }

    public static byte[] marshalArkivmelding(Arkivmelding am) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Arkivmelding.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshaller.marshal(am, bos);
        return bos.toByteArray();
    }

    public static Arkivmelding unmarshalArkivmelding(InputStream inputStream) throws JAXBException {
        JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Arkivmelding.class}, new HashMap());
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new StreamSource(inputStream), Arkivmelding.class).getValue();
    }

    public static Saksmappe getSaksmappe(Arkivmelding am) {
        return am.getMappe().stream()
                .filter(Saksmappe.class::isInstance)
                .map(Saksmappe.class::cast)
                .findFirst()
                .orElseThrow(() -> new MeldingsUtvekslingRuntimeException("No \"Saksmappe\" found in Arkivmelding"));
    }

    public static Journalpost getJournalpost(Arkivmelding am) {
        return getSaksmappe(am).getBasisregistrering().stream()
                .filter(Journalpost.class::isInstance)
                .map(Journalpost.class::cast)
                .findFirst()
                .orElseThrow(() -> new MeldingsUtvekslingRuntimeException("No \"Journalpost\" found in Arkivmelding"));
    }
}
