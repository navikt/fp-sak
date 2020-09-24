package no.nav.foreldrepenger.ytelse.beregning.regler;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

import static no.nav.vedtak.feil.LogLevel.ERROR;

public interface FinnOverlappendeBeregningsgrunnlagOgUttaksPerioderFeil extends DeklarerteFeil {

    FinnOverlappendeBeregningsgrunnlagOgUttaksPerioderFeil FACTORY = FeilFactory.create(FinnOverlappendeBeregningsgrunnlagOgUttaksPerioderFeil.class);

    @TekniskFeil(feilkode = "FP-655547", feilmelding = "Finner ikke matchende uttaksaktivitet til beregningsgrunnlagandel. Beregningsgrunnlagsandel: %s Utaksaktiviteter: %s", logLevel = ERROR)
    Feil fantIngenKorresponderendeUttakaktivitetFeil(String andelbeskrivelse, String uttaksaktiviteter);

}
