package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag;

import no.nav.vedtak.exception.TekniskException;

public final class FastsettUttaksgrunnlagFeil {

    private FastsettUttaksgrunnlagFeil() {
    }

    public static TekniskException kunneIkkeUtledeEndringsdato(Long behandlingId) {
        return new TekniskException("FP-282721",
            "Kunne ikke utlede endringsdato for revurdering med behandlingId=" + behandlingId);
    }
}
