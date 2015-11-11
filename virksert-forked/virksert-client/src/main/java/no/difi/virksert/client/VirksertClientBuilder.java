package no.difi.virksert.client;

public class VirksertClientBuilder {

    public static VirksertClientBuilder newInstance() {
        return new VirksertClientBuilder();
    }

    public static VirksertClientBuilder forProduction() {
        return newInstance().setUri("https://virksomhetssertifikat.difi.no/").setScope("production");
    }

    public static VirksertClientBuilder forTest() {
        return newInstance().setUri("https://test-virksomhetssertifikat.difi.no/").setScope("test");
    }

    VirksertClientBuilder() {
    }

    private String uri;
    private String scope;

    public VirksertClientBuilder setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public VirksertClientBuilder setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public VirksertClient build() {
        return new VirksertClient(uri, scope);
    }
}
