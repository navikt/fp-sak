package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoRegelOrkestrering;
import no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoResultat;

import java.util.Optional;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.map;


@ApplicationScoped
public class StønadskontoRegelAdapter {

    private final StønadskontoRegelOrkestrering stønadskontoRegel = new StønadskontoRegelOrkestrering();
    private final StønadskontoRegelOversetter stønadskontoRegelOversetter = new StønadskontoRegelOversetter();

    public Stønadskontoberegning beregnKontoer(BehandlingReferanse ref,
                                               YtelseFordelingAggregat ytelseFordelingAggregat,
                                               FagsakRelasjon fagsakRelasjon,
                                               Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                               ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        var resultat = beregnKontoerMedResultat(ref, ytelseFordelingAggregat, fagsakRelasjon,
            annenpartsGjeldendeUttaksplan, ytelsespesifiktGrunnlag);
        return konverterTilStønadskontoberegning(resultat);
    }

    public StønadskontoResultat beregnKontoerMedResultat(BehandlingReferanse ref,
                                                         YtelseFordelingAggregat ytelseFordelingAggregat,
                                                         FagsakRelasjon fagsakRelasjon,
                                                         Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                                         ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        var grunnlag = stønadskontoRegelOversetter.tilRegelmodell(ytelseFordelingAggregat,
            fagsakRelasjon, annenpartsGjeldendeUttaksplan, ytelsespesifiktGrunnlag, ref);

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
}
