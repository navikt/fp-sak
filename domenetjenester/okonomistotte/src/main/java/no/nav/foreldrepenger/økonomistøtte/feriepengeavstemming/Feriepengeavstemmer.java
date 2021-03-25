package no.nav.foreldrepenger.økonomistøtte.feriepengeavstemming;

import java.math.BigDecimal;
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
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AvvikReberegningFeriepenger;
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
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.økonomistøtte.HentOppdragMedPositivKvittering;

@ApplicationScoped
public class Feriepengeavstemmer {
    private static final String AVVIK_KODE = "FP-110712";
    private static final String BRUKER = "Bruker";
    private static final String ARBGIVER = "ArbGiv";
    private static final Logger LOG = LoggerFactory.getLogger(Feriepengeavstemmer.class);
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

    public boolean avstem(long behandlingId, boolean logging) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<BeregningsresultatEntitet> beregningsresultatOpt = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId);
        if (behandling == null || beregningsresultatOpt.isEmpty()) {
            return false;
        }
        var saksnummer = behandling.getFagsak().getSaksnummer();
        List<BeregningsresultatFeriepengerPrÅr> feriepengerAndeler = beregningsresultatOpt.get().getBeregningsresultatFeriepenger()
            .map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe)
            .orElse(Collections.emptyList());
        List<Oppdrag110> positiveOppdrag = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

        var oppdrag = sorterteOppdragFeriepenger(positiveOppdrag, logging);
        var tilkjent = sorterteTilkjenteFeriepenger(feriepengerAndeler);
        Map<GrupperingNøkkel, BigDecimal> summert = new LinkedHashMap<>();
        oppdrag.forEach(summert::put);
        tilkjent.forEach((key, value) -> summert.put(key, summert.getOrDefault(key, BigDecimal.ZERO).subtract(value.getVerdi())));

        if (logging) {
            Map<Year, BigDecimal> summertÅr = new LinkedHashMap<>();
            oppdrag.forEach((key, value) -> summertÅr.put(key.getOpptjent(), summertÅr.getOrDefault(key.getOpptjent(), BigDecimal.ZERO).add(value)));
            tilkjent.forEach((key, value) -> summertÅr.put(key.getOpptjent(), summertÅr.getOrDefault(key.getOpptjent(), BigDecimal.ZERO).subtract(value.getVerdi())));

            summert.entrySet().stream()
                .filter(e -> Math.abs(e.getValue().longValue()) > 3)
                .forEach(e -> LOG.info("{}:{}:saksnummer:{}:år:{}:mottaker:{}:diff:{}:oppdrag:{}:tilkjent:{}",
                    AVVIK_KODE, erAvvik(summertÅr.get(e.getKey().getOpptjent())) ? "oppdrag-tilkjent" : "omfordelt",
                    saksnummer.getVerdi(), e.getKey().getOpptjent(), e.getKey().getMottaker(), e.getValue().longValue(),
                    oppdrag.getOrDefault(e.getKey(), BigDecimal.ZERO).longValue(), tilkjent.getOrDefault(e.getKey(), Beløp.ZERO).getVerdi().longValue()));
        }
        return summert.values().stream().anyMatch(Feriepengeavstemmer::erAvvik);
    }

    public AvvikReberegningFeriepenger sjekkReberegnFeriepenger(long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<BeregningsresultatEntitet> beregningsresultatOpt = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId);
        if (behandling == null || beregningsresultatOpt.isEmpty()) {
            return AvvikReberegningFeriepenger.INGEN_AVVIK;
        }
        var saksnummer = behandling.getFagsak().getSaksnummer();
        List<BeregningsresultatFeriepengerPrÅr> feriepengerAndeler = beregningsresultatOpt.get().getBeregningsresultatFeriepenger()
            .map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe)
            .orElse(Collections.emptyList());
        List<Oppdrag110> positiveOppdrag = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

        var oppdrag = sorterteOppdragFeriepenger(positiveOppdrag, false);
        var tilkjent = sorterteTilkjenteFeriepenger(feriepengerAndeler);
        Map<GrupperingNøkkel, BigDecimal> summert = new LinkedHashMap<>();
        oppdrag.forEach(summert::put);
        tilkjent.forEach((key, value) -> summert.put(key, summert.getOrDefault(key, BigDecimal.ZERO).subtract(value.getVerdi())));


        var erAvvikBrukerFør2020 = summert.entrySet().stream().anyMatch(e -> GrupperingNøkkel.BRUKER_FØR_2020.equals(e.getKey()) && erAvvik(e.getValue()));
        if (erAvvikBrukerFør2020) return AvvikReberegningFeriepenger.AVVIK_BRUKER_2019;
        var erAvvik = summert.values().stream().anyMatch(Feriepengeavstemmer::erAvvik);
        return erAvvik ? AvvikReberegningFeriepenger.AVVIK_ANDRE : AvvikReberegningFeriepenger.INGEN_AVVIK;
    }

    private static boolean erAvvik(BigDecimal diff) {
        return Math.abs(diff.longValue()) > 3;
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

    private static Map<GrupperingNøkkel, BigDecimal> sorterteOppdragFeriepenger(List<Oppdrag110> oppdrag110Liste, boolean notQuiet) {
        Map<GrupperingNøkkel, BigDecimal> gjeldendeOL = new HashMap<>();

        for (Oppdragslinje150 linje : sorterEtterDatoFP(oppdrag110Liste)) {
            var nøkkel = new GrupperingNøkkel(Year.from(linje.getDatoVedtakFom().minusYears(1)), mottakerFraLinje150(linje));
            var forrige = gjeldendeOL.get(nøkkel);
            if (linje.gjelderOpphør()) {
                if (forrige == null) {
                    if (notQuiet) LOG.warn("Opphør uten noe å opphøre: delytelse {} klasseKode {} fom {} opphørsdato {} tidligste {}",
                        linje.getDelytelseId(), linje.getKodeKlassifik(), linje.getDatoVedtakFom(), linje.getDatoStatusFom(), forrige);
                } else if (forrige.intValue() != linje.getSats().getVerdi()) {
                    if (notQuiet) LOG.warn("Avvik gjeldende beløp: delytelse {} klasseKode {} fom {} opphørt {} gjeldende {}",
                        linje.getDelytelseId(), linje.getKodeKlassifik(), linje.getDatoVedtakFom(), linje.getSats(), forrige);
                } else {
                    gjeldendeOL.remove(nøkkel);
                }
            }
            if (!linje.gjelderOpphør()) {
                gjeldendeOL.put(nøkkel, BigDecimal.valueOf(linje.getSats().getVerdi()));
            }
        }
        return gjeldendeOL;
    }

    private static List<Oppdragslinje150> sorterEtterDatoFP(Collection<Oppdrag110> input) {
        return input.stream()
            .map(Oppdrag110::getOppdragslinje150Liste)
            .flatMap(Collection::stream)
            .filter(ol150 -> ol150.getKodeKlassifik().gjelderFeriepenger())
            .sorted(Comparator.comparing(Oppdragslinje150::getOpprettetTidspunkt))
            .collect(Collectors.toList());
    }


    public static class GrupperingNøkkel {

        private final Year opptjent;
        private final String mottaker;

        public GrupperingNøkkel(Year opptjent, String mottaker) {
            this.opptjent = opptjent;
            this.mottaker = mottaker;
        }

        static GrupperingNøkkel BRUKER_FØR_2020 = new GrupperingNøkkel(Year.of(2019), BRUKER);

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
