package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

public class SammenlignBeregningsresultatFeriepengerMedRegelResultat {

    private static final Logger LOG = LoggerFactory.getLogger(SammenlignBeregningsresultatFeriepengerMedRegelResultat.class);

    private static final int AKSEPTERT_AVVIK = 3;

    private static final String AVVIK_KODE = "FP-110713";
    private static final String BRUKER = "Bruker";
    private static final String ARBGIVER = "ArbGiv";

    private SammenlignBeregningsresultatFeriepengerMedRegelResultat() {
        // unused
    }

    public static boolean erAvvik(BeregningsresultatEntitet resultat, BeregningsresultatFeriepengerRegelModell regelModell) {

        if (regelModell.getFeriepengerPeriode() == null) {
            // Lagrer sporing
            var tilkjent = resultat.getBeregningsresultatFeriepenger()
                .map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe).orElse(List.of()).stream()
                .map(BeregningsresultatFeriepengerPrÅr::getÅrsbeløp)
                .reduce(new Beløp(BigDecimal.ZERO), Beløp::adder);
            return Math.abs(tilkjent.getVerdi().longValue()) > AKSEPTERT_AVVIK;
        }

        List<no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr> andelerFraRegelKjøring =
            regelModell.getBeregningsresultatPerioder().stream()
                .flatMap(periode -> periode.getBeregningsresultatAndelList().stream())
                .flatMap(andel -> andel.getBeregningsresultatFeriepengerPrÅrListe().stream())
                .filter(SammenlignBeregningsresultatFeriepengerMedRegelResultat::erAvrundetÅrsbeløpUlik0)
                .collect(Collectors.toList());

