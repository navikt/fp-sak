package no.nav.foreldrepenger.økonomi.feriepengeavstemming;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.økonomi.økonomistøtte.HentOppdragMedPositivKvittering;

@ApplicationScoped
public class Feriepengeavstemmer {
    private static final String AVVIK_KODE = "FP-110712";
    private static final String BRUKER = "Bruker";
    private static final String ARBGIVER = "ArbGiv";
    private static final Logger LOGGER = LoggerFactory.getLogger(Feriepengeavstemmer.class);
    private HentOppdragMedPositivKvittering hentOppdragMedPositivKvittering;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    public Feriepengeavstemmer() {
        // CDI
    }

    @Inject
    public Feriepengeavstemmer(HentOppdragMedPositivKvittering hentOppdragMedPositivKvittering,
                               BehandlingRepository behandlingRepository,
                               BeregningsresultatRepository beregningsresultatRepository) {
        this.hentOppdragMedPositivKvittering = hentOppdragMedPositivKvittering;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    public boolean avstem(long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<BeregningsresultatEntitet> beregningsresultatOpt = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        if (behandling == null || beregningsresultatOpt.isEmpty()) {
            return false;
        }
        String saksnummer = behandling.getFagsak().getSaksnummer().getVerdi();
        List<BeregningsresultatFeriepengerPrÅr> feriepengerAndeler = beregningsresultatOpt.get().getBeregningsresultatFeriepenger()
            .map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe)
            .orElse(Collections.emptyList());
        List<Oppdrag110> positiveOppdrag = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(behandling);

        var oppdrag = sorterteOppdragFeriepenger(positiveOppdrag);
        var tilkjent = sorterteTilkjenteFeriepenger(feriepengerAndeler);
        Map<GrupperingNøkkel, BigDecimal> summert = new LinkedHashMap<>();
        oppdrag.forEach(summert::put);
        tilkjent.forEach((key, value) -> summert.put(key, summert.getOrDefault(key, BigDecimal.ZERO).subtract(value.getVerdi())));

        Map<Year, BigDecimal> summertÅr = new LinkedHashMap<>();
        oppdrag.forEach((key,value) -> summertÅr.put(key.getOpptjent(), summertÅr.getOrDefault(key.getOpptjent(), BigDecimal.ZERO).add(value)));
        tilkjent.forEach((key,value) -> summertÅr.put(key.getOpptjent(), summertÅr.getOrDefault(key.getOpptjent(), BigDecimal.ZERO).subtract(value.getVerdi())));

        summertÅr.entrySet().stream()
            .filter(e -> Math.abs(e.getValue().longValue()) > 3)
            .forEach(e -> LOGGER.info("{} årlig oppdrag-tilkjent saksnummer {} behandling {} år {} diff {}",
                AVVIK_KODE, saksnummer, behandlingId, e.getKey(), e.getValue().longValue()));

        summert.entrySet().stream()
            .filter(e -> Math.abs(e.getValue().longValue()) > 3)
            .forEach(e -> LOGGER.info("{} andel {} saksnummer {} behandling {} år {} mottaker {} diff {}",
                AVVIK_KODE, erAvvik(summertÅr.get(e.getKey().getOpptjent())) ? "oppdrag-tilkjent" : "omfordelt",
                saksnummer, behandlingId, e.getKey().getOpptjent(), e.getKey().getMottaker(), e.getValue().longValue()));
        /*
        List<FeriepengeOppdrag> fpOppdrag = sorterteFeriepenger(positiveOppdrag);

        boolean avvikErFunnet = false;
        Map<FeriepengeAndel.MottakerPrÅr, List<FeriepengeAndel>> feriepengerPrMottakerÅrMap = aggregerFeriepengeandeler(feriepengerAndeler);
        // Sammenlign tilkjent ytelse mot oppdrag
        for (var entry : feriepengerPrMottakerÅrMap.entrySet()) {
            FeriepengeAndel.MottakerPrÅr mottaker = entry.getKey();
            List<FeriepengeOppdrag> matchendeOppdrag = finnMatchendeOppdragForTilkjentYtelse(mottaker, fpOppdrag);
            long oppdragLongSum = matchendeOppdrag.stream().mapToLong(FeriepengeOppdrag::getSats).reduce(Long::sum).orElse(0L);
            BigDecimal oppdragSum = BigDecimal.valueOf(oppdragLongSum);
            BigDecimal beregnedeFeriepenger = entry.getValue().stream().map(FeriepengeAndel::getBeløp).reduce(Beløp::adder).orElse(Beløp.ZERO).getVerdi();
            BigDecimal diff = beregnedeFeriepenger.subtract(oppdragSum);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                String mottakerBeskrivelse = mottaker.isSøkerErMottaker() ? "SØKER" : mottaker.getOrgnr();
                avvikErFunnet = true;
                LOGGER.info("{} Avvik mellom ny og gammel feriepengeberegning på saksnummer {} behandling {}\n  Mottaker {}\n Oppdragsum {}\n  Feriepengegrunnlag {} Diff {}",
                    AVVIK_KODE, saksnummer, behandlingId, mottakerBeskrivelse, oppdragSum.longValue(), beregnedeFeriepenger.longValue(), diff.longValue());
            }
        }
        Map<FeriepengeOppdrag.OppdragMottakerPrÅr, List<FeriepengeOppdrag>> oppdragMottakerPrÅrMap = fpOppdrag.stream().collect(Collectors.groupingBy(FeriepengeOppdrag::getMottaker));
        // Sammenlign oppdrag mot tilkjent ytelse
        for (var entry : oppdragMottakerPrÅrMap.entrySet()) {
            FeriepengeOppdrag.OppdragMottakerPrÅr oppdragMottakerPrÅr = entry.getKey();
            List<BeregningsresultatFeriepengerPrÅr> matchendeTilkjentYtelse = finnMatchendeTilkjentYtelseForOppdrag(oppdragMottakerPrÅr, feriepengerAndeler);
            BigDecimal tilkjentYtelseSum = matchendeTilkjentYtelse.stream().map(BeregningsresultatFeriepengerPrÅr::getÅrsbeløp).reduce(Beløp::adder).orElse(Beløp.ZERO).getVerdi();
            long oppdragSumLong = entry.getValue().stream().mapToLong(FeriepengeOppdrag::getSats).reduce(Long::sum).orElse(0L);
            BigDecimal oppdragSum = BigDecimal.valueOf(oppdragSumLong);
            BigDecimal diff = oppdragSum.subtract(tilkjentYtelseSum);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                String mottakerBeskrivelse = oppdragMottakerPrÅr.isErSøker() ? "SØKER" : oppdragMottakerPrÅr.getMottakerId();
                avvikErFunnet = true;
                LOGGER.info("{} Avvik mellom ny og gammel feriepengeberegning på saksnummer {} behandling {}\n  Mottaker {}\n Oppdragsum {}\n  Feriepengegrunnlag {} Diff {}",
                    AVVIK_KODE, saksnummer, behandlingId, mottakerBeskrivelse, oppdragSum.longValue(), tilkjentYtelseSum.longValue(), diff.longValue());
            }
        }
         */

        return summert.values().stream().anyMatch(Feriepengeavstemmer::erAvvik);
    }

