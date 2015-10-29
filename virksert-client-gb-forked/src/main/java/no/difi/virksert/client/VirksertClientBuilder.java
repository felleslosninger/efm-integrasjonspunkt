package no.difi.virksert.client;

public class VirksertClientBuilder {

    private String[] intermediateAliases;
    private String[] rootAliases;

    public static VirksertClientBuilder newInstance() {
        return new VirksertClientBuilder();
    }

    public static VirksertClientBuilder forProduction() {
        return newInstance().setUri("https://virksomhetssertifikat.difi.no/")
                .setScope("production")
                .setIntermediateAliases(new String[]{"buypass-intermediate", "commfides-intermediate"})
                .setRootAliases(new String[]{"buypass-root", "commfides-root"});
    }

    public static VirksertClientBuilder forTest() {
        return newInstance().setUri("https://test-virksomhetssertifikat.difi.no/")
                .setScope("test")
                .setIntermediateAliases(new String[]{"buypass-intermediate", "commfides-intermediate"})
                .setRootAliases(new String[]{"buypass-root", "commfides-root"});
    }

    public static VirksertClientBuilder forTest(String uri, String scope, String[] rootAliases, String[] intermediateAliases) {
        return newInstance().setUri(uri).setScope(scope)
                .setIntermediateAliases(intermediateAliases)
                .setRootAliases(rootAliases);
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

    public VirksertClientBuilder setRootAliases(String[] rootAliases) {
        this.rootAliases = rootAliases;
        return this;
    }

    public VirksertClientBuilder setIntermediateAliases(String[] intermediateAliases) {
        this.intermediateAliases = intermediateAliases;
        return this;
    }


    public VirksertClient build() {
        return new VirksertClient(uri, scope, rootAliases, intermediateAliases);
    }
}
