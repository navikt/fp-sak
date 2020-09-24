package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface BeregningsresultatVerifisererFeil extends DeklarerteFeil {
    BeregningsresultatVerifisererFeil FEILFACTORY = FeilFactory.create(BeregningsresultatVerifisererFeil.class);

    @TekniskFeil(feilkode = "FP-370742", feilmelding = "Precondition feilet: Finner ikke matchende beregningsgrunnlagandel for uttaksandel %s . Listen med beregningsgrunnlagandeler er: %s", logLevel = LogLevel.ERROR)
    Feil verifiserAtUttakAndelHarMatchendeBeregningsgrunnlagAndel(String uttakAndelBeskrivelse, String beregningsgrunnlagandeler);

    @TekniskFeil(feilkode = "FP-370743", feilmelding = "Precondition feilet: Finner ikke matchende uttaksandel for beregningsgrunnlagsandel %s . Listen med uttaksandeler er: %s", logLevel = LogLevel.ERROR)
    Feil verifiserAtBeregningsgrunnlagAndelHarMatchendeUttakandel(String beregningsgrunnlagandel, String uttaksandeler);

    @TekniskFeil(feilkode = "FP-370744", feilmelding = "Postcondition feilet: Beregningsresultat i ugyldig tilstand etter steg. Dagsatsen på %s er mindre enn 0, men skulle ikke vært det.", logLevel = LogLevel.ERROR)
    Feil verifiserIkkeNegativDagsats(String obj);

    @TekniskFeil(feilkode = "FP-370745", feilmelding = "Postcondition feilet: Beregningsresultat i ugyldig tilstand etter steg. Dagsats på andel skal til arbeidsgiver men arbeidsgiver er ikke satt", logLevel = LogLevel.ERROR)
    Feil verifiserAtArbeidsgiverErSatt();

    @TekniskFeil(feilkode = "FP-370747", feilmelding = "Postcondition feilet: Beregningsresultat i ugyldig tilstand etter steg. Andel med status %s skal aldri ha utbetaling til arbeidsgiver", logLevel = LogLevel.ERROR)
    Feil verifiserAtStatusSomIkkeErATIkkeKanHaUtbetalingTilArbeidsgiver(String aktivitetstatusKode);

}
