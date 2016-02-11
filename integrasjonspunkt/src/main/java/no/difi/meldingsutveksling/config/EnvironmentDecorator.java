package no.difi.meldingsutveksling.config;

import org.springframework.core.env.Environment;

class EnvironmentDecorator implements Environment {

    private Environment environment;

    public EnvironmentDecorator(Environment e) {
        environment = e;
    }

    public String[] getActiveProfiles() {
        return environment.getActiveProfiles();
    }

    public String resolveRequiredPlaceholders(String s) throws IllegalArgumentException {
        return environment.resolveRequiredPlaceholders(s);
    }

    public String[] getDefaultProfiles() {
        return environment.getDefaultProfiles();
    }

    public <T> T getRequiredProperty(String s, Class<T> aClass) throws IllegalStateException {
        return environment.getRequiredProperty(s, aClass);
    }

    public String getRequiredProperty(String s) throws IllegalStateException {
        return environment.getRequiredProperty(s);
    }

    public <T> T getProperty(String s, Class<T> aClass, T t) {
        return environment.getProperty(s, aClass, t);
    }

    public String getProperty(String s) {
        final String propertyValue = environment.getProperty(s);
        return propertyValue;
    }

    public String resolvePlaceholders(String s) {
        return environment.resolvePlaceholders(s);
    }

    public <T> Class<T> getPropertyAsClass(String s, Class<T> aClass) {
        return environment.getPropertyAsClass(s, aClass);
    }

    public boolean acceptsProfiles(String... strings) {
        return environment.acceptsProfiles(strings);
    }

    public boolean containsProperty(String s) {
        return environment.containsProperty(s);
    }

    public <T> T getProperty(String s, Class<T> aClass) {
        return environment.getProperty(s, aClass);
    }

    public String getProperty(String s, String s1) {
        return environment.getProperty(s, s1);
    }
}
