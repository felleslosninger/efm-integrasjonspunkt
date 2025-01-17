package no.difi.meldingsutveksling.cucumber;

import lombok.Data;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Data
class ZipContent {

    private List<Document> files = new ArrayList<>();

    ZipContent file(Document file) {
        files.add(file);
        return this;
    }

    ZipContent files(Collection<Document> in) {
        files.addAll(in);
        return this;
    }

    Document getFile(String filename) {
        return getOptionalFile(filename)
                .orElseThrow(() -> new IllegalArgumentException("File not found for %s".formatted(filename)));
    }

    Optional<Document> getOptionalFile(String filename) {
        return files.stream()
                .filter(p -> p.getFilename().equals(filename))
                .findAny();
    }
}
