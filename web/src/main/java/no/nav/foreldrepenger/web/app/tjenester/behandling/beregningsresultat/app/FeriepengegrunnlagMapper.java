package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app;

import java.util.Collections;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.FeriepengegrunnlagAndelDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.FeriepengegrunnlagDto;

public final class FeriepengegrunnlagMapper {

    private FeriepengegrunnlagMapper() {
        // Skjuler default konstruktør
    }

    public static Optional<FeriepengegrunnlagDto> map(BeregningsresultatEntitet entitet) {
        var feriepengerPrÅr = entitet.getBeregningsresultatFeriepenger()
            .map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe)
            .orElse(Collections.emptyList());
        if (feriepengerPrÅr.isEmpty()) {
            return Optional.empty();
        }
        var feriepenger = entitet.getBeregningsresultatFeriepenger().orElseThrow();
        var builder = FeriepengegrunnlagDto.builder()
            .medFeriepengeperiodeFom(feriepenger.getFeriepengerPeriodeFom())
            .medFeriepengeperiodeTom(feriepenger.getFeriepengerPeriodeTom());
        feriepengerPrÅr.stream()
            .map(FeriepengegrunnlagMapper::mapAndel)
            .forEach(builder::leggTilAndel);
        return Optional.of(builder.build());

    }

    private static FeriepengegrunnlagAndelDto mapAndel(BeregningsresultatFeriepengerPrÅr prÅr) {
        var andel = prÅr.getBeregningsresultatAndel();
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
