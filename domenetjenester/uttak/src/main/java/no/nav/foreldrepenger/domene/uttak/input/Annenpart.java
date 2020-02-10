package no.nav.foreldrepenger.domene.uttak.input;

import java.util.Objects;

public class Annenpart {
    private final boolean innvilgetES;
    private final Long gjeldendeVedtakBehandlingId;

    public Annenpart(boolean innvilgetES, Long gjeldendeVedtakBehandlingId) {
        Objects.requireNonNull(gjeldendeVedtakBehandlingId, "Uttak bryr seg bare om annenpart hvis det foreligger et vedtak");
        this.innvilgetES = innvilgetES;
        this.gjeldendeVedtakBehandlingId = gjeldendeVedtakBehandlingId;
    }

    public boolean harInnvilgetES() {
        return innvilgetES;
    }

    public long getGjeldendeVedtakBehandlingId() {
        return gjeldendeVedtakBehandlingId;
    }
}
