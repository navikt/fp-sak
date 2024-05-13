package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.map;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoRegelOrkestrering;
import no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoResultat;


@ApplicationScoped
public class StønadskontoRegelAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(StønadskontoRegelAdapter.class);

    private final StønadskontoRegelOrkestrering stønadskontoRegel = new StønadskontoRegelOrkestrering();
    private final StønadskontoRegelOversetter stønadskontoRegelOversetter = new StønadskontoRegelOversetter();

    public Stønadskontoberegning beregnKontoer(BehandlingReferanse ref,
                                               YtelseFordelingAggregat ytelseFordelingAggregat,
                                               Dekningsgrad dekningsgrad,
                                               Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                               ForeldrepengerGrunnlag ytelsespesifiktGrunnlag,
                                               Map<StønadskontoType, Integer> tidligereUtregning) {
        var resultat = beregnKontoerMedResultat(ref, ytelseFordelingAggregat, dekningsgrad,
            annenpartsGjeldendeUttaksplan, ytelsespesifiktGrunnlag, tidligereUtregning);
        return konverterTilStønadskontoberegning(resultat);
    }

    public Optional<Stønadskontoberegning> beregnKontoerSjekkDiff(BehandlingReferanse ref,
                                                                  YtelseFordelingAggregat ytelseFordelingAggregat,
                                                                  Dekningsgrad dekningsgrad,
                                                                  Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                                                  ForeldrepengerGrunnlag ytelsespesifiktGrunnlag,
                                                                  Map<StønadskontoType, Integer> tidligereUtregning) {
        var resultat = beregnKontoerMedResultat(ref, ytelseFordelingAggregat, dekningsgrad,
            annenpartsGjeldendeUttaksplan, ytelsespesifiktGrunnlag, tidligereUtregning);
        return endretUtregning(tidligereUtregning, resultat) ? Optional.of(konverterTilStønadskontoberegning(resultat)) : Optional.empty();
    }

    public void beregnKontoerLoggDiff(BehandlingReferanse ref,
                                      YtelseFordelingAggregat ytelseFordelingAggregat,
                                      Dekningsgrad dekningsgrad,
                                      Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                      ForeldrepengerGrunnlag ytelsespesifiktGrunnlag,
                                      Map<StønadskontoType, Integer> tidligereUtregning,
                                      Long tidligereStønadskontoId, boolean sakStengt) {
        var resultat = beregnKontoerMedResultat(ref, ytelseFordelingAggregat, dekningsgrad,
            annenpartsGjeldendeUttaksplan, ytelsespesifiktGrunnlag, tidligereUtregning);
        if (sakStengt) { // Tilfelle der fellesperioden var 130 dager i 2018 (da funker fletteMax dårlig)
            var nyUtregningBeholdkontoer = resultat.getStønadskontoerBeholdStønadsdager().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(e -> map(e.getKey()), Map.Entry::getValue));
            if (!tidligereUtregning.equals(nyUtregningBeholdkontoer)) {
                LOG.info("FPSAK migrer konto til UR behold endret for behandling {} konto {} fra {} til {}", ref.behandlingId(), tidligereStønadskontoId, tidligereUtregning, nyUtregningBeholdkontoer);
            }
            return;
        }
        var nyUtregning = resultat.getStønadskontoer().entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .collect(Collectors.toMap(e -> map(e.getKey()), Map.Entry::getValue));
        var nyUtregningBeholdkontoer = resultat.getStønadskontoerBeholdStønadsdager().entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .collect(Collectors.toMap(e -> map(e.getKey()), Map.Entry::getValue));
        if (!tidligereUtregning.equals(nyUtregning)) {
            var endringer = utledEndringer(tidligereUtregning, nyUtregning);
            LOG.info("FPSAK migrer konto til UR max endret for behandling {} konto {} endringer {}", ref.behandlingId(), tidligereStønadskontoId, endringer);
        }
        if (!tidligereUtregning.equals(nyUtregningBeholdkontoer)) {
            var endringer = utledEndringer(tidligereUtregning, nyUtregningBeholdkontoer);
            LOG.info("FPSAK migrer konto til UR behold endret for behandling {} konto {} endringer {}", ref.behandlingId(), tidligereStønadskontoId, endringer);
        }
        if (!nyUtregningBeholdkontoer.equals(nyUtregning)) {
            var endringer1 = utledEndringer(tidligereUtregning, nyUtregning);
            var endringer2 = utledEndringer(nyUtregning, nyUtregningBeholdkontoer);
            LOG.info("FPSAK migrer konto diff for behandling {} konto {} fra tidligere {} diff {}", ref.behandlingId(), tidligereStønadskontoId, endringer1, endringer2);
        }
    }

    public StønadskontoResultat beregnKontoerMedResultat(BehandlingReferanse ref,
                                                         YtelseFordelingAggregat ytelseFordelingAggregat,
                                                         Dekningsgrad dekningsgrad,
                                                         Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                                         ForeldrepengerGrunnlag ytelsespesifiktGrunnlag,
                                                         Map<StønadskontoType, Integer> tidligereUtregning) {
        var grunnlag = stønadskontoRegelOversetter.tilRegelmodell(ytelseFordelingAggregat,
            dekningsgrad, annenpartsGjeldendeUttaksplan, ytelsespesifiktGrunnlag, ref, tidligereUtregning);

        return stønadskontoRegel.beregnKontoer(grunnlag);
    }

    private Stønadskontoberegning konverterTilStønadskontoberegning(StønadskontoResultat stønadskontoResultat) {
        var stønadskontoberegningBuilder = Stønadskontoberegning.builder()
            .medRegelEvaluering(stønadskontoResultat.getEvalueringResultat())
            .medRegelInput(stønadskontoResultat.getInnsendtGrunnlag());

        var maksDagerStønadskonto = stønadskontoResultat.getStønadskontoer();
        for (var entry : maksDagerStønadskonto.entrySet()) {
            var stønadskonto = Stønadskonto.builder()
                .medMaxDager(entry.getValue())
                .medStønadskontoType(map(entry.getKey()))
                .build();
            stønadskontoberegningBuilder.medStønadskonto(stønadskonto);
        }
        return stønadskontoberegningBuilder.build();
    }

    private boolean endretUtregning(Map<StønadskontoType, Integer> tidligereUtregning, StønadskontoResultat resultat) {
        var nyUtregning = resultat.getStønadskontoer().entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .collect(Collectors.toMap(e -> map(e.getKey()), Map.Entry::getValue));
        var nyUtregningBeholdkontoer = resultat.getStønadskontoerBeholdStønadsdager().entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .collect(Collectors.toMap(e -> map(e.getKey()), Map.Entry::getValue));
        return !nyUtregning.equals(tidligereUtregning);
    }

    private List<KontoEndring> utledEndringer(Map<StønadskontoType, Integer> m1, Map<StønadskontoType, Integer> m2) {
        var nøkler = new HashSet<>(m1.keySet());
        nøkler.addAll(m2.keySet());
        return nøkler.stream()
            .filter(n -> !m1.getOrDefault(n, 0).equals(m2.getOrDefault(n, 0)))
            .map(n -> new KontoEndring(n, m1.getOrDefault(n, 0), m2.getOrDefault(n, 0)))
            .toList();
    }

    private record KontoEndring(StønadskontoType type, Integer dagerEksisterende, Integer dagerNye) { }
}
