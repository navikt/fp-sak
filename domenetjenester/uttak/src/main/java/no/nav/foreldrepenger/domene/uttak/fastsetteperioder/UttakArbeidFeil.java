package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;


import static no.nav.vedtak.feil.LogLevel.ERROR;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface UttakArbeidFeil extends DeklarerteFeil {

    @TekniskFeil(feilkode = "FP-677743", feilmelding = "Fant ikke beregningsgrunnlag for behandling %s", logLevel = ERROR)
    Feil manglendeBeregningsgrunnlag(Long behandlingId);
}
