package no.difi.meldingsutveksling.cucumber;

import lombok.Value;

import java.util.Map;
import java.util.Set;

@Value
class AsicInfo {

    private final Map<String, String> fileMap;

    Set<String> getFilenames() {
        return fileMap.keySet();
    }

    String getContent(String filename) {
        return fileMap.get(filename);
    }
}
