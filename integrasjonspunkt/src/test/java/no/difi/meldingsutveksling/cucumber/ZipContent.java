package no.difi.meldingsutveksling.cucumber;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Data
class ZipContent {

    private List<ZipFile> files = new ArrayList<>();

    ZipContent files(ZipFile file) {
        files.add(file);
        return this;
    }

    ZipContent files(Collection<ZipFile> in) {
        files.addAll(in);
        return this;
    }

    ZipFile getFile(String filename) {
        return getOptionalFile(filename)
                .orElseThrow(() -> new IllegalArgumentException(String.format("File not found for %s", filename)));
    }

    Optional<ZipFile> getOptionalFile(String filename) {
        return files.stream()
                .filter(p -> p.getFileName().equals(filename))
                .findAny();
    }
}
