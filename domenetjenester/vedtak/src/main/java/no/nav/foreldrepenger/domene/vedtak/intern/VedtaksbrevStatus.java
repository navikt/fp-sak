package no.nav.foreldrepenger.domene.vedtak.intern;

public enum VedtaksbrevStatus {
    VEDTAKSBREV_PRODUSERES,
    INGEN_VEDTAKSBREV,
    INGEN_VEDTAKSBREV_ANKE,
    INGEN_VEDTAKSBREV_KLAGEBEHANDLING,
    INGEN_VEDTAKSBREV_BEHANDLING_ETTER_KLAGE,
    INGEN_VEDTAKSBREV_JUSTERING_AV_FERIEPENGER,
    INGEN_VEDTAKSBREV_INGEN_KONSEKVENS_FOR_YTELSE;

    public boolean vedtaksbrevSkalProduseres() {
        return VEDTAKSBREV_PRODUSERES.equals(this);
    }
}
