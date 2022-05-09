package no.nav.foreldrepenger.ytelse.beregning.svp;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.util.Virkedager;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class SvangerskapFeriepengeKvoteBeregner {
    private int svpFerieKvote;
    private static final Set<Inntektskategori> KATEGORIER_MED_FERIEPENGER = Set.of(Inntektskategori.ARBEIDSTAKER, Inntektskategori.SJØMANN);

    public SvangerskapFeriepengeKvoteBeregner() {
        // CDI
    }

    @Inject
    public SvangerskapFeriepengeKvoteBeregner(@KonfigVerdi(value = "svp.antall.dager.feriepenger", defaultVerdi = "64") int antallDagerFeriepenger) {
        this.svpFerieKvote = antallDagerFeriepenger;
    }


    public Optional<Integer> beregn(BeregningsresultatEntitet beregnetYtelse, List<BeregningsresultatEntitet> annenTilkjentYtelsePåSammeSvangerskap) {
        var førsteDagMedFeriepengerOpt = finnFørsteDagSomGirFeriepenger(beregnetYtelse);
        if (førsteDagMedFeriepengerOpt.isEmpty()) {
            return Optional.empty();
        }
        var førsteDagMedFeriepenger = førsteDagMedFeriepengerOpt.get();
        var brukteFeriedager = annenTilkjentYtelsePåSammeSvangerskap.stream().mapToInt(ty -> finnBrukteFeriepengedager(ty, førsteDagMedFeriepenger)).sum();
        if (brukteFeriedager > svpFerieKvote) {
            throw new IllegalStateException("Brukte feriedager overstiger kvote! Tidligere saker må revurderes først. Brukte feriedager var " + brukteFeriedager);
        }
        return Optional.of(svpFerieKvote - brukteFeriedager);
    }

    private Integer finnBrukteFeriepengedager(BeregningsresultatEntitet tilkjentytelse, LocalDate førsteDagMedFeriepenger) {
        var feriepengetidslinjeOpt = tilkjentytelse.getBeregningsresultatFeriepenger().flatMap(this::lagFeriepengeperiode);
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

    private List<LocalDateInterval> lagTilkjentYtelseFerieTidslinje(BeregningsresultatEntitet tilkjentytelse) {
        return tilkjentytelse.getBeregningsresultatPerioder()
            .stream()
            .filter(p -> finnesAndelMedKravPåFeriepengerOgUtbetaling(p.getBeregningsresultatAndelList()))
            .map(this::lagIntervall)
            .collect(Collectors.toList());
    }

    private Optional<LocalDateInterval> lagFeriepengeperiode(BeregningsresultatFeriepenger beregningsresultatFeriepenger) {
        if (beregningsresultatFeriepenger.getFeriepengerPeriodeFom() == null || beregningsresultatFeriepenger.getFeriepengerPeriodeTom() == null) {
            return Optional.empty();
        }
        return Optional.of(new LocalDateInterval(beregningsresultatFeriepenger.getFeriepengerPeriodeFom(), beregningsresultatFeriepenger.getFeriepengerPeriodeTom()));
    }

    private LocalDateInterval lagIntervall(BeregningsresultatPeriode p) {

        return new LocalDateInterval(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom());
    }

    private Optional<LocalDate> finnFørsteDagSomGirFeriepenger(BeregningsresultatEntitet beregnetYtelse) {
        return beregnetYtelse.getBeregningsresultatPerioder().stream()
            .filter(p -> finnesAndelMedKravPåFeriepengerOgUtbetaling(p.getBeregningsresultatAndelList()))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(LocalDate::compareTo);
    }

    private boolean finnesAndelMedKravPåFeriepengerOgUtbetaling(List<BeregningsresultatAndel> andeler) {
        return andeler.stream().filter(andel -> KATEGORIER_MED_FERIEPENGER.contains(andel.getInntektskategori())).anyMatch(andel -> andel.getDagsats() > 0);

    }
}
