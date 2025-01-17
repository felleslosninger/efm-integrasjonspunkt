package no.difi.meldingsutveksling.arkivmelding;

import lombok.SneakyThrows;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ArkivmeldingUtil {

    private final JAXBContext marshallerContext;

    @SneakyThrows
    public ArkivmeldingUtil() {
        this.marshallerContext = JAXBContext.newInstance(Arkivmelding.class);
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
        marshallerContext.createMarshaller().marshal(am, bos);
        return bos.toByteArray();
    }

    public Arkivmelding unmarshalArkivmelding(Resource resource) throws JAXBException {
        try (InputStream inputStream = resource.getInputStream()) {
            return marshallerContext.createUnmarshaller().unmarshal(new StreamSource(inputStream), Arkivmelding.class).getValue();
        } catch (IOException e) {
            throw new ArkivmeldingRuntimeException("Could not unmarshal Arkivmelding", e);
        }
    }

    public Saksmappe getSaksmappe(Arkivmelding am) {
        return am.getMappe().stream()
                .filter(Saksmappe.class::isInstance)
                .map(Saksmappe.class::cast)
                .findFirst()
                .orElseThrow(() -> new ArkivmeldingRuntimeException("No \"Saksmappe\" found in Arkivmelding"));
    }

    public Journalpost getJournalpost(Arkivmelding am) {
        return getSaksmappe(am).getBasisregistrering().stream()
                .filter(Journalpost.class::isInstance)
                .map(Journalpost.class::cast)
                .findFirst()
                .orElseThrow(() -> new ArkivmeldingRuntimeException("No \"Journalpost\" found in Arkivmelding"));
    }
}
