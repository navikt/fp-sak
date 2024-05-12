package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.map;

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
                                      Long tidligereStønadskontoId) {
        var resultat = beregnKontoerMedResultat(ref, ytelseFordelingAggregat, dekningsgrad,
            annenpartsGjeldendeUttaksplan, ytelsespesifiktGrunnlag, tidligereUtregning);
        var nyUtregning = resultat.getStønadskontoer().entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .collect(Collectors.toMap(e -> map(e.getKey()), Map.Entry::getValue));
        var nyUtregningBeholdkontoer = resultat.getStønadskontoerBeholdStønadsdager().entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .collect(Collectors.toMap(e -> map(e.getKey()), Map.Entry::getValue));
        if (!tidligereUtregning.equals(nyUtregning)) {
            LOG.info("FPSAK migrer konto til UR max endret for behandling {} konto {} fra {} til {}", ref.behandlingId(), tidligereStønadskontoId, tidligereUtregning, nyUtregning);
        }
        if (!tidligereUtregning.equals(nyUtregningBeholdkontoer)) {
            LOG.info("FPSAK migrer konto til UR behold endret for behandling {} konto {} fra {} til {}", ref.behandlingId(), tidligereStønadskontoId, tidligereUtregning, nyUtregningBeholdkontoer);
        }
        if (!nyUtregningBeholdkontoer.equals(nyUtregning) || !tidligereUtregning.equals(nyUtregning) || !tidligereUtregning.equals(nyUtregningBeholdkontoer)) {
            LOG.info("FPSAK migrer konto diff for behandling {} konto {} fra {} til {} eller {}", ref.behandlingId(), tidligereStønadskontoId, tidligereUtregning, nyUtregning, nyUtregningBeholdkontoer);
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
}