    private static boolean erAvvik(BigDecimal diff) {
        return Math.abs(diff.longValue()) > 3;
    }

    private List<BeregningsresultatFeriepengerPrÅr> finnMatchendeTilkjentYtelseForOppdrag(FeriepengeOppdrag.OppdragMottakerPrÅr oppdragMottakerPrÅr, List<BeregningsresultatFeriepengerPrÅr> feriepengerAndeler) {
        if (oppdragMottakerPrÅr.isErSøker()) {
            return feriepengerAndeler.stream()
                .filter( fpa -> matcherUtbetalingÅr(oppdragMottakerPrÅr, fpa) && fpa.getBeregningsresultatAndel().erBrukerMottaker())
                .collect(Collectors.toList());
        } else {
            return feriepengerAndeler.stream()
                .filter( fpa -> matcherUtbetalingÅr(oppdragMottakerPrÅr, fpa)
                    && !fpa.getBeregningsresultatAndel().erBrukerMottaker()
                    && oppdragMottakerPrÅr.getMottakerId().equals(finnOrgnrFraTilkjentYtelse(fpa)))
                .collect(Collectors.toList());
        }
    }

    private String finnOrgnrFraTilkjentYtelse(BeregningsresultatFeriepengerPrÅr fpa) {
        Optional<String> orgnr = fpa.getBeregningsresultatAndel().getArbeidsgiver().map(Arbeidsgiver::getOrgnr);
        return orgnr.map(this::lagMottakerRefusjonId).orElse(null);
    }

    private Map<FeriepengeAndel.MottakerPrÅr, List<FeriepengeAndel>> aggregerFeriepengeandeler(List<BeregningsresultatFeriepengerPrÅr> feriepengerAndeler) {
        List<FeriepengeAndel> feriepengeandeler = feriepengerAndeler.stream().map(andel -> new FeriepengeAndel(andel.getBeregningsresultatAndel().erBrukerMottaker(),
            andel.getBeregningsresultatAndel().getArbeidsgiver().map(Arbeidsgiver::getOrgnr).orElse(null),
            andel.getOpptjeningsår(),
            andel.getÅrsbeløp()))
            .collect(Collectors.toList());
        return feriepengeandeler.stream().collect(Collectors.groupingBy(FeriepengeAndel::getMottakerPrÅr));

    }

    private List<FeriepengeOppdrag> finnMatchendeOppdragForTilkjentYtelse(FeriepengeAndel.MottakerPrÅr fpAndel, List<FeriepengeOppdrag> fpOppdrag) {
        if (fpAndel.isSøkerErMottaker()) {
            return fpOppdrag.stream().filter(fpo -> fpo.getMottakerPerson() != null && matcherUtbetalingÅr(fpAndel, fpo)).collect(Collectors.toList());
        } else {
            return fpOppdrag.stream().filter(fpo -> fpo.getMottakerRefusjon() != null
                && fpo.getMottakerRefusjon().equals(lagMottakerRefusjonId(fpAndel.getOrgnr()))
                && matcherUtbetalingÅr(fpAndel, fpo)).collect(Collectors.toList());
        }
    }

