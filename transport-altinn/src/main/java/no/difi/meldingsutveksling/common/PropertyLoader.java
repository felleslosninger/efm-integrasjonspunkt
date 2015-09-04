package no.difi.meldingsutveksling.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class PropertyLoader {
    private String filename = "application.properties";

    public PropertyLoader() {
    }

    public PropertyLoader(String filename) {
        this.filename = filename;
    }

    public Properties load() {
        Properties properties = new Properties();

        try(InputStream resource = this.getClass().getResourceAsStream(filename)) {
            properties.load(new BufferedReader(new InputStreamReader(resource)));
        } catch (IOException e) {
            throw new PropertyLoaderException(e);
        }
        return properties;
    }

    private class PropertyLoaderException extends RuntimeException {
        public PropertyLoaderException(IOException e) {

        }
    }
}