        return sammenlignFeriepengeandeler(andelerFraRegelKjøring,
            resultat.getBeregningsresultatFeriepenger().map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe).orElse(List.of()));
    }

    public static boolean loggAvvik(Saksnummer saksnummer, Long behandlingId, BeregningsresultatEntitet resultat, BeregningsresultatFeriepengerRegelModell regelModell) {

        if (regelModell.getFeriepengerPeriode() == null) {
            // Lagrer sporing
            var tilkjent = resultat.getBeregningsresultatFeriepenger()
                .map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe).orElse(List.of()).stream()
                .map(BeregningsresultatFeriepengerPrÅr::getÅrsbeløp)
                .reduce(new Beløp(BigDecimal.ZERO), Beløp::adder);
            return Math.abs(tilkjent.getVerdi().longValue()) > AKSEPTERT_AVVIK;
        }

        List<no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr> andelerFraRegelKjøring =
            regelModell.getBeregningsresultatPerioder().stream()
                .flatMap(periode -> periode.getBeregningsresultatAndelList().stream())
                .flatMap(andel -> andel.getBeregningsresultatFeriepengerPrÅrListe().stream())
                .filter(SammenlignBeregningsresultatFeriepengerMedRegelResultat::erAvrundetÅrsbeløpUlik0)
                .collect(Collectors.toList());

        return sammenlignFeriepengerLogg(saksnummer, behandlingId, andelerFraRegelKjøring,
            resultat.getBeregningsresultatFeriepenger().map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe).orElse(List.of()));
    }

    private static boolean erAvrundetÅrsbeløpUlik0(no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr prÅr) {
        long årsbeløp = prÅr.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP).longValue();
        return årsbeløp != 0L;
    }

    private static boolean sammenlignFeriepengeandeler(List<no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr> nyeAndeler,
                                                List<BeregningsresultatFeriepengerPrÅr> gjeldendeAndeler) {
        var simulert = sorterteTilkjenteRegelFeriepenger(nyeAndeler);
        var tilkjent = sorterteTilkjenteFeriepenger(gjeldendeAndeler);

        Map<AndelGruppering, BigDecimal> summert = new LinkedHashMap<>();
        tilkjent.forEach((key, value) -> summert.put(key, value.getVerdi()));
        simulert.forEach((key, value) -> summert.put(key, summert.getOrDefault(key, BigDecimal.ZERO).subtract(value.getVerdi())));

        return summert.values().stream().anyMatch(SammenlignBeregningsresultatFeriepengerMedRegelResultat::erAvvik);
    }

    private static boolean sammenlignFeriepengerLogg(Saksnummer saksnummer, Long behandlingId,
                                                     List<no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr> nyeAndeler,
                                                     List<BeregningsresultatFeriepengerPrÅr> gjeldendeAndeler) {
        var simulert = sorterteTilkjenteRegelFeriepenger(nyeAndeler);
        var tilkjent = sorterteTilkjenteFeriepenger(gjeldendeAndeler);

        Map<AndelGruppering, BigDecimal> summert = new LinkedHashMap<>();
        tilkjent.forEach((key, value) -> summert.put(key, value.getVerdi()));
        simulert.forEach((key, value) -> summert.put(key, summert.getOrDefault(key, BigDecimal.ZERO).subtract(value.getVerdi())));

        Map<Year, BigDecimal> summertÅr = new LinkedHashMap<>();
        tilkjent.forEach((key,value) -> summertÅr.put(key.getOpptjent(), summertÅr.getOrDefault(key.getOpptjent(), BigDecimal.ZERO).add(value.getVerdi())));
        simulert.forEach((key,value) -> summertÅr.put(key.getOpptjent(), summertÅr.getOrDefault(key.getOpptjent(), BigDecimal.ZERO).subtract(value.getVerdi())));

        summert.entrySet().stream()
            .filter(e -> erAvvik(e.getValue()))
            .forEach(e -> LOG.info("{} andel {} saksnummer {} behandling {} år {} mottaker {} diff {} gammel {} ny {}",
                AVVIK_KODE, erAvvik(summertÅr.get(e.getKey().getOpptjent())) ? "tilkjent-simulert" : "omfordelt",
                saksnummer, behandlingId, e.getKey().getOpptjent(), e.getKey().getMottaker(), e.getValue().longValue(),
                tilkjent.getOrDefault(e.getKey(), Beløp.ZERO).getVerdi().longValue(),
                simulert.getOrDefault(e.getKey(), Beløp.ZERO).getVerdi().longValue()));

        return summert.values().stream().anyMatch(SammenlignBeregningsresultatFeriepengerMedRegelResultat::erAvvik);
    }

    private static boolean erAvvik(BigDecimal diff) {
        return Math.abs(diff.longValue()) > AKSEPTERT_AVVIK;
    }

    private static Map<AndelGruppering, Beløp> sorterteTilkjenteFeriepenger(List<BeregningsresultatFeriepengerPrÅr> feriepenger) {
        return feriepenger.stream()
            .collect(Collectors.groupingBy(AndelGruppering::new,
                Collectors.reducing(new Beløp(BigDecimal.ZERO), BeregningsresultatFeriepengerPrÅr::getÅrsbeløp, Beløp::adder)));
    }

    private static Map<AndelGruppering, Beløp> sorterteTilkjenteRegelFeriepenger(List<no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr> feriepenger) {
        return feriepenger.stream()
            .collect(Collectors.groupingBy(AndelGruppering::new,
                Collectors.reducing(new Beløp(BigDecimal.ZERO), prÅr -> new Beløp(prÅr.getÅrsbeløp()), Beløp::adder)));
    }

    private static class AndelGruppering {
        Year opptjent;
        String mottaker;

        AndelGruppering(BeregningsresultatFeriepengerPrÅr andel) {
            this.opptjent = Year.from(andel.getOpptjeningsår());
            this.mottaker = andel.getBeregningsresultatAndel().erBrukerMottaker() ? BRUKER :
                andel.getBeregningsresultatAndel().getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(ARBGIVER);
        }

        AndelGruppering(no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr andel) {
            this.opptjent = Year.from(andel.getOpptjeningÅr());
            this.mottaker = andel.getBeregningsresultatAndel().erBrukerMottaker() ? BRUKER :
                Optional.ofNullable(andel.getBeregningsresultatAndel().getArbeidsgiverId()).orElse(ARBGIVER);
        }

        public Year getOpptjent() {
            return opptjent;
        }

        public String getMottaker() {
            return mottaker;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AndelGruppering that = (AndelGruppering) o;
            return Objects.equals(opptjent, that.opptjent) && Objects.equals(mottaker, that.mottaker);
        }

        @Override
        public int hashCode() {
            return Objects.hash(opptjent, mottaker);
        }

        @Override
        public String toString() {
            return "AndelGruppering{" +
                "år=" + opptjent +
                ", mottaker='" + mottaker + '\'' +
                '}';
        }
    }
}
