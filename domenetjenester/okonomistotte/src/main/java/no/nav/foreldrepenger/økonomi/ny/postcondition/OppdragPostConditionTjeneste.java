package no.nav.foreldrepenger.økonomi.ny.postcondition;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomi.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomi.ny.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomi.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomi.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomi.ny.mapper.EksisterendeOppdragMapper;
import no.nav.foreldrepenger.økonomi.ny.mapper.TilkjentYtelseMapper;
import no.nav.foreldrepenger.økonomi.ny.tjeneste.EndringsdatoTjeneste;
import no.nav.foreldrepenger.økonomi.ny.util.SetUtil;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.vedtak.feil.Feil;

@ApplicationScoped
public class OppdragPostConditionTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(OppdragPostConditionTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private ØkonomioppdragRepository økonomioppdragRepository;
    private FagsakRepository fagsakRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    OppdragPostConditionTjeneste() {
        //for CDI proxy
    }

    @Inject
    public OppdragPostConditionTjeneste(BehandlingRepository behandlingRepository, BeregningsresultatRepository beregningsresultatRepository, ØkonomioppdragRepository økonomioppdragRepository, FagsakRepository fagsakRepository, FamilieHendelseRepository familieHendelseRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.fagsakRepository = fagsakRepository;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    public void softPostCondition(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
        if (fagsakYtelseType == FagsakYtelseType.FORELDREPENGER || fagsakYtelseType == FagsakYtelseType.SVANGERSKAPSPENGER) {
            BeregningsresultatEntitet beregningsresultat = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId()).orElse(null);
            softPostCondition(behandling, beregningsresultat);
        }
    }

    private void softPostCondition(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        try {
            sammenlignEffektAvOppdragMedTilkjentYtelseOgLoggAvvik(behandling, beregningsresultat);
        } catch (Exception e) {
            logger.warn("Teknisk feil ved sammenligning av effekt av oppdrag mot tilkjent ytelse for " + behandling.getFagsak().getSaksnummer() + " behandling " + behandling.getId() + ". Dette bør undersøkes: " + e.getMessage(), e);
        }
    }

    private boolean sammenlignEffektAvOppdragMedTilkjentYtelseOgLoggAvvik(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        Map<Betalingsmottaker, TilkjentYtelseDifferanse> resultat = sammenlignEffektAvOppdragMedTilkjentYtelse(behandling, beregningsresultat);
        boolean altOk = true;
        for (var entry : resultat.entrySet()) {
            Feil feil = konverterTilFeil(behandling.getFagsak().getSaksnummer(), Long.toString(behandling.getId()), entry.getKey(), entry.getValue());
            if (feil != null) {
                feil.log(logger);
                altOk = false;
            }
        }
        return altOk;
    }

    private Map<Betalingsmottaker, TilkjentYtelseDifferanse> sammenlignEffektAvOppdragMedTilkjentYtelse(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        List<Oppdragskontroll> oppdragene = økonomioppdragRepository.finnAlleOppdragForSak(saksnummer);
        Fagsak sak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        Map<KjedeNøkkel, OppdragKjede> oppdragskjeder = EksisterendeOppdragMapper.tilKjeder(oppdragene);
        GruppertYtelse målbilde = TilkjentYtelseMapper.lagFor(sak.getYtelseType(), finnFamilieYtelseType(behandling)).fordelPåNøkler(beregningsresultat);

        Set<KjedeNøkkel> alleKjedenøkler = SetUtil.union(oppdragskjeder.keySet(), målbilde.getNøkler());
        Set<Betalingsmottaker> betalingsmottakere = alleKjedenøkler.stream().map(KjedeNøkkel::getBetalingsmottaker).collect(Collectors.toSet());

        HashMap<Betalingsmottaker, TilkjentYtelseDifferanse> resultat = new HashMap<>();
        for (Betalingsmottaker betalingsmottaker : betalingsmottakere) {
            List<TilkjentYtelseDifferanse> differanser = new ArrayList<>();
            for (KjedeNøkkel nøkkel : alleKjedenøkler) {
                if (nøkkel.getBetalingsmottaker().equals(betalingsmottaker)) {
                    OppdragKjede oppdragKjede = oppdragskjeder.getOrDefault(nøkkel, OppdragKjede.EMPTY);
                    Ytelse ytelse = målbilde.getYtelsePrNøkkel().getOrDefault(nøkkel, Ytelse.EMPTY);
                    Ytelse effektAvOppdragskjede = oppdragKjede.tilYtelse();
                    finnDifferanse(ytelse, effektAvOppdragskjede).ifPresent(differanser::add);
                }
            }
            LocalDate førsteDatoForDifferanseSats = finnLaveste(differanser, TilkjentYtelseDifferanse::getFørsteDatoForDifferanseSats);
            LocalDate førsteDatoForDifferanseUtbetalingsgrad = finnLaveste(differanser, TilkjentYtelseDifferanse::getFørsteDatoForDifferanseUtbetalingsgrad);
            long sumForskjell = differanser.stream().mapToLong(TilkjentYtelseDifferanse::getDifferanseYtelse).sum();
            resultat.put(betalingsmottaker, new TilkjentYtelseDifferanse(førsteDatoForDifferanseSats, førsteDatoForDifferanseUtbetalingsgrad, sumForskjell));
        }
        return resultat;
    }


    private Feil konverterTilFeil(Saksnummer saksnummer, String behandlingId, Betalingsmottaker betalingsmottaker, TilkjentYtelseDifferanse differanse) {
        if (!differanse.harAvvik()) {
            return null;
        }
        LocalDate datoEndringYtelse = differanse.getFørsteDatoForDifferanseSats();
        LocalDate datoEndringUtbetalingsgrad = differanse.getFørsteDatoForDifferanseUtbetalingsgrad();
        long sumForskjell = differanse.getDifferanseYtelse();

        String message = "Sammenligning av effekt av oppdrag mot tilkjent ytelse viser avvik for " + saksnummer + ", behandling " + behandlingId + " til " + betalingsmottaker + ". Dette bør undersøkes og evt. patches. Det er ";
        if (Objects.equals(datoEndringYtelse, datoEndringUtbetalingsgrad)) {
            message += "forskjell i sats og utbetalingsgrad mellom oppdrag og tilkjent ytelse fra " + datoEndringYtelse + ". ";
        } else {
            if (datoEndringYtelse != null) {
                message += "forskjell i sats mellom oppdrag og tilkjent ytelse fra " + datoEndringYtelse + ". ";
            }
            if (datoEndringUtbetalingsgrad != null) {
                message += "forskjell i utbetalingsgrad mellom oppdrag og tilkjent ytelse fra " + datoEndringUtbetalingsgrad + ". ";
            }
        }
        message += " Sum effekt er " + formatForskjell(sumForskjell);
        return OppdragValideringFeil.FACTORY.valideringsfeil(message);
    }

    private String formatForskjell(long forskjell) {
        long rettsgebyr = 1172;
        if (forskjell > 4 * rettsgebyr) {
            return "vesentlig overbetaling";
        }
        if (forskjell > 0) {
            return "overbetaling av " + forskjell;
        }
        if (forskjell < -4 * rettsgebyr) {
            return "vesentlig underbetaling";
        }
        if (forskjell < 0) {
            return "underbetaling av " + forskjell;
        }
        return "ingen feilutbetaling";
    }

    private static <T> LocalDate finnLaveste(List<T> liste, Function<T, LocalDate> datofunksjon) {
        return liste.stream()
            .map(datofunksjon)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);
    }

    static class TilkjentYtelseDifferanse {
        private LocalDate førsteDatoForDifferanseSats;
        private LocalDate førsteDatoForDifferanseUtbetalingsgrad;
        private long differanseYtelse;

        public TilkjentYtelseDifferanse(LocalDate førsteDatoForDifferanseSats, LocalDate førsteDatoForDifferanseUtbetalingsgrad, long differanseYtelse) {
            this.førsteDatoForDifferanseSats = førsteDatoForDifferanseSats;
            this.førsteDatoForDifferanseUtbetalingsgrad = førsteDatoForDifferanseUtbetalingsgrad;
            this.differanseYtelse = differanseYtelse;
        }

        public LocalDate getFørsteDatoForDifferanseSats() {
            return førsteDatoForDifferanseSats;
        }

        public LocalDate getFørsteDatoForDifferanseUtbetalingsgrad() {
            return førsteDatoForDifferanseUtbetalingsgrad;
        }

        public long getDifferanseYtelse() {
            return differanseYtelse;
        }

        public boolean harAvvik() {
            return førsteDatoForDifferanseSats != null || førsteDatoForDifferanseUtbetalingsgrad != null || differanseYtelse != 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TilkjentYtelseDifferanse that = (TilkjentYtelseDifferanse) o;
            return differanseYtelse == that.differanseYtelse &&
                Objects.equals(førsteDatoForDifferanseSats, that.førsteDatoForDifferanseSats) &&
                Objects.equals(førsteDatoForDifferanseUtbetalingsgrad, that.førsteDatoForDifferanseUtbetalingsgrad);
        }

        @Override
        public int hashCode() {
            return Objects.hash(førsteDatoForDifferanseSats, førsteDatoForDifferanseUtbetalingsgrad, differanseYtelse);
        }

        @Override
        public String toString() {
            return "TilkjentYtelseDifferanse{" +
                "førsteDatoForDifferanseSats=" + førsteDatoForDifferanseSats +
                ", førsteDatoForDifferanseUtbetalingsgrad=" + førsteDatoForDifferanseUtbetalingsgrad +
                ", differanseYtelse=" + differanseYtelse +
                '}';
        }
    }

    static Optional<TilkjentYtelseDifferanse> finnDifferanse(Ytelse ytelse, Ytelse effektAvOppdragskjede) {
        LocalDate datoEndringYtelse = EndringsdatoTjeneste.finnEndringsdatoForEndringSats(ytelse, effektAvOppdragskjede);
        LocalDate datoEndringUtbetalingsgrad = EndringsdatoTjeneste.finnEndringsdatoForEndringUtbetalingsgrad(ytelse, effektAvOppdragskjede);
        long differanseYtelse = effektAvOppdragskjede.summerYtelse() - ytelse.summerYtelse();

        if (datoEndringYtelse == null && datoEndringUtbetalingsgrad == null && differanseYtelse == 0) {
            return Optional.empty();
        }
        return Optional.of(new TilkjentYtelseDifferanse(datoEndringYtelse, datoEndringUtbetalingsgrad, differanseYtelse));
    }

    private FamilieYtelseType finnFamilieYtelseType(Behandling behandling) {
        FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            return gjelderFødsel(behandling.getId())
                ? FamilieYtelseType.FØDSEL
                : FamilieYtelseType.ADOPSJON;
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsakYtelseType)) {
            return FamilieYtelseType.SVANGERSKAPSPENGER;
        } else {
            return null;
        }
    }

    private boolean gjelderFødsel(Long behandlingId) {
        return familieHendelseRepository.hentAggregat(behandlingId)
            .getGjeldendeVersjon().getGjelderFødsel();
    }
}
