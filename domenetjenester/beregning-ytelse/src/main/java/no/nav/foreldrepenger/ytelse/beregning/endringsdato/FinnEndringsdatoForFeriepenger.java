package no.nav.foreldrepenger.ytelse.beregning.endringsdato;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.domene.typer.Beløp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class FinnEndringsdatoForFeriepenger {


    private FinnEndringsdatoForFeriepenger(){
        // Skjuler default
    }

    /**
     * Utleder endringsdato for feriepenger, antar at tilkjent ytelse er lik nok til å ikke gi endringsdato og
     * sjekker derfor kun på total feriepenger pr opptjeningsår
     *  @param originaltResultat - Perioder fra revurderingen
     * @param revurderingResultat - Perioder fra førstegangsbehandlingen
     * @return En Optional av type LocalDate hvis endring er funnet
     *         En Optional som er tom hvis ingen endring er funnet.
     */
    public static Optional<LocalDate> finnEndringsdato(Optional<BeregningsresultatFeriepenger> originaltResultat,
                                                       Optional<BeregningsresultatFeriepenger> revurderingResultat) {
        if (originaltResultat.isPresent() && revurderingResultat.isPresent()) {
            return utledEndringsdato(originaltResultat.get(), revurderingResultat.get());
        }
        if (originaltResultat.isEmpty() && revurderingResultat.isEmpty()) {
            return Optional.empty();
        }
        return originaltResultat.isPresent()
            ? finnFørsteUtbetalingsår(originaltResultat.get())
            : finnFørsteUtbetalingsår(revurderingResultat.get());
    }

    private static Optional<LocalDate> utledEndringsdato(BeregningsresultatFeriepenger originaleFeriepenger,
                                                         BeregningsresultatFeriepenger revurderingFeriepenger) {
        Set<LocalDate> alleOpptjeningsår = new HashSet<>();
        originaleFeriepenger.getBeregningsresultatFeriepengerPrÅrListe().forEach(andel -> alleOpptjeningsår.add(andel.getOpptjeningsår()));
        revurderingFeriepenger.getBeregningsresultatFeriepengerPrÅrListe().forEach(andel -> alleOpptjeningsår.add(andel.getOpptjeningsår()));

        Iterator<LocalDate> iterator = alleOpptjeningsår.iterator();
        List<LocalDate> opptjeningsårMedDiff = new ArrayList<>();
        while (iterator.hasNext()) {
            LocalDate opptjeningsår = iterator.next();
            Beløp originaltGrunnlag = finnGrunnlagForÅr(opptjeningsår, originaleFeriepenger.getBeregningsresultatFeriepengerPrÅrListe());
            Beløp revurderingGrunnlag = finnGrunnlagForÅr(opptjeningsår, revurderingFeriepenger.getBeregningsresultatFeriepengerPrÅrListe());
            if (originaltGrunnlag.compareTo(revurderingGrunnlag) != 0) {
                opptjeningsårMedDiff.add(opptjeningsår);
            }
        }
        Optional<LocalDate> førsteÅrMedDiff = opptjeningsårMedDiff.stream().min(Comparator.naturalOrder());
        return førsteÅrMedDiff.flatMap(localDate -> lagEndringsdatoForOpptjeningsår(localDate.getYear()));
    }

    private static Beløp finnGrunnlagForÅr(LocalDate opptjeningsår,
                                           List<BeregningsresultatFeriepengerPrÅr> andeler) {
        return andeler.stream()
            .filter(andel -> andel.getOpptjeningsår().getYear() == opptjeningsår.getYear())
            .map(BeregningsresultatFeriepengerPrÅr::getÅrsbeløp)
            .filter(beløp -> !beløp.erNulltall())
            .reduce(Beløp::adder)
            .orElse(Beløp.ZERO);
    }

    private static Beløp aggregerBeløp(List<BeregningsresultatFeriepengerPrÅr> andeler) {
        return andeler.stream()
            .map(BeregningsresultatFeriepengerPrÅr::getÅrsbeløp)
            .filter(b -> !b.erNulltall())
            .reduce(Beløp::adder)
            .orElse(Beløp.ZERO);
    }

    private static Optional<LocalDate> finnFørsteUtbetalingsår(BeregningsresultatFeriepenger feriepenger) {
        Optional<LocalDate> tidligsteOpptjeningsår = feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .min(Comparator.comparing(BeregningsresultatFeriepengerPrÅr::getOpptjeningsår))
            .map(BeregningsresultatFeriepengerPrÅr::getOpptjeningsår);
        if (tidligsteOpptjeningsår.isEmpty()) {
            return Optional.empty();
        }
        int opptjeningsår = tidligsteOpptjeningsår.get().getYear();
        return lagEndringsdatoForOpptjeningsår(opptjeningsår);
    }


    private static Optional<LocalDate> lagEndringsdatoForOpptjeningsår(int opptjeningsår) {
        int åretFeriepengeneUtbetales = opptjeningsår + 1;
        // Endringsdato for feriepenger er like 1. mai året etter opptjeningsåret TFP-3956
        LocalDate endringsdato = LocalDate.of(åretFeriepengeneUtbetales, 5, 1);
        return Optional.of(endringsdato);
    }
}
