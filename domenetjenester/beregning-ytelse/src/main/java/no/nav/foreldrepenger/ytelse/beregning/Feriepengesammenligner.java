package no.nav.foreldrepenger.ytelse.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetOgArbeidsforholdNøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class Feriepengesammenligner {
    private static final Logger logger = LoggerFactory.getLogger(Feriepengesammenligner.class);
    private final long behandlingId;
    private final Saksnummer saksnummer;
    private final BeregningsresultatEntitet nyttResultat;
    private final BeregningsresultatEntitet gjeldendeResultat;
    private static final String AVVIK_KODE = "FP-110711";
    private static final String BRUKER = "Bruker";

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
            sammenlignFeriepengeandeler(nyttFeriepengegrunnlag.get().getBeregningsresultatFeriepengerPrÅrListe(),
                gjeldendeFeriepengegrunnlag.get().getBeregningsresultatFeriepengerPrÅrListe());
        }
        else if (nyttFeriepengegrunnlag.isPresent() || gjeldendeFeriepengegrunnlag.isPresent()) {
            // Vet at kun en av de er present
            loggOgSettAvvik("beregningsresultatFeriepenger", nyttFeriepengegrunnlag, gjeldendeFeriepengegrunnlag);
        }
        return finnesAvvik;
    }

    private void sammenlignFeriepengeandeler(List<BeregningsresultatFeriepengerPrÅr> nyeAndeler,
                                                List<BeregningsresultatFeriepengerPrÅr> gjeldendeAndeler) {
        Map<LocalDate, List<BeregningsresultatFeriepengerPrÅr>> nyÅrTilBeløpMap = årTilAndelerMap(nyeAndeler);
        Map<LocalDate, List<BeregningsresultatFeriepengerPrÅr>> gjeldendeÅrTilBeløpMap = årTilAndelerMap(gjeldendeAndeler);
        sammenlignÅrligeFeriepenger(nyÅrTilBeløpMap, gjeldendeÅrTilBeløpMap);
        Set<LocalDate> alleÅrene = Stream.concat(nyÅrTilBeløpMap.keySet().stream(), gjeldendeÅrTilBeløpMap.keySet().stream()).collect(Collectors.toSet());
        alleÅrene.forEach(år -> sammenlignGrupperteFeriepenger(år, nyÅrTilBeløpMap.getOrDefault(år, List.of()), gjeldendeÅrTilBeløpMap.getOrDefault(år, List.of())));
    }

    private void sammenlignÅrligeFeriepenger(Map<LocalDate, List<BeregningsresultatFeriepengerPrÅr>> nytt,
                                             Map<LocalDate, List<BeregningsresultatFeriepengerPrÅr>> eksisterende) {
        Map<LocalDate, BigDecimal> summert = new LinkedHashMap<>();
        eksisterende.forEach((key, value) -> summert.put(key, finnÅrsbeløp(value).getVerdi()));
        nytt.forEach((key, value) -> summert.put(key, summert.getOrDefault(key, BigDecimal.ZERO).subtract(finnÅrsbeløp(value).getVerdi())));
        summert.entrySet().stream()
            .filter(e -> Math.abs(e.getValue().longValue()) > 3)
            .forEach(e -> loggOgSettAvvik("Årsbeløp for " + Year.from(e.getKey()) + " diff " + e.getValue().longValue(),
                finnÅrsbeløp(nytt.getOrDefault(e.getKey(), List.of())).getVerdi().longValue(),
                finnÅrsbeløp(eksisterende.getOrDefault(e.getKey(), List.of())).getVerdi().longValue()));
    }

    private void sammenlignGrupperteFeriepenger(LocalDate år, List<BeregningsresultatFeriepengerPrÅr> nytt,
                                             List<BeregningsresultatFeriepengerPrÅr> eksisterende) {
        var nyGruppert = nøkkelTilGrupperingMap(nytt);
        var gmlGruppert = nøkkelTilGrupperingMap(eksisterende);
        Map<Feriepengesammenligner.AndelGruppering, BigDecimal> summert = new LinkedHashMap<>();
        gmlGruppert.forEach((k, v) -> summert.put(k, finnÅrsbeløp(v).getVerdi()));
        nyGruppert.forEach((k, v) -> summert.put(k, summert.getOrDefault(k, BigDecimal.ZERO).subtract(finnÅrsbeløp(v).getVerdi())));
        summert.entrySet().stream()
            .filter(e -> Math.abs(e.getValue().longValue()) > 3)
            .forEach(e -> loggOgSettAvvik("Andelsbeløp for " + e.getKey() + " år " + Year.from(år) + " diff " + e.getValue().longValue(),
                finnÅrsbeløp(nyGruppert.getOrDefault(e.getKey(), List.of())).getVerdi().longValue(),
                finnÅrsbeløp(gmlGruppert.getOrDefault(e.getKey(), List.of())).getVerdi().longValue()));
    }

    private void sammenlignNøkler(LocalDate år, Map<AktivitetOgArbeidsforholdNøkkel, List<BeregningsresultatFeriepengerPrÅr>> nyNøkkelTilAndelerMap,
                                  Map<AktivitetOgArbeidsforholdNøkkel, List<BeregningsresultatFeriepengerPrÅr>> gjeldendeNøkkelTilAndelerMap) {
        nyNøkkelTilAndelerMap.forEach((nøkkel, andeler) -> {
            List<BeregningsresultatFeriepengerPrÅr> gjeldendeAndelerForNøkkel = gjeldendeNøkkelTilAndelerMap.getOrDefault(nøkkel, List.of());
            Beløp gjeldendeÅrsbeløp = finnÅrsbeløp(gjeldendeAndelerForNøkkel);
            Beløp nyttÅrsbeløp = finnÅrsbeløp(andeler);
            var diff = gjeldendeÅrsbeløp.getVerdi().subtract(nyttÅrsbeløp.getVerdi()).longValue();
            if (Math.abs(diff) > 3) {
                loggOgSettAvvik("Årsbeløp for andel " + nøkkel + " i år " + Year.from(år), nyttÅrsbeløp.getVerdi().longValue(), gjeldendeÅrsbeløp.getVerdi().longValue());
            }
        });
    }

    private Beløp finnÅrsbeløp(List<BeregningsresultatFeriepengerPrÅr> andeler) {
        return andeler.stream().map(BeregningsresultatFeriepengerPrÅr::getÅrsbeløp).reduce(Beløp::adder).orElse(Beløp.ZERO);
    }

    private Map<AktivitetOgArbeidsforholdNøkkel, List<BeregningsresultatFeriepengerPrÅr>> nøkkelTilAndelerMap(List<BeregningsresultatFeriepengerPrÅr> andeler) {
        return andeler.stream()
            .collect(Collectors.groupingBy(an -> an.getBeregningsresultatAndel().getAktivitetOgArbeidsforholdNøkkel()));
    }

    private Map<AndelGruppering, List<BeregningsresultatFeriepengerPrÅr>> nøkkelTilGrupperingMap(List<BeregningsresultatFeriepengerPrÅr> andeler) {
        return andeler.stream()
            .collect(Collectors.groupingBy(AndelGruppering::new));
    }

    private Map<LocalDate, List<BeregningsresultatFeriepengerPrÅr>> årTilAndelerMap(List<BeregningsresultatFeriepengerPrÅr> andeler) {
        return andeler.stream().collect(Collectors.groupingBy(BeregningsresultatFeriepengerPrÅr::getOpptjeningsår));
    }

    private void sammenlignFeriepengeperiode(BeregningsresultatFeriepenger nytt, BeregningsresultatFeriepenger gjeldende) {
        boolean ulikFOM = !Objects.equals(nytt.getFeriepengerPeriodeFom(), gjeldende.getFeriepengerPeriodeFom());
        boolean ulikTOM = !Objects.equals(nytt.getFeriepengerPeriodeTom(), gjeldende.getFeriepengerPeriodeTom());
        if (ulikFOM) {
            loggOgSettAvvik("feriepengeperiodeFOM", nytt.getFeriepengerPeriodeFom(), gjeldende.getFeriepengerPeriodeFom());
        }
        if (ulikTOM) {
            loggOgSettAvvik("feriepengeperiodeTOM", nytt.getFeriepengerPeriodeTom(), gjeldende.getFeriepengerPeriodeTom());
        }
    }

    private void loggOgSettAvvik(String beskrivelse, Object nytt, Object gammelt) {
        this.finnesAvvik = true;
        String gammelBeskrivelse = gammelt == null ? "null" : gammelt.toString();
        String nyBeskrivelse = nytt == null ? "null" : nytt.toString();
        logger.info("{} Avvik mellom ny og gammel feriepengeberegning på saksnummer {} behandling {}\n   Avvik {}\n    Gammelt {} Nytt {}",
            AVVIK_KODE, saksnummer.getVerdi(), behandlingId, beskrivelse, gammelBeskrivelse, nyBeskrivelse);
    }

    private static class AndelGruppering {
        String mottaker;
        String arbeidsgiver;
        InternArbeidsforholdRef arbeidsforholdRef;
        AktivitetStatus aktivitetStatus;
        Inntektskategori inntektskategori;

        AndelGruppering(BeregningsresultatFeriepengerPrÅr andel) {
            this.mottaker = andel.getBeregningsresultatAndel().erBrukerMottaker() ? BRUKER : "Arbgiv";
            this.arbeidsgiver = andel.getBeregningsresultatAndel().getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null);
            this.arbeidsforholdRef = andel.getBeregningsresultatAndel().getArbeidsforholdRef();
            this.aktivitetStatus = andel.getBeregningsresultatAndel().getAktivitetStatus();
            this.inntektskategori = andel.getBeregningsresultatAndel().getInntektskategori();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AndelGruppering that = (AndelGruppering) o;
            return mottaker.equals(that.mottaker) && Objects.equals(arbeidsgiver, that.arbeidsgiver) && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef) && aktivitetStatus == that.aktivitetStatus && inntektskategori == that.inntektskategori;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mottaker, arbeidsgiver, arbeidsforholdRef, aktivitetStatus, inntektskategori);
        }

        @Override
        public String toString() {
            return "AndelGruppering{" +
                "mottaker='" + mottaker + '\'' +
                ", arbeidsgiver=" + arbeidsgiver +
                ", aktivitetStatus=" + aktivitetStatus +
                '}';
        }
    }

}
