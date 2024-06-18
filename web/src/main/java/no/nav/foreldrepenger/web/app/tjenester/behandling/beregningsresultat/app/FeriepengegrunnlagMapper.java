package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.FeriepengegrunnlagAndelDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.FeriepengegrunnlagDto;

public final class FeriepengegrunnlagMapper {

    private FeriepengegrunnlagMapper() {
        // Skjuler default konstruktør
    }

    public static Optional<FeriepengegrunnlagDto> map(List<BeregningsresultatFeriepengerPrÅr> feriepengerPrÅr) {
        if (feriepengerPrÅr.isEmpty()) {
            return Optional.empty();
        }
        var andeler = feriepengerPrÅr.stream()
            .map(FeriepengegrunnlagMapper::mapAndel)
            .toList();
        return Optional.of(new FeriepengegrunnlagDto(andeler));

    }

    private static FeriepengegrunnlagAndelDto mapAndel(BeregningsresultatFeriepengerPrÅr prÅr) {
        return FeriepengegrunnlagAndelDto.builder()
            .medOpptjeningsår(prÅr.getOpptjeningsåret())
            .medÅrsbeløp(prÅr.getÅrsbeløp().getVerdi())
            .medAktivitetStatus(prÅr.getAktivitetStatus())
            .medArbeidsgiverId(prÅr.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null))
            .medArbeidsforholdId(prÅr.getArbeidsforholdRef().getReferanse())
            .medErBrukerMottaker(prÅr.erBrukerMottaker())
            .build();
    }
}
