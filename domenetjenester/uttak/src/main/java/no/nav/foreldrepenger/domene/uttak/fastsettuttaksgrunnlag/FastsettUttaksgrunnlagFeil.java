package no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface FastsettUttaksgrunnlagFeil extends DeklarerteFeil {

    FastsettUttaksgrunnlagFeil FACTORY = FeilFactory.create(FastsettUttaksgrunnlagFeil.class);

    @TekniskFeil( feilkode = "FP-282721", feilmelding = "Kunne ikke utlede endringsdato for revurdering med behandlingId=%s", logLevel = LogLevel.ERROR)
    Feil kunneIkkeUtledeEndringsdato(Long behandlingId);

}
