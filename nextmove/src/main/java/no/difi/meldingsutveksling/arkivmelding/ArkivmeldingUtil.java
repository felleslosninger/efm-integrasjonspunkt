package no.difi.meldingsutveksling.arkivmelding;

import lombok.SneakyThrows;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ArkivmeldingUtil {

    private final JAXBContext jaxbContext;

    @SneakyThrows
    public ArkivmeldingUtil() {
        this.jaxbContext = JAXBContextFactory.createContext(new Class[]{Arkivmelding.class}, new HashMap());
    }

    public List<String> getFilenames(Arkivmelding am) {
        return getJournalpost(am).getDokumentbeskrivelseAndDokumentobjekt().stream()
                .filter(Dokumentbeskrivelse.class::isInstance)
                .map(Dokumentbeskrivelse.class::cast)
                .sorted(Comparator.comparing(Dokumentbeskrivelse::getDokumentnummer))
                .flatMap(d -> d.getDokumentobjekt().stream())
                .map(Dokumentobjekt::getReferanseDokumentfil)
                .collect(Collectors.toList());
    }

    public byte[] marshalArkivmelding(Arkivmelding am) throws JAXBException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        jaxbContext.createMarshaller().marshal(am, bos);
        return bos.toByteArray();
    }

    public Arkivmelding unmarshalArkivmelding(InputStream inputStream) throws JAXBException {
        return jaxbContext.createUnmarshaller().unmarshal(new StreamSource(inputStream), Arkivmelding.class).getValue();
    }

    public Saksmappe getSaksmappe(Arkivmelding am) {
        return am.getMappe().stream()
                .filter(Saksmappe.class::isInstance)
                .map(Saksmappe.class::cast)
                .findFirst()
                .orElseThrow(() -> new MeldingsUtvekslingRuntimeException("No \"Saksmappe\" found in Arkivmelding"));
    }

    public Journalpost getJournalpost(Arkivmelding am) {
        return getSaksmappe(am).getBasisregistrering().stream()
                .filter(Journalpost.class::isInstance)
                .map(Journalpost.class::cast)
                .findFirst()
                .orElseThrow(() -> new MeldingsUtvekslingRuntimeException("No \"Journalpost\" found in Arkivmelding"));
    }
}
