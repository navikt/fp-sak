package no.nav.foreldrepenger.ytelse.beregning.adapter;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerResultat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SammenlignBeregningsresultatFeriepengerMedRegelResultat {

    private static final int AKSEPTERT_AVVIK = 4;

    private static final String BRUKER = "Bruker";
    private static final String ARBGIVER = "ArbGiv";

    private SammenlignBeregningsresultatFeriepengerMedRegelResultat() {
        // unused
    }

    public static boolean erAvvik(BeregningsresultatEntitet resultat, BeregningsresultatFeriepengerResultat feriepengerResultat) {

        if (feriepengerResultat.feriepengerPeriode() == null) {
            var tilkjent = resultat.getBeregningsresultatFeriepenger()
                .map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe).orElse(List.of()).stream()
                .map(BeregningsresultatFeriepengerPrÅr::getÅrsbeløp)
                .reduce(new Beløp(BigDecimal.ZERO), Beløp::adder);
            return Math.abs(tilkjent.getVerdi().longValue()) > AKSEPTERT_AVVIK;
        }

        var andelerFraRegelKjøring =
            feriepengerResultat.beregningsresultatPerioder().stream()
                .flatMap(periode -> periode.getBeregningsresultatAndelList().stream())
                .flatMap(andel -> andel.getBeregningsresultatFeriepengerPrÅrListe().stream())
                .filter(SammenlignBeregningsresultatFeriepengerMedRegelResultat::erAvrundetÅrsbeløpUlik0)
                .toList();

        return sammenlignFeriepengeandelerHarAvvik(andelerFraRegelKjøring,
            resultat.getBeregningsresultatFeriepenger().map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe).orElse(List.of()));
    }

    private static boolean erAvrundetÅrsbeløpUlik0(no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr prÅr) {
        var årsbeløp = prÅr.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP).longValue();
        return årsbeløp != 0L;
    }

    private static boolean sammenlignFeriepengeandelerHarAvvik(List<no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr> nyeAndeler,
                                                               List<BeregningsresultatFeriepengerPrÅr> gjeldendeAndeler) {
        var simulert = sorterteTilkjenteRegelFeriepenger(nyeAndeler);
        var tilkjent = sorterteTilkjenteFeriepenger(gjeldendeAndeler);

        Map<AndelGruppering, BigDecimal> summert = new LinkedHashMap<>();
        tilkjent.forEach((key, value) -> summert.put(key, value.getVerdi()));
        simulert.forEach((key, value) -> summert.put(key, summert.getOrDefault(key, BigDecimal.ZERO).subtract(value.getVerdi())));

        return summert.values().stream().anyMatch(SammenlignBeregningsresultatFeriepengerMedRegelResultat::erAvvik);
    }

    private static boolean erAvvik(BigDecimal diff) {
        return Math.abs(diff.longValue()) > AKSEPTERT_AVVIK;
    }

    private static Map<AndelGruppering, Beløp> sorterteTilkjenteFeriepenger(List<BeregningsresultatFeriepengerPrÅr> feriepenger) {
        return feriepenger.stream()
            .collect(Collectors.groupingBy(AndelGruppering::fraBeregningEntitet,
                Collectors.reducing(new Beløp(BigDecimal.ZERO), BeregningsresultatFeriepengerPrÅr::getÅrsbeløp, Beløp::adder)));
    }

    private static Map<AndelGruppering, Beløp> sorterteTilkjenteRegelFeriepenger(List<no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr> feriepenger) {
        return feriepenger.stream()
            .collect(Collectors.groupingBy(AndelGruppering::fraBeregningRegel,
                Collectors.reducing(new Beløp(BigDecimal.ZERO), prÅr -> new Beløp(prÅr.getÅrsbeløp()), Beløp::adder)));
    }

    private record AndelGruppering(Year opptjent, String mottaker) {

        static AndelGruppering fraBeregningEntitet(BeregningsresultatFeriepengerPrÅr andel) {
            return new AndelGruppering(Year.from(andel.getOpptjeningsår()),
                andel.getBeregningsresultatAndel().erBrukerMottaker() ? BRUKER :
                    andel.getBeregningsresultatAndel().getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(ARBGIVER));
        }

        static AndelGruppering fraBeregningRegel(no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr andel) {
            return new AndelGruppering(Year.from(andel.getOpptjeningÅr()),
                andel.erBrukerMottaker() ? BRUKER : Optional.ofNullable(andel.getArbeidsgiverId()).orElse(ARBGIVER));
        }
    }
}
