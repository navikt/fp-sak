package no.nav.foreldrepenger.web.app.tjenester.formidling;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.kontrakter.fpsak.tilkjentytelse.TilkjentYtelseDagytelseDto;

public final class BrevGrunnlagTilkjentYtelseTjeneste {

    private BrevGrunnlagTilkjentYtelseTjeneste() {
        // Skjuler default konstruktør
    }

    public static TilkjentYtelseDagytelseDto mapDagytelse(BeregningsresultatEntitet bgRes) {
        var perioder = bgRes.getBeregningsresultatPerioder()
            .stream()
            .map(BrevGrunnlagTilkjentYtelseTjeneste::mapPeriode)
            .toList();
        return new TilkjentYtelseDagytelseDto(perioder);
    }

    private static TilkjentYtelseDagytelseDto.TilkjentYtelsePeriodeDto mapPeriode(BeregningsresultatPeriode resultatPeriode) {
        var andelMap = resultatPeriode.getBeregningsresultatAndelList()
            .stream()
            .collect(Collectors.groupingBy(BrevGrunnlagTilkjentYtelseTjeneste::genererAndelKey));

        var andeler = andelMap.values().stream().map(BrevGrunnlagTilkjentYtelseTjeneste::mapAndeler).toList();

        return new TilkjentYtelseDagytelseDto.TilkjentYtelsePeriodeDto(resultatPeriode.getBeregningsresultatPeriodeFom(),
            resultatPeriode.getBeregningsresultatPeriodeTom(), resultatPeriode.getDagsats(), andeler);
    }

    private static TilkjentYtelseDagytelseDto.TilkjentYtelseAndelDto mapAndeler(List<BeregningsresultatAndel> andeler) {
        if (andeler.isEmpty()) {
            throw new IllegalArgumentException("Forventet minst en andel å sende til formidling, men fikk ingen");
        }
        // Summerer opp refusjon og direkteutbetaling og legge alt på en andel for enklere representasjon til formidling
        var sumSøker = andeler.stream().filter(BeregningsresultatAndel::erBrukerMottaker).mapToInt(BeregningsresultatAndel::getDagsats).sum();
        var sumRefusjon = andeler.stream().filter(andel -> !andel.erBrukerMottaker()).mapToInt(BeregningsresultatAndel::getDagsats).sum();
        var andel = andeler.get(0);
        return mapAndel(andel, sumSøker, sumRefusjon);
    }

    private static TilkjentYtelseDagytelseDto.TilkjentYtelseAndelDto mapAndel(BeregningsresultatAndel andel, int tilSøker, int refusjon) {
        var arbeidsgiverIdent = andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null);
        var referanse = andel.getArbeidsforholdRef().getReferanse();
        return new TilkjentYtelseDagytelseDto.TilkjentYtelseAndelDto(arbeidsgiverIdent, refusjon, tilSøker,
            mapAktivitetstatus(andel.getAktivitetStatus()), referanse, andel.getStillingsprosent(), andel.getUtbetalingsgrad());
    }

    private static TilkjentYtelseDagytelseDto.Aktivitetstatus mapAktivitetstatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus aktivitetStatus) {
        return switch (aktivitetStatus) {
            case ARBEIDSAVKLARINGSPENGER -> TilkjentYtelseDagytelseDto.Aktivitetstatus.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> TilkjentYtelseDagytelseDto.Aktivitetstatus.ARBEIDSTAKER;
            case DAGPENGER -> TilkjentYtelseDagytelseDto.Aktivitetstatus.DAGPENGER;
            case FRILANSER -> TilkjentYtelseDagytelseDto.Aktivitetstatus.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> TilkjentYtelseDagytelseDto.Aktivitetstatus.MILITÆR_ELLER_SIVIL;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> TilkjentYtelseDagytelseDto.Aktivitetstatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case BRUKERS_ANDEL -> TilkjentYtelseDagytelseDto.Aktivitetstatus.BRUKERS_ANDEL;
            case KUN_YTELSE -> TilkjentYtelseDagytelseDto.Aktivitetstatus.KUN_YTELSE;
            case TTLSTØTENDE_YTELSE -> TilkjentYtelseDagytelseDto.Aktivitetstatus.TTLSTØTENDE_YTELSE;
            case VENTELØNN_VARTPENGER -> TilkjentYtelseDagytelseDto.Aktivitetstatus.VENTELØNN_VARTPENGER;
            case UDEFINERT -> TilkjentYtelseDagytelseDto.Aktivitetstatus.UDEFINERT;
            case KOMBINERT_AT_SN, KOMBINERT_AT_FL_SN, // Burde ikke oppstå for aktivitetstatuser på andeler
                KOMBINERT_AT_FL, KOMBINERT_FL_SN -> throw new IllegalArgumentException("Ugyldig aktivitetstatus " + aktivitetStatus);
        };
    }

    private record AktivitetStatusMedIdentifikator(AktivitetStatus aktivitetStatus, Optional<String> idenfifikator) {}

    private static AktivitetStatusMedIdentifikator genererAndelKey(BeregningsresultatAndel andel) {
        return new AktivitetStatusMedIdentifikator(andel.getAktivitetStatus(), finnSekundærIdentifikator(andel));
    }

    private static Optional<String> finnSekundærIdentifikator(BeregningsresultatAndel andel) {
        if (andel.getArbeidsforholdRef().getReferanse() != null) {
            return Optional.of(andel.getArbeidsforholdRef().getReferanse());
        }
        return andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator);
    }

}
