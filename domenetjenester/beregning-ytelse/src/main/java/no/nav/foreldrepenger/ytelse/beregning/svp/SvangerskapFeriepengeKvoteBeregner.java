package no.nav.foreldrepenger.ytelse.beregning.svp;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.util.Virkedager;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;

public class SvangerskapFeriepengeKvoteBeregner {
    private static final int SVP_FERIE_KVOTE = 64;
    private static final Set<Inntektskategori> KATEGORIER_MED_FERIEPENGER = Set.of(Inntektskategori.ARBEIDSTAKER, Inntektskategori.SJØMANN);

    public static Optional<Integer> beregn(BeregningsresultatEntitet beregnetYtelse, List<BeregningsresultatEntitet> annenTilkjentYtelsePåSammeSvangerskap) {
        var førsteDagMedFeriepengerOpt = finnFørsteDagSomGirFeriepenger(beregnetYtelse);
        if (førsteDagMedFeriepengerOpt.isEmpty()) {
            return Optional.empty();
        }
        var førsteDagMedFeriepenger = førsteDagMedFeriepengerOpt.get();
        var brukteFeriedager = annenTilkjentYtelsePåSammeSvangerskap.stream().mapToInt(ty -> finnBrukteFeriepengedager(ty, førsteDagMedFeriepenger)).sum();
        if (brukteFeriedager >= SVP_FERIE_KVOTE) {
            return Optional.of(0);
        }
        return Optional.of(SVP_FERIE_KVOTE - brukteFeriedager);
    }

    private static Integer finnBrukteFeriepengedager(BeregningsresultatEntitet tilkjentytelse, LocalDate førsteDagMedFeriepenger) {
        var feriepengetidslinjeOpt = lagFeriepengeperiode(tilkjentytelse.getBeregningsresultatFeriepenger());
        if (feriepengetidslinjeOpt.isEmpty()) {
            return 0;
        }
        var intervallerSomKvalifisererTilFeriepenger = lagTilkjentYtelseFerieTidslinje(tilkjentytelse);
        var perioderMedFeriepengerBeregnet = intervallerSomKvalifisererTilFeriepenger.stream()
            .map(inter -> inter.overlap(feriepengetidslinjeOpt.get()))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
        var periodeSomKanPåvirkeNyFeriepengeberegning = new LocalDateInterval(Tid.TIDENES_BEGYNNELSE, førsteDagMedFeriepenger.minusDays(1));
        var perioderSomTrekkerFraFeriekvote = perioderMedFeriepengerBeregnet.stream()
            .map(inter -> inter.overlap(periodeSomKanPåvirkeNyFeriepengeberegning))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
        return perioderSomTrekkerFraFeriekvote.stream().mapToInt(p -> Virkedager.beregnAntallVirkedager(p.getFomDato(), p.getTomDato())).sum();
    }

    private static List<LocalDateInterval> lagTilkjentYtelseFerieTidslinje(BeregningsresultatEntitet tilkjentytelse) {
        return tilkjentytelse.getBeregningsresultatPerioder()
            .stream()
            .filter(p -> finnesAndelMedKravPåFeriepengerOgUtbetaling(p.getBeregningsresultatAndelList()))
            .map(SvangerskapFeriepengeKvoteBeregner::lagIntervall)
            .collect(Collectors.toList());
    }

    private static Optional<LocalDateInterval> lagFeriepengeperiode(Optional<BeregningsresultatFeriepenger> beregningsresultatFeriepenger) {
        var fom = beregningsresultatFeriepenger.map(BeregningsresultatFeriepenger::getFeriepengerPeriodeFom);
        var tom = beregningsresultatFeriepenger.map(BeregningsresultatFeriepenger::getFeriepengerPeriodeTom);
        if (fom.isEmpty() || tom.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new LocalDateInterval(fom.get(), tom.get()));
    }

    private static LocalDateInterval lagIntervall(BeregningsresultatPeriode p) {

        return new LocalDateInterval(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom());
    }

    private static Optional<LocalDate> finnFørsteDagSomGirFeriepenger(BeregningsresultatEntitet beregnetYtelse) {
        return beregnetYtelse.getBeregningsresultatPerioder().stream()
            .filter(p -> finnesAndelMedKravPåFeriepengerOgUtbetaling(p.getBeregningsresultatAndelList()))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(LocalDate::compareTo);
    }

    private static boolean finnesAndelMedKravPåFeriepengerOgUtbetaling(List<BeregningsresultatAndel> andeler) {
        return andeler.stream().filter(andel -> KATEGORIER_MED_FERIEPENGER.contains(andel.getInntektskategori())).anyMatch(andel -> andel.getDagsats() > 0);

    }
}
