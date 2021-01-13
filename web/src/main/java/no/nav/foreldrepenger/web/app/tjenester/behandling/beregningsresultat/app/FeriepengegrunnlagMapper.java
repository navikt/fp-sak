package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.FeriepengegrunnlagAndelDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.FeriepengegrunnlagDto;

import java.util.Optional;

public final class FeriepengegrunnlagMapper {

    private FeriepengegrunnlagMapper() {
        // Skjuler default konstruktør
    }

    public static Optional<FeriepengegrunnlagDto> map(BeregningsresultatEntitet entitet) {
        if (entitet.getBeregningsresultatFeriepenger().isEmpty()) {
            return Optional.empty();
        }
        BeregningsresultatFeriepenger feriepenger = entitet.getBeregningsresultatFeriepenger().get();
        FeriepengegrunnlagDto.Builder builder = FeriepengegrunnlagDto.builder()
            .medFeriepengeperiodeFom(feriepenger.getFeriepengerPeriodeFom())
            .medFeriepengeperiodeTom(feriepenger.getFeriepengerPeriodeTom());
        feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .map(FeriepengegrunnlagMapper::mapAndel)
            .forEach(builder::leggTilAndel);
        return Optional.of(builder.build());

    }

    private static FeriepengegrunnlagAndelDto mapAndel(BeregningsresultatFeriepengerPrÅr prÅr) {
        BeregningsresultatAndel andel = prÅr.getBeregningsresultatAndel();
        return FeriepengegrunnlagAndelDto.builder()
            .medOpptjeningsår(prÅr.getOpptjeningsåret())
            .medÅrsbeløp(prÅr.getÅrsbeløp().getVerdi())
            .medAktivitetStatus(andel.getAktivitetStatus())
            .medArbeidsgiverId(andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null))
            .medArbeidsforholdId(andel.getArbeidsforholdRef().getReferanse())
            .medErBrukerMottaker(andel.erBrukerMottaker())
            .medYtelseperiodeFom(andel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeFom())
            .medYtelseperiodeTom(andel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeTom())
            .build();
    }
}
