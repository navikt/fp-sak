package no.nav.foreldrepenger.ytelse.beregning;

import static no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra;

import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapInputFraVLTilRegelGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.adapter.SammenlignBeregningsresultatFeriepengerMedRegelResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.foreldrepenger.ytelse.beregning.regler.feriepenger.RegelBeregnFeriepenger;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;

public abstract class BeregnFeriepengerTjeneste {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private MapInputFraVLTilRegelGrunnlag inputTjeneste;
    protected int antallDagerFeriepenger;

    protected BeregnFeriepengerTjeneste() {
        //NOSONAR
    }

    public BeregnFeriepengerTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                     MapInputFraVLTilRegelGrunnlag inputTjeneste,
                                     int antallDagerFeriepenger) {
        if (antallDagerFeriepenger == 0) {
            throw new IllegalStateException(
                "Injeksjon av antallDagerFeriepenger feilet. antallDagerFeriepenger kan ikke være 0.");
        }
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.inputTjeneste = inputTjeneste;
        this.antallDagerFeriepenger = antallDagerFeriepenger;
    }


    public void beregnFeriepenger(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat) {

        var arbeidstakerVedSTP = inputTjeneste.arbeidstakerVedSkjæringstidspunkt(ref);
        var annenPartsBehandling = finnAnnenPartsBehandling(ref);
        var annenPartsBeregningsresultat = annenPartsBehandling.map(Behandling::getId)
            .flatMap(beregningsresultatRepository::hentUtbetBeregningsresultat);
        var gjeldendeDekningsgrad = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(ref.fagsakId()).orElseThrow()
            .getGjeldendeDekningsgrad();

        var regelModell = mapFra(ref, beregningsresultat, annenPartsBeregningsresultat, gjeldendeDekningsgrad,
            arbeidstakerVedSTP, finnTigjengeligeFeriepengedager(ref, beregningsresultat));
        var regelInput = toJson(regelModell);

        var regelBeregnFeriepenger = new RegelBeregnFeriepenger();
        var evaluation = regelBeregnFeriepenger.evaluer(regelModell);
        var sporing = EvaluationSerializer.asJson(evaluation);

        MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(beregningsresultat, regelModell, regelInput, sporing);
    }

    public boolean avvikBeregnetFeriepengerBeregningsresultat(BehandlingReferanse ref) {
        return beregningsresultatRepository.hentUtbetBeregningsresultat(ref.behandlingId())
            .map(br -> avvikBeregnetFeriepengerBeregningsresultat(ref, br)).orElse(false);
    }

    public boolean avvikBeregnetFeriepengerBeregningsresultat(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat) {

        var arbeidstakerVedSTP = inputTjeneste.arbeidstakerVedSkjæringstidspunkt(ref);
        var annenPartsBehandling = finnAnnenPartsBehandling(ref);
        var annenPartsBeregningsresultat = annenPartsBehandling.map(Behandling::getId)
            .flatMap(beregningsresultatRepository::hentUtbetBeregningsresultat);
        var gjeldendeDekningsgrad = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(ref.fagsakId()).orElseThrow()
            .getGjeldendeDekningsgrad();

        var regelModell = mapFra(ref, beregningsresultat, annenPartsBeregningsresultat, gjeldendeDekningsgrad,
            arbeidstakerVedSTP, finnTigjengeligeFeriepengedager(ref, beregningsresultat));

        var regelBeregnFeriepenger = new RegelBeregnFeriepenger();
        regelBeregnFeriepenger.evaluer(regelModell);

        return SammenlignBeregningsresultatFeriepengerMedRegelResultat.erAvvik(beregningsresultat, regelModell);
    }

    private Optional<Behandling> finnAnnenPartsBehandling(BehandlingReferanse ref) {
        return finnAnnenPartsFagsak(ref)
            .flatMap(fagsak -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()));
    }

    private Optional<Fagsak> finnAnnenPartsFagsak(BehandlingReferanse ref) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(ref.fagsakId())
            .flatMap(fagsakRelasjon -> fagsakRelasjon.getRelatertFagsakFraId(ref.fagsakId()));
    }

    private String toJson(BeregningsresultatFeriepengerRegelModell grunnlag) {
        return StandardJsonConfig.toJson(grunnlag);
    }

    protected abstract int finnTigjengeligeFeriepengedager(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat);
}
