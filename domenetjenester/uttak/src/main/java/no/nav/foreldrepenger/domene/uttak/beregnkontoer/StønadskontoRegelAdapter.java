package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.map;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.regler.uttak.beregnkontoer.StønadskontoRegelOrkestrering;
import no.nav.foreldrepenger.regler.uttak.beregnkontoer.StønadskontoResultat;
import no.nav.foreldrepenger.regler.uttak.beregnkontoer.grunnlag.BeregnKontoerGrunnlag;

@ApplicationScoped
public class StønadskontoRegelAdapter {

    private StønadskontoRegelOrkestrering stønadskontoRegel = new StønadskontoRegelOrkestrering();
    private StønadskontoRegelOversetter stønadskontoRegelOversetter = new StønadskontoRegelOversetter();

    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public StønadskontoRegelAdapter(UttakRepositoryProvider uttakRepositoryProvider) {
        this.behandlingsresultatRepository = uttakRepositoryProvider.getBehandlingsresultatRepository();
    }

    StønadskontoRegelAdapter() {
        //CDI
    }

    public Stønadskontoberegning beregnKontoer(BehandlingReferanse ref,
                                               YtelseFordelingAggregat ytelseFordelingAggregat,
                                               FagsakRelasjon fagsakRelasjon,
                                               Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                               ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        var resultat = beregnKontoerMedResultat(ref, ytelseFordelingAggregat, fagsakRelasjon, annenpartsGjeldendeUttaksplan, ytelsespesifiktGrunnlag);
        return konverterTilStønadskontoberegning(resultat);
    }

    public StønadskontoResultat beregnKontoerMedResultat(BehandlingReferanse ref,
                                                         YtelseFordelingAggregat ytelseFordelingAggregat,
                                                         FagsakRelasjon fagsakRelasjon,
                                                         Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                                         ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        boolean harSøkerRett = !behandlingsresultatRepository.hentHvisEksisterer(ref.getBehandlingId()).orElseThrow().isVilkårAvslått();

        BeregnKontoerGrunnlag grunnlag = stønadskontoRegelOversetter.tilRegelmodell(ref.getRelasjonsRolleType(),
            ytelseFordelingAggregat, harSøkerRett, fagsakRelasjon, annenpartsGjeldendeUttaksplan, ytelsespesifiktGrunnlag);

        return stønadskontoRegel.beregnKontoer(grunnlag);
    }

    private Stønadskontoberegning konverterTilStønadskontoberegning(StønadskontoResultat stønadskontoResultat) {
        Stønadskontoberegning.Builder stønadskontoberegningBuilder = Stønadskontoberegning.builder()
            .medRegelEvaluering(stønadskontoResultat.getEvalueringResultat())
            .medRegelInput(stønadskontoResultat.getInnsendtGrunnlag());

        Map<no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype, Integer> maksDagerStønadskonto = stønadskontoResultat.getStønadskontoer();
        for (Map.Entry<no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype, Integer> entry : maksDagerStønadskonto.entrySet()) {
            Stønadskonto stønadskonto = Stønadskonto.builder()
                .medMaxDager(entry.getValue())
                .medStønadskontoType(map(entry.getKey()))
                .build();
            stønadskontoberegningBuilder.medStønadskonto(stønadskonto);
        }
        return stønadskontoberegningBuilder.build();
    }
}
