package no.nav.foreldrepenger.ytelse.beregning.svp;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.ytelse.beregning.Feriepengedager;
import no.nav.foreldrepenger.ytelse.beregning.Virkedager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;

public class SvangerskapFeriepengeKvoteBeregner {
    private static final int SVP_FERIEDAGER = Feriepengedager.forYtelse(FagsakYtelseType.SVANGERSKAPSPENGER);

    private SvangerskapFeriepengeKvoteBeregner() {
    }

    public static Optional<Integer> beregn(BeregningsresultatEntitet beregnetYtelse, List<BehandlingBeregningsresultatEntitet> annenTilkjentYtelsePåSammeSvangerskap) {
        var førsteDagMedFeriepengerOpt = finnFørsteDagSomGirFeriepenger(beregnetYtelse);
        if (førsteDagMedFeriepengerOpt.isEmpty()) {
            return Optional.empty();
        }
        var førsteDagMedFeriepenger = førsteDagMedFeriepengerOpt.get();
        var brukteFeriedager = annenTilkjentYtelsePåSammeSvangerskap.stream().mapToInt(ty -> finnBrukteFeriepengedager(ty, førsteDagMedFeriepenger)).sum();
        if (brukteFeriedager > SVP_FERIEDAGER) {
            throw new IllegalStateException("Brukte feriedager overstiger kvote! Tidligere saker må revurderes først. Brukte feriedager var " + brukteFeriedager);
        }
        return Optional.of(SVP_FERIEDAGER - brukteFeriedager);
    }

    private static Integer finnBrukteFeriepengedager(BehandlingBeregningsresultatEntitet tilkjentytelse, LocalDate førsteDagMedFeriepenger) {
        var feriepengetidslinjeOpt = tilkjentytelse.getBeregningsresultatFeriepenger().flatMap(SvangerskapFeriepengeKvoteBeregner::lagFeriepengeperiode);
        if (feriepengetidslinjeOpt.isEmpty()) {
            return 0;
        }
        var intervallerSomKvalifisererTilFeriepenger = lagTilkjentYtelseFerieTidslinje(tilkjentytelse.getGjeldendePerioder());
        var perioderMedFeriepengerBeregnet = intervallerSomKvalifisererTilFeriepenger.stream()
            .map(inter -> inter.overlap(feriepengetidslinjeOpt.get()))
            .flatMap(Optional::stream)
            .toList();
        var periodeSomKanPåvirkeNyFeriepengeberegning = new LocalDateInterval(Tid.TIDENES_BEGYNNELSE, førsteDagMedFeriepenger.minusDays(1));
        var perioderSomTrekkerFraFeriekvote = perioderMedFeriepengerBeregnet.stream()
            .map(inter -> inter.overlap(periodeSomKanPåvirkeNyFeriepengeberegning))
            .flatMap(Optional::stream)
            .toList();
        return perioderSomTrekkerFraFeriekvote.stream().mapToInt(p -> Virkedager.beregnAntallVirkedager(p.getFomDato(), p.getTomDato())).sum();
    }

    private static List<LocalDateInterval> lagTilkjentYtelseFerieTidslinje(List<BeregningsresultatPeriode> tilkjentytelse) {
        return tilkjentytelse.stream()
            .filter(p -> finnesAndelMedKravPåFeriepengerOgUtbetaling(p.getBeregningsresultatAndelList()))
            .map(SvangerskapFeriepengeKvoteBeregner::lagIntervall)
            .toList();
    }

    private static Optional<LocalDateInterval> lagFeriepengeperiode(BeregningsresultatFeriepenger beregningsresultatFeriepenger) {
        if (beregningsresultatFeriepenger.getFeriepengerPeriodeFom() == null || beregningsresultatFeriepenger.getFeriepengerPeriodeTom() == null) {
            return Optional.empty();
        }
        return Optional.of(new LocalDateInterval(beregningsresultatFeriepenger.getFeriepengerPeriodeFom(), beregningsresultatFeriepenger.getFeriepengerPeriodeTom()));
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
        return andeler.stream().filter(andel -> Inntektskategori.girFeriepenger().contains(andel.getInntektskategori())).anyMatch(andel -> andel.getDagsats() > 0);

    }
}
