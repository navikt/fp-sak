package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.vedtak.exception.TekniskException;

public final class FinnEndringsdatoFeil {

    private FinnEndringsdatoFeil() {
    }

    public static TekniskException behandlingErIkkeEnRevurdering(Long behandlingId) {
        var msg = String.format("Behandlingen med id %s er ikke en revurdering", behandlingId);
        return new TekniskException("FP-655544", msg);
    }

    public static TekniskException manglendeOriginalBehandling(Long behandlingId) {
        var msg = String.format("Fant ikke en original behandling for revurdering med id %s", behandlingId);
        return new TekniskException("FP-655545", msg);
    }

    public static TekniskException manglendeBeregningsresultatPeriode(Long beregningsresultatId) {
        var msg = String.format("Fant ikke beregningsresultatperiode for beregningsresultat med id %s",
            beregningsresultatId);
        return new TekniskException("FP-655542", msg);
    }
}
