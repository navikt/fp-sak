package no.nav.foreldrepenger.økonomi.feriepengeavstemming;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.økonomi.økonomistøtte.HentOppdragMedPositivKvittering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class Feriepengeavstemmer {
    private static final String AVVIK_KODE = "FP-110712";
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

        return avvikErFunnet;
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


    //input er List<Oppdrag110> tidligereOppdrag110 = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);
    private static List<FeriepengeOppdrag> sorterteFeriepenger(List<Oppdrag110> oppdrag110Liste) {
        Map<Long, Oppdragslinje150> gjeldendeOL = new HashMap<>();

        for (Oppdragslinje150 linje : sorterEtterDelytelseIdFP(oppdrag110Liste)) {
            var forrige = gjeldendeOL.get(linje.getDelytelseId());
            if (linje.gjelderOpphør()) {
                if (forrige == null) {
                    LOGGER.warn("Opphør uten noe å opphøre: delytelse {} klasseKode {} fom {} tom {} opphørsdato {} tidligste {}",
                        linje.getDelytelseId(), linje.getKodeKlassifik(), linje.getDatoVedtakFom(), linje.getDatoVedtakTom(), linje.getDatoStatusFom(), forrige);
                } else {
                    gjeldendeOL.remove(linje.getDelytelseId());
                }
            }
            if (!linje.gjelderOpphør()) {
                gjeldendeOL.put(linje.getDelytelseId(), linje);
            }
        }
        return gjeldendeOL.values().stream()
            .map(ol150 -> new FeriepengeOppdrag(ØkonomiKodeKlassifik.fraKode(ol150.getKodeKlassifik()),
                ol150.getDatoVedtakFom(), ol150.getSats(),
                ol150.getUtbetalesTilId(), ol150.getRefusjonsinfo156() != null ? ol150.getRefusjonsinfo156().getRefunderesId() : null))
            .collect(Collectors.toList());
    }


    private static List<Oppdragslinje150> sorterEtterDelytelseIdFP(Collection<Oppdrag110> input) {
        return input.stream()
            .map(Oppdrag110::getOppdragslinje150Liste)
            .flatMap(Collection::stream)
            .filter(ol150 -> ØkonomiKodeKlassifik.fraKode(ol150.getKodeKlassifik()).gjelderFerie())
            .sorted(Comparator.comparing(Oppdragslinje150::getDelytelseId)
                .thenComparing(Oppdragslinje150::getOpprettetTidspunkt))
            .collect(Collectors.toList());
    }


}
