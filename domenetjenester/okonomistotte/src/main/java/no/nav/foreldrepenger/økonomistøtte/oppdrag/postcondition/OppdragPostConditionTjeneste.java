package no.nav.foreldrepenger.økonomistøtte.oppdrag.postcondition;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.EksisterendeOppdragMapper;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.TilkjentYtelseMapper;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste.EndringsdatoTjeneste;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.util.SetUtil;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class OppdragPostConditionTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OppdragPostConditionTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private ØkonomioppdragRepository økonomioppdragRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    OppdragPostConditionTjeneste() {
        //for CDI proxy
    }

    @Inject
    public OppdragPostConditionTjeneste(BehandlingRepository behandlingRepository, BeregningsresultatRepository beregningsresultatRepository, ØkonomioppdragRepository økonomioppdragRepository, FamilieHendelseRepository familieHendelseRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    public void softPostCondition(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsakYtelseType = behandling.getFagsakYtelseType();
        if (fagsakYtelseType == FagsakYtelseType.FORELDREPENGER || fagsakYtelseType == FagsakYtelseType.SVANGERSKAPSPENGER) {
            var beregningsresultat = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId()).orElse(null);
            softPostCondition(behandling, beregningsresultat);
        }
    }

    private void softPostCondition(Behandling behandling, BehandlingBeregningsresultatEntitet beregningsresultat) {
        try {
            sammenlignEffektAvOppdragMedTilkjentYtelseOgLoggAvvik(behandling, beregningsresultat);
        } catch (Exception e) {
            LOG.warn("Teknisk feil ved sammenligning av effekt av oppdrag mot tilkjent ytelse for {} behandling {} . Dette bør undersøkes: {}",
                behandling.getSaksnummer(), behandling.getId(), e.getMessage(), e);
        }
    }

    private boolean sammenlignEffektAvOppdragMedTilkjentYtelseOgLoggAvvik(Behandling behandling, BehandlingBeregningsresultatEntitet beregningsresultat) {
        var resultat = sammenlignEffektAvOppdragMedTilkjentYtelse(behandling, beregningsresultat);
        var altOk = true;
        for (var entry : resultat.entrySet()) {
            var feil = konverterTilFeil(behandling.getSaksnummer(), Long.toString(behandling.getId()), entry.getKey(), entry.getValue());
            if (feil != null) {
                LOG.warn(feil.getMessage());
                if ("FP-767898".equals(feil.getKode())) {
                    altOk = false;
                }
            }
        }
        return altOk;
    }

    private Map<Betalingsmottaker, TilkjentYtelseDifferanse> sammenlignEffektAvOppdragMedTilkjentYtelse(Behandling behandling,
                                                                                                        BehandlingBeregningsresultatEntitet beregningsresultat) {
        var saksnummer = behandling.getSaksnummer();
        var oppdragene = økonomioppdragRepository.finnAlleOppdragForSak(saksnummer);
        var oppdragskjeder = EksisterendeOppdragMapper.tilKjeder(oppdragene);
        var målbilde = beregningsresultat == null ? GruppertYtelse.TOM :
            TilkjentYtelseMapper.lagFor(finnFamilieYtelseType(behandling)).fordelPåNøkler(beregningsresultat);
        var alleKjedenøkler = SetUtil.union(oppdragskjeder.keySet(), målbilde.getNøkler());
        var betalingsmottakere = alleKjedenøkler.stream().map(KjedeNøkkel::getBetalingsmottaker).collect(Collectors.toSet());

        var resultat = new HashMap<Betalingsmottaker, TilkjentYtelseDifferanse>();
        for (var betalingsmottaker : betalingsmottakere) {
            List<TilkjentYtelseDifferanse> differanser = new ArrayList<>();
            for (var nøkkel : alleKjedenøkler) {
                if (nøkkel.getBetalingsmottaker().equals(betalingsmottaker)) {
                    var oppdragKjede = oppdragskjeder.getOrDefault(nøkkel, OppdragKjede.EMPTY);
                    var ytelse = målbilde.getYtelsePrNøkkel().getOrDefault(nøkkel, Ytelse.EMPTY);
                    var effektAvOppdragskjede = oppdragKjede.tilYtelse();
                    finnDifferanse(ytelse, effektAvOppdragskjede, betalingsmottaker).ifPresent(differanser::add);
                }
            }
            var førsteDatoForDifferanseSats = finnLaveste(differanser, TilkjentYtelseDifferanse::førsteDatoForDifferanseSats);
            var førsteDatoForDifferanseUtbetalingsgrad = finnLaveste(differanser, TilkjentYtelseDifferanse::førsteDatoForDifferanseUtbetalingsgrad);
            var sumForskjell = differanser.stream().mapToLong(TilkjentYtelseDifferanse::differanseYtelse).sum();
            resultat.put(betalingsmottaker, new TilkjentYtelseDifferanse(førsteDatoForDifferanseSats, førsteDatoForDifferanseUtbetalingsgrad, sumForskjell));
        }
        return resultat;
    }


    private TekniskException konverterTilFeil(Saksnummer saksnummer, String behandlingId, Betalingsmottaker betalingsmottaker, TilkjentYtelseDifferanse differanse) {
        if (!differanse.harAvvik()) {
            return null;
        }
        var datoEndringYtelse = differanse.førsteDatoForDifferanseSats();
        var datoEndringUtbetalingsgrad = differanse.førsteDatoForDifferanseUtbetalingsgrad();
        var sumForskjell = differanse.differanseYtelse();

        var message = "Sammenligning av effekt av oppdrag mot tilkjent ytelse viser avvik for " + saksnummer + ", behandling " + behandlingId + " til " + betalingsmottaker + ". Dette bør undersøkes og evt. patches. Det er ";
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
        if (sumForskjell == 0) {
            return OppdragValideringFeil.minorValideringsfeil(message);
        }
        return OppdragValideringFeil.valideringsfeil(message);
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

    record TilkjentYtelseDifferanse(LocalDate førsteDatoForDifferanseSats, LocalDate førsteDatoForDifferanseUtbetalingsgrad, long differanseYtelse) {

        public boolean harAvvik() {
            return førsteDatoForDifferanseSats != null || førsteDatoForDifferanseUtbetalingsgrad != null || differanseYtelse != 0;
        }
    }

    static Optional<TilkjentYtelseDifferanse> finnDifferanse(Ytelse ytelse, Ytelse effektAvOppdragskjede, Betalingsmottaker betalingsmottaker) {
        var datoEndringYtelse = EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdatoForEndringSats(ytelse, effektAvOppdragskjede);
        var datoEndringUtbetalingsgrad = betalingsmottaker == Betalingsmottaker.BRUKER
            ? EndringsdatoTjeneste.ignorerDagsatsIHelg().finnEndringsdatoForEndringUtbetalingsgrad(ytelse, effektAvOppdragskjede)
            : null; //utbetalingsgrad er ikke relevant for refusjon
        var differanseYtelse = effektAvOppdragskjede.summerYtelse() - ytelse.summerYtelse();

        if (datoEndringYtelse == null && datoEndringUtbetalingsgrad == null && differanseYtelse == 0) {
            return Optional.empty();
        }
        return Optional.of(new TilkjentYtelseDifferanse(datoEndringYtelse, datoEndringUtbetalingsgrad, differanseYtelse));
    }

    private FamilieYtelseType finnFamilieYtelseType(Behandling behandling) {
        var fagsakYtelseType = behandling.getFagsakYtelseType();
        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            return gjelderFødsel(behandling.getId());
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsakYtelseType)) {
            return FamilieYtelseType.SVANGERSKAPSPENGER;
        }
        return null;
    }

    private FamilieYtelseType gjelderFødsel(Long behandlingId) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .filter(FamilieHendelseEntitet::getGjelderAdopsjon)
            .map(fh -> FamilieYtelseType.ADOPSJON).orElse(FamilieYtelseType.FØDSEL);
    }
}
