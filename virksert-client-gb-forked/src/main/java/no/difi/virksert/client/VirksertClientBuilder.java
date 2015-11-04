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
                .setTrustedIntermediateAliases(new String[]{"buypass-intermediate", "commfides-intermediate"})
                .setTrustedRootAliases(new String[]{"buypass-root", "commfides-root"});
    }

    public static VirksertClientBuilder forTest() {
        return newInstance().setUri("https://test-virksomhetssertifikat.difi.no/")
                .setScope("test")
                .setTrustedIntermediateAliases(new String[]{"buypass-intermediate", "commfides-intermediate"})
                .setTrustedRootAliases(new String[]{"buypass-root", "commfides-root"});
    }

    public static VirksertClientBuilder forCustom(String uri, String scope, String[] rootAliases, String[] intermediateAliases) {
        return newInstance().setUri(uri).setScope(scope)
                .setTrustedIntermediateAliases(intermediateAliases)
                .setTrustedRootAliases(rootAliases);
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

    public VirksertClientBuilder setTrustedRootAliases(String... rootAliases) {
        this.rootAliases = rootAliases;
        return this;
    }

    public VirksertClientBuilder setTrustedIntermediateAliases(String... intermediateAliases) {
        this.intermediateAliases = intermediateAliases;
        return this;
    }

    public VirksertClient build() {
        return new VirksertClient(uri, scope, rootAliases, intermediateAliases);
    }
}
