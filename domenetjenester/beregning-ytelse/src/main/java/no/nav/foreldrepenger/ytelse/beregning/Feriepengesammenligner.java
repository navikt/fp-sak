package no.nav.foreldrepenger.ytelse.beregning;

import java.math.BigDecimal;
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

public class Feriepengesammenligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Feriepengesammenligner.class);
    private final long behandlingId;
    private final Saksnummer saksnummer;
    private final BeregningsresultatEntitet nyttResultat;
    private final BeregningsresultatEntitet gjeldendeResultat;
    private static final String AVVIK_KODE = "FP-110711";
    private static final String BRUKER = "Bruker";
    private static final String ARBGIVER = "ArbGiv";


    private boolean finnesAvvik = false;

    public Feriepengesammenligner(long behandlingId,
                                  Saksnummer saksnummer,
                                  BeregningsresultatEntitet nyttResultat,
                                  BeregningsresultatEntitet gjeldendeResultat) {
        this.behandlingId = behandlingId;
        this.saksnummer = saksnummer;
        this.nyttResultat = nyttResultat;
        this.gjeldendeResultat = gjeldendeResultat;
    }

    protected boolean finnesAvvik() {
        Optional<BeregningsresultatFeriepenger> nyttFeriepengegrunnlag = nyttResultat.getBeregningsresultatFeriepenger();
        Optional<BeregningsresultatFeriepenger> gjeldendeFeriepengegrunnlag = gjeldendeResultat.getBeregningsresultatFeriepenger();
        if (nyttFeriepengegrunnlag.isPresent() && gjeldendeFeriepengegrunnlag.isPresent()) {
            sammenlignFeriepengeperiode(nyttFeriepengegrunnlag.get(), gjeldendeFeriepengegrunnlag.get());
            finnesAvvik = sammenlignFeriepengeandeler(nyttFeriepengegrunnlag.get().getBeregningsresultatFeriepengerPrÅrListe(),
                gjeldendeFeriepengegrunnlag.get().getBeregningsresultatFeriepengerPrÅrListe());
        } else if (nyttFeriepengegrunnlag.isPresent() || gjeldendeFeriepengegrunnlag.isPresent()) {
            // Vet at kun en av de er present
            LOGGER.info("{} grunnlag mangler for saksnummer {} behandling {} Gammelt {} Nytt {}",
                AVVIK_KODE, saksnummer.getVerdi(), behandlingId, gjeldendeFeriepengegrunnlag.isPresent(), nyttFeriepengegrunnlag.isPresent());
            return true;
        }
        return finnesAvvik;
    }

    protected boolean finnesAvvikUtenomPeriode() {
        Optional<BeregningsresultatFeriepenger> nyttFeriepengegrunnlag = nyttResultat.getBeregningsresultatFeriepenger();
        Optional<BeregningsresultatFeriepenger> gjeldendeFeriepengegrunnlag = gjeldendeResultat.getBeregningsresultatFeriepenger();
        if (nyttFeriepengegrunnlag.isPresent() && gjeldendeFeriepengegrunnlag.isPresent()) {
            finnesAvvik = sammenlignFeriepengeandeler(nyttFeriepengegrunnlag.get().getBeregningsresultatFeriepengerPrÅrListe(),
                gjeldendeFeriepengegrunnlag.get().getBeregningsresultatFeriepengerPrÅrListe());
        } else if (nyttFeriepengegrunnlag.isPresent() || gjeldendeFeriepengegrunnlag.isPresent()) {
            // Vet at kun en av de er present
            LOGGER.info("{} grunnlag mangler for saksnummer {} behandling {} Gammelt {} Nytt {}",
                AVVIK_KODE, saksnummer.getVerdi(), behandlingId, gjeldendeFeriepengegrunnlag.isPresent(), nyttFeriepengegrunnlag.isPresent());
            return true;
        }
        return finnesAvvik;
    }

    private boolean sammenlignFeriepengeandeler(List<BeregningsresultatFeriepengerPrÅr> nyeAndeler,
                                                List<BeregningsresultatFeriepengerPrÅr> gjeldendeAndeler) {
        var simulert = sorterteTilkjenteFeriepenger(nyeAndeler);
        var tilkjent = sorterteTilkjenteFeriepenger(gjeldendeAndeler);

        Map<AndelGruppering, BigDecimal> summert = new LinkedHashMap<>();
        tilkjent.forEach((key, value) -> summert.put(key, value.getVerdi()));
        simulert.forEach((key, value) -> summert.put(key, summert.getOrDefault(key, BigDecimal.ZERO).subtract(value.getVerdi())));

        Map<Year, BigDecimal> summertÅr = new LinkedHashMap<>();
        tilkjent.forEach((key,value) -> summertÅr.put(key.getOpptjent(), summertÅr.getOrDefault(key.getOpptjent(), BigDecimal.ZERO).add(value.getVerdi())));
        simulert.forEach((key,value) -> summertÅr.put(key.getOpptjent(), summertÅr.getOrDefault(key.getOpptjent(), BigDecimal.ZERO).subtract(value.getVerdi())));

        summertÅr.entrySet().stream()
            .filter(e -> Math.abs(e.getValue().longValue()) > 3)
            .forEach(e -> LOGGER.info("{} årlig tilkjent-simulert saksnummer {} behandling {} år {} diff {}",
                AVVIK_KODE, saksnummer, behandlingId, e.getKey(), e.getValue().longValue()));

        summert.entrySet().stream()
            .filter(e -> Math.abs(e.getValue().longValue()) > 3)
            .forEach(e -> LOGGER.info("{} andel {} saksnummer {} behandling {} år {} mottaker {} diff {} gammel {} ny {}",
                AVVIK_KODE, erAvvik(summertÅr.get(e.getKey().getOpptjent())) ? "tilkjent-simulert" : "omfordelt",
                saksnummer, behandlingId, e.getKey().getOpptjent(), e.getKey().getMottaker(), e.getValue().longValue(),
                tilkjent.getOrDefault(e.getKey(), Beløp.ZERO).getVerdi().longValue(),
                simulert.getOrDefault(e.getKey(), Beløp.ZERO).getVerdi().longValue()));

        return summert.values().stream().anyMatch(Feriepengesammenligner::erAvvik);
    }

    private static boolean erAvvik(BigDecimal diff) {
        return Math.abs(diff.longValue()) > 3;
    }

    private void sammenlignFeriepengeperiode(BeregningsresultatFeriepenger nytt, BeregningsresultatFeriepenger gjeldende) {
        boolean ulikFOM = !Objects.equals(nytt.getFeriepengerPeriodeFom(), gjeldende.getFeriepengerPeriodeFom());
        boolean ulikTOM = !Objects.equals(nytt.getFeriepengerPeriodeTom(), gjeldende.getFeriepengerPeriodeTom());
        if (ulikFOM) {
            LOGGER.info("{} feriepengeperiodeFOM saksnummer {} behandling {} Gammelt {} Nytt {}",
                AVVIK_KODE, saksnummer.getVerdi(), behandlingId, gjeldende.getFeriepengerPeriodeFom(), nytt.getFeriepengerPeriodeFom());
        }
        if (ulikTOM) {
            LOGGER.info("{} feriepengeperiodeTOM saksnummer {} behandling {} Gammelt {} Nytt {}",
                AVVIK_KODE, saksnummer.getVerdi(), behandlingId, gjeldende.getFeriepengerPeriodeTom(), nytt.getFeriepengerPeriodeTom());
        }
    }

    private static Map<AndelGruppering, Beløp> sorterteTilkjenteFeriepenger(List<BeregningsresultatFeriepengerPrÅr> feriepenger) {
        return feriepenger.stream()
            .collect(Collectors.groupingBy(AndelGruppering::new,
                Collectors.reducing(new Beløp(BigDecimal.ZERO), BeregningsresultatFeriepengerPrÅr::getÅrsbeløp, Beløp::adder)));
    }

    private static class AndelGruppering {
        Year opptjent;
        String mottaker;

        AndelGruppering(BeregningsresultatFeriepengerPrÅr andel) {
            this.opptjent = Year.from(andel.getOpptjeningsår());
            this.mottaker = andel.getBeregningsresultatAndel().erBrukerMottaker() ? BRUKER :
                andel.getBeregningsresultatAndel().getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(ARBGIVER);
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