    private String lagMottakerRefusjonId(String orgnr) {
        return String.format("00%s", orgnr);
    }

    private boolean matcherUtbetalingÅr(FeriepengeAndel.MottakerPrÅr fpAndel, FeriepengeOppdrag fpoppdrag) {
        LocalDate utbetalingFom = LocalDate.of(fpAndel.getOpptjeningsår().getYear() + 1, 5, 1);
        return utbetalingFom.equals(fpoppdrag.getUtbetalesFom());
    }

    private boolean matcherUtbetalingÅr(FeriepengeOppdrag.OppdragMottakerPrÅr oppdragMottaker, BeregningsresultatFeriepengerPrÅr fpAndel) {
        LocalDate utbetalingFom = LocalDate.of(fpAndel.getOpptjeningsår().getYear() + 1, 5, 1);
        return utbetalingFom.equals(oppdragMottaker.getUtbetalesFom());
    }

   private static String mottakerFraBRAndel(BeregningsresultatAndel andel) {
       return andel.erBrukerMottaker() ? BRUKER : andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(ARBGIVER);
   }

    private static String mottakerFraRefusjon156(Refusjonsinfo156 info) {
        return info != null && info.getRefunderesId() != null ? info.getRefunderesId().substring(2,11) : ARBGIVER;
    }

    private static String mottakerFraLinje150(Oppdragslinje150 oppdragslinje150) {
        return oppdragslinje150.getUtbetalesTilId() != null ? BRUKER : mottakerFraRefusjon156(oppdragslinje150.getRefusjonsinfo156());
    }

    private static Map<GrupperingNøkkel, Beløp> sorterteTilkjenteFeriepenger(List<BeregningsresultatFeriepengerPrÅr> feriepenger) {
        return feriepenger.stream()
            .collect(Collectors.groupingBy(a -> new GrupperingNøkkel(Year.from(a.getOpptjeningsår()), mottakerFraBRAndel(a.getBeregningsresultatAndel())),
                Collectors.reducing(Beløp.ZERO, BeregningsresultatFeriepengerPrÅr::getÅrsbeløp, Beløp::adder)));
    }

    private static Map<GrupperingNøkkel, BigDecimal> sorterteOppdragFeriepenger(List<Oppdrag110> oppdrag110Liste) {
        Map<GrupperingNøkkel, BigDecimal> gjeldendeOL = new HashMap<>();

        for (Oppdragslinje150 linje : sorterEtterDatoFP(oppdrag110Liste)) {
            var nøkkel = new GrupperingNøkkel(Year.from(linje.getDatoVedtakFom().minusYears(1)), mottakerFraLinje150(linje));
            var forrige = gjeldendeOL.get(nøkkel);
            if (linje.gjelderOpphør()) {
                if (forrige == null) {
                    LOGGER.warn("Opphør uten noe å opphøre: delytelse {} klasseKode {} fom {} opphørsdato {} tidligste {}",
                        linje.getDelytelseId(), linje.getKodeKlassifik(), linje.getDatoVedtakFom(), linje.getDatoStatusFom(), forrige);
                } else if (forrige.longValue() != linje.getSats()) {
                    LOGGER.warn("Avvik gjeldende beløp: delytelse {} klasseKode {} fom {} opphørt {} gjeldende {}",
                        linje.getDelytelseId(), linje.getKodeKlassifik(), linje.getDatoVedtakFom(), linje.getSats(), forrige);
                } else {
                    gjeldendeOL.remove(nøkkel);
                }
            }
            if (!linje.gjelderOpphør()) {
                gjeldendeOL.put(nøkkel, new BigDecimal(linje.getSats()));
            }
        }
        return gjeldendeOL;
    }

    private static List<Oppdragslinje150> sorterEtterDatoFP(Collection<Oppdrag110> input) {
        return input.stream()
            .map(Oppdrag110::getOppdragslinje150Liste)
            .flatMap(Collection::stream)
            .filter(ol150 -> ØkonomiKodeKlassifik.fraKode(ol150.getKodeKlassifik()).gjelderFerie())
            .sorted(Comparator.comparing(Oppdragslinje150::getOpprettetTidspunkt))
            .collect(Collectors.toList());
    }


    public static class GrupperingNøkkel {

        private Year opptjent;
        private String mottaker;

        public GrupperingNøkkel(Year opptjent, String mottaker) {
            this.opptjent = opptjent;
            this.mottaker = mottaker;
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
            GrupperingNøkkel that = (GrupperingNøkkel) o;
            return Objects.equals(opptjent, that.opptjent) && Objects.equals(mottaker, that.mottaker);
        }

        @Override
        public int hashCode() {
            return Objects.hash(opptjent, mottaker);
        }
    }


}
