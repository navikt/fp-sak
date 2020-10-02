package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.adapter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjenteFeriepenger;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjenteFeriepengerPrÅr;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class TilkjentYtelseMapper {

    private FamilieYtelseType familieYtelseType;

    public TilkjentYtelseMapper(FamilieYtelseType familieYtelseType) {
        this.familieYtelseType = familieYtelseType;
    }

    public TilkjentYtelse map(BeregningsresultatEntitet beregningsresultat) {
        TilkjentYtelse tilkjentYtelseFP = opprettForenkletBeregningsresultatFP(beregningsresultat);
        mapPerioder(beregningsresultat, tilkjentYtelseFP);

        return tilkjentYtelseFP;
    }

    public List<TilkjentYtelsePeriode> mapPerioderFomEndringsdato(BeregningsresultatEntitet beregningsresultat) {
        List<BeregningsresultatPeriode> brPeriodeListe = sortBeregningsresultatPerioder(beregningsresultat.getBeregningsresultatPerioder());
        LocalDate endringsdato = utledEndringsdato(beregningsresultat, brPeriodeListe);
        List<BeregningsresultatPeriode> brPerioderFomEndringsdatoListe = getBeregningsresultatPerioderFomEndringsdato(endringsdato, brPeriodeListe);

        return opprettOppdragPerioderFomEndringsdato(endringsdato, brPerioderFomEndringsdatoListe, null);
    }

    private void mapPerioder(BeregningsresultatEntitet beregningsresultat, TilkjentYtelse tilkjentYtelseFP) {
        List<BeregningsresultatPeriode> brPeriodeListe = sortBeregningsresultatPerioder(beregningsresultat.getBeregningsresultatPerioder());
        if (!brPeriodeListe.isEmpty()) {
            // vi ønsker å hente første BeregningsresultatPeriode.fom
            LocalDate endringsdato = brPeriodeListe.get(0).getBeregningsresultatPeriodeFom();
            opprettOppdragPerioderFomEndringsdato(endringsdato, brPeriodeListe, tilkjentYtelseFP);
        }
    }

    private TilkjentYtelse opprettForenkletBeregningsresultatFP(BeregningsresultatEntitet beregningsresultat) {
        TilkjentYtelse.Builder builder = TilkjentYtelse.builder();
        beregningsresultat.getEndringsdato()
            .ifPresent(builder::medEndringsdato);
        return builder.build();
    }

    private List<TilkjentYtelsePeriode> opprettOppdragPerioderFomEndringsdato(LocalDate endringsdato, List<BeregningsresultatPeriode> brPerioderFomEndringsdatoListe,
                                                                              TilkjentYtelse tilkjentYtelseFP) {
        List<TilkjentYtelsePeriode> tilkjentYtelsePeriodeFPListe = new ArrayList<>();

        if (tilkjentYtelseFP != null && finnesFeriepenger(brPerioderFomEndringsdatoListe)) {
            buildOppdragFeriepenger(tilkjentYtelseFP);
        }
        for (BeregningsresultatPeriode brPeriode : brPerioderFomEndringsdatoListe) {
            TilkjentYtelsePeriode tilkjentYtelsePeriodeFP = opprettOppdragPeriode(brPeriode, endringsdato, tilkjentYtelseFP);
            tilkjentYtelsePeriodeFPListe.add(tilkjentYtelsePeriodeFP);
        }
        return tilkjentYtelsePeriodeFPListe;
    }

    private TilkjentYtelsePeriode opprettOppdragPeriode(BeregningsresultatPeriode brPeriode, LocalDate endringsdato,
                                                        TilkjentYtelse tilkjentYtelseFP) {

        LocalDate fom = finnStartDatoForOppdragPeriode(brPeriode, endringsdato);

        TilkjentYtelsePeriode.Builder oppdragPeriodeBuilder = TilkjentYtelsePeriode.builder()
            .medPeriode(new LocalDateInterval(fom, brPeriode.getBeregningsresultatPeriodeTom()));

        TilkjentYtelsePeriode tilkjentYtelsePeriodeFP = tilkjentYtelseFP == null ? oppdragPeriodeBuilder.build()
            : oppdragPeriodeBuilder.build(tilkjentYtelseFP);

        Optional<TilkjenteFeriepenger> oppdragFeriepengerOpt = tilkjentYtelseFP == null ? Optional.empty()
            : tilkjentYtelseFP.getTilkjenteFeriepenger();

        List<BeregningsresultatAndel> brAndelListe = brPeriode.getBeregningsresultatAndelList();
        brAndelListe.forEach(brAndel ->
            buildOppdragAndel(brAndel, tilkjentYtelsePeriodeFP, oppdragFeriepengerOpt));

        return tilkjentYtelsePeriodeFP;
    }

    private void buildOppdragAndel(BeregningsresultatAndel brAndel, TilkjentYtelsePeriode tilkjentYtelsePeriodeFP,
                                   Optional<TilkjenteFeriepenger> oppdragFeriepengerOpt) {
        TilkjentYtelseAndel.Builder builder = TilkjentYtelseAndel.builder()
            .medBrukerErMottaker(brAndel.erBrukerMottaker())
            .medFamilieYtelseType(familieYtelseType)
            .medDagsats(brAndel.getDagsats())
            .medInntektskategori(brAndel.getInntektskategori())
            .medUtbetalingsgrad(brAndel.getUtbetalingsgrad());
        brAndel.getArbeidsgiver()
            .ifPresent(builder::medArbeidsgiver);
        TilkjentYtelseAndel tilkjentYtelseAndel = builder.build(tilkjentYtelsePeriodeFP);

        oppdragFeriepengerOpt.ifPresent(oppdragFeriepenger ->
            brAndel.getBeregningsresultatFeriepengerPrÅrListe().forEach(brFeriepengerPrÅr ->
                buildOppdragFeriepengerPrÅr(brFeriepengerPrÅr, tilkjentYtelseAndel, oppdragFeriepenger)));
    }

    private static TilkjenteFeriepenger buildOppdragFeriepenger(TilkjentYtelse tilkjentYtelseFP) {
        return TilkjenteFeriepenger.builder()
            .build(tilkjentYtelseFP);
    }

    private static TilkjenteFeriepengerPrÅr buildOppdragFeriepengerPrÅr(BeregningsresultatFeriepengerPrÅr brFeriepengerPrÅr,
                                                                        TilkjentYtelseAndel tilkjentYtelseAndel,
                                                                        TilkjenteFeriepenger tilkjenteFeriepenger) {
        return TilkjenteFeriepengerPrÅr.builder()
            .medOpptjeningÅr(brFeriepengerPrÅr.getOpptjeningsår())
            .medÅrsbeløp(brFeriepengerPrÅr.getÅrsbeløp())
            .build(tilkjenteFeriepenger, tilkjentYtelseAndel);
    }

    private static boolean finnesFeriepenger(List<BeregningsresultatPeriode> brPerioderFomEndringsdatoListe) {
        return brPerioderFomEndringsdatoListe.stream()
            .flatMap(periode -> periode.getBeregningsresultatAndelList().stream())
            .anyMatch(andel -> !andel.getBeregningsresultatFeriepengerPrÅrListe().isEmpty());
    }

    private static LocalDate utledEndringsdato(BeregningsresultatEntitet beregningsresultat, List<BeregningsresultatPeriode> brPeriodeListe) {
        // vi ønsker å hente første BeregningsresultatPeriode.fom
        LocalDate førstePeriodeFom = brPeriodeListe.get(0).getBeregningsresultatPeriodeFom();
        return beregningsresultat.getEndringsdato()
            .orElse(førstePeriodeFom);
    }

    private static List<BeregningsresultatPeriode> sortBeregningsresultatPerioder(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        return beregningsresultatPerioder.stream()
            .sorted(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            .collect(Collectors.toList());
    }

    private static LocalDate finnStartDatoForOppdragPeriode(BeregningsresultatPeriode brPeriode, LocalDate endringsdato) {
        if (brPeriode.getBeregningsresultatPeriodeFom().isBefore(endringsdato)) {
            return endringsdato;
        }
        return brPeriode.getBeregningsresultatPeriodeFom();
    }

    private static List<BeregningsresultatPeriode> getBeregningsresultatPerioderFomEndringsdato(LocalDate endringsdato, List<BeregningsresultatPeriode> brPeriodeListe) {

        List<BeregningsresultatPeriode> brPerioderFomEndringsdatoListe = new ArrayList<>();
        for (BeregningsresultatPeriode brPeriode : brPeriodeListe) {
            if (!brPeriode.getBeregningsresultatPeriodeTom().isBefore(endringsdato)) {
                brPerioderFomEndringsdatoListe.add(brPeriode);
            }
        }
        return brPerioderFomEndringsdatoListe;
    }
}
