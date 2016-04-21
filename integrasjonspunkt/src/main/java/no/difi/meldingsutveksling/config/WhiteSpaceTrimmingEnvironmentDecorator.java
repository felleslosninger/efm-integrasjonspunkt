package no.difi.meldingsutveksling.config;

import org.springframework.core.env.Environment;

class WhiteSpaceTrimmingEnvironmentDecorator extends EnvironmentDecorator {

    public WhiteSpaceTrimmingEnvironmentDecorator(Environment e) {
        super(e);
    }

    @Override
    public String getProperty(String s) {
        final String property = super.getProperty(s);
        if (property != null) {
            return property.trim();
        } else {
            return null;
        }
    }

    @Override
    public String getProperty(String s, String s1) {
        return super.getProperty(s, s1);
    }
}
