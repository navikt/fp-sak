package no.nav.foreldrepenger.ytelse.beregning.endringsdato;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatFeriepengerEndringModell;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatFeriepengerPrÅrEndringModell;

import java.time.LocalDate;
import java.time.Year;
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
    public static Optional<LocalDate> finnEndringsdato(Optional<BeregningsresultatFeriepengerEndringModell> originaltResultat,
                                                       Optional<BeregningsresultatFeriepengerEndringModell> revurderingResultat) {
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

    private static Optional<LocalDate> utledEndringsdato(BeregningsresultatFeriepengerEndringModell originaleFeriepenger,
                                                         BeregningsresultatFeriepengerEndringModell revurderingFeriepenger) {
        Set<Year> alleOpptjeningsår = new HashSet<>();
        originaleFeriepenger.getFeriepengerPrÅrListe().forEach(andel -> alleOpptjeningsår.add(andel.getOpptjeningsår()));
        revurderingFeriepenger.getFeriepengerPrÅrListe().forEach(andel -> alleOpptjeningsår.add(andel.getOpptjeningsår()));

        Iterator<Year> iterator = alleOpptjeningsår.iterator();
        List<Year> opptjeningsårMedDiff = new ArrayList<>();
        while (iterator.hasNext()) {
            Year opptjeningsår = iterator.next();
            Beløp originaltGrunnlag = finnGrunnlagForÅr(opptjeningsår, originaleFeriepenger.getFeriepengerPrÅrListe());
            Beløp revurderingGrunnlag = finnGrunnlagForÅr(opptjeningsår, revurderingFeriepenger.getFeriepengerPrÅrListe());
            if (originaltGrunnlag.compareTo(revurderingGrunnlag) != 0) {
                opptjeningsårMedDiff.add(opptjeningsår);
            }
        }
        Optional<Year> førsteÅrMedDiff = opptjeningsårMedDiff.stream().min(Comparator.naturalOrder());
        return førsteÅrMedDiff.flatMap(år -> lagEndringsdatoForOpptjeningsår(år.getValue()));
    }

    private static Beløp finnGrunnlagForÅr(Year opptjeningsår,
                                           List<BeregningsresultatFeriepengerPrÅrEndringModell> andeler) {
        return andeler.stream()
            .filter(andel -> andel.getOpptjeningsår().equals(opptjeningsår))
            .map(BeregningsresultatFeriepengerPrÅrEndringModell::getÅrsbeløp)
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

    private static Optional<LocalDate> finnFørsteUtbetalingsår(BeregningsresultatFeriepengerEndringModell feriepenger) {
        Optional<Year> tidligsteOpptjeningsår = feriepenger.getFeriepengerPrÅrListe().stream()
            .min(Comparator.comparing(BeregningsresultatFeriepengerPrÅrEndringModell::getOpptjeningsår))
            .map(BeregningsresultatFeriepengerPrÅrEndringModell::getOpptjeningsår);
        if (tidligsteOpptjeningsår.isEmpty()) {
            return Optional.empty();
        }
        int opptjeningsår = tidligsteOpptjeningsår.get().getValue();
        return lagEndringsdatoForOpptjeningsår(opptjeningsår);
    }


    private static Optional<LocalDate> lagEndringsdatoForOpptjeningsår(int opptjeningsår) {
        int åretFeriepengeneUtbetales = opptjeningsår + 1;
        // Endringsdato for feriepenger er like 1. mai året etter opptjeningsåret TFP-3956
        LocalDate endringsdato = LocalDate.of(åretFeriepengeneUtbetales, 5, 1);
        return Optional.of(endringsdato);
    }
}
