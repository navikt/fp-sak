package no.nav.foreldrepenger.domene.arbeidsforhold;

public class AktørYtelseEndring {
    private final boolean eksklusiveYtelserEndret;
    private final boolean andreytelserEndret;

    AktørYtelseEndring(boolean eksklusiveYtelserEndret, boolean andreytelserEndret) {
        this.eksklusiveYtelserEndret = eksklusiveYtelserEndret;
        this.andreytelserEndret = andreytelserEndret;
    }

    public boolean erEndret() {
        return eksklusiveYtelserEndret || andreytelserEndret;
    }

    public boolean erEksklusiveYtelserEndret() {
        return eksklusiveYtelserEndret;
    }
    public boolean erAndreYtelserEndret() {
        return andreytelserEndret;
    }
}
