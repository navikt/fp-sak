package no.nav.foreldrepenger.web.app.rest;

public class TestContextPathProvider implements ContextPathProvider {

    private static final String DEFAULT = "/testpath";

    private final String contextpath;

    public TestContextPathProvider(String contextpath) {
        this.contextpath = contextpath;
    }

    public TestContextPathProvider() {
        this(DEFAULT);
    }

    @Override
    public String get() {
        return contextpath;
    }
}
