package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.map;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoRegelOrkestrering;
import no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoResultat;


@Dependent
public class StønadskontoRegelAdapter {

    private static final StønadskontoRegelOrkestrering STØNADSKONTO_REGEL = new StønadskontoRegelOrkestrering();

    private final UttakCore2024 uttakCore2024;

    @Inject
    public StønadskontoRegelAdapter(UttakCore2024 uttakCore2024) {
        this.uttakCore2024 = uttakCore2024;
    }

    public Optional<Stønadskontoberegning> beregnKontoerSjekkDiff(BehandlingReferanse ref,
                                                                  Skjæringstidspunkt skjæringstidspunkt,
                                                                  YtelseFordelingAggregat ytelseFordelingAggregat,
                                                                  Dekningsgrad dekningsgrad,
                                                                  Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                                                  ForeldrepengerGrunnlag ytelsespesifiktGrunnlag,
                                                                  Map<StønadskontoType, Integer> tidligereUtregning) {
        var resultat = beregnKontoerMedResultat(ref, skjæringstidspunkt, ytelseFordelingAggregat, dekningsgrad,
            annenpartsGjeldendeUttaksplan, ytelsespesifiktGrunnlag, tidligereUtregning);
        return endretUtregning(tidligereUtregning, resultat) ? Optional.of(konverterTilStønadskontoberegning(resultat)) : Optional.empty();
    }

    private StønadskontoResultat beregnKontoerMedResultat(BehandlingReferanse ref,
                                                          Skjæringstidspunkt skjæringstidspunkt,
                                                         YtelseFordelingAggregat ytelseFordelingAggregat,
                                                         Dekningsgrad dekningsgrad,
                                                         Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                                         ForeldrepengerGrunnlag ytelsespesifiktGrunnlag,
                                                         Map<StønadskontoType, Integer> tidligereUtregning) {
        var grunnlag = StønadskontoRegelOversetter.tilRegelmodell(ytelseFordelingAggregat, skjæringstidspunkt,
            dekningsgrad, annenpartsGjeldendeUttaksplan, ytelsespesifiktGrunnlag, ref, tidligereUtregning, uttakCore2024);

        return STØNADSKONTO_REGEL.beregnKontoer(grunnlag);
    }

    private Stønadskontoberegning konverterTilStønadskontoberegning(StønadskontoResultat stønadskontoResultat) {
        var stønadskontoberegningBuilder = Stønadskontoberegning.builder()
            .medRegelEvaluering(stønadskontoResultat.getEvalueringResultat())
            .medRegelInput(stønadskontoResultat.getInnsendtGrunnlag())
            .medRegelVersjon(stønadskontoResultat.getRegelVersjon());

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
        return !nyUtregning.equals(tidligereUtregning);
    }

}
