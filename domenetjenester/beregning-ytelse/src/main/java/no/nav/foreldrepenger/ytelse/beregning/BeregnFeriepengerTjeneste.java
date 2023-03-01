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
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapInputFraVLTilRegelGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.adapter.SammenlignBeregningsresultatFeriepengerMedRegelResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegler;

public abstract class BeregnFeriepengerTjeneste {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private MapInputFraVLTilRegelGrunnlag inputTjeneste;
    protected int antallDagerFeriepenger;

    protected BeregnFeriepengerTjeneste() {
        //NOSONAR
    }

    protected BeregnFeriepengerTjeneste(BehandlingRepositoryProvider repositoryProvider,
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
        var annenPartArbeidstakerVedSTP = annenPartsBehandling.map(BehandlingReferanse::fra)
            .map(inputTjeneste::arbeidstakerVedSkjæringstidspunkt).orElse(false);
        Optional<BeregningsresultatEntitet> annenPartsBeregningsresultat = annenPartArbeidstakerVedSTP ?
            annenPartsBehandling.map(Behandling::getId).flatMap(beregningsresultatRepository::hentUtbetBeregningsresultat) : Optional.empty();
        var gjeldendeDekningsgrad = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(ref.fagsakId()).orElseThrow()
            .getGjeldendeDekningsgrad();

        var grunnlag = mapFra(ref, beregningsresultat, annenPartsBeregningsresultat, gjeldendeDekningsgrad,
            arbeidstakerVedSTP, finnTigjengeligeFeriepengedager(ref, beregningsresultat));

        var resultat = BeregningsresultatRegler.fastsettFeriepenger(grunnlag);

        MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(beregningsresultat, resultat);
    }

    public boolean avvikBeregnetFeriepengerBeregningsresultat(BehandlingReferanse ref) {
        return beregningsresultatRepository.hentUtbetBeregningsresultat(ref.behandlingId())
            .map(br -> avvikBeregnetFeriepengerBeregningsresultat(ref, br)).orElse(false);
    }

    public boolean avvikBeregnetFeriepengerBeregningsresultat(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat) {

        var arbeidstakerVedSTP = inputTjeneste.arbeidstakerVedSkjæringstidspunkt(ref);
        var annenPartsBehandling = finnAnnenPartsBehandling(ref);
        var annenPartArbeidstakerVedSTP = annenPartsBehandling.map(BehandlingReferanse::fra)
            .map(inputTjeneste::arbeidstakerVedSkjæringstidspunkt).orElse(false);
        Optional<BeregningsresultatEntitet> annenPartsBeregningsresultat = annenPartArbeidstakerVedSTP ?
            annenPartsBehandling.map(Behandling::getId).flatMap(beregningsresultatRepository::hentUtbetBeregningsresultat) : Optional.empty();
        var gjeldendeDekningsgrad = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(ref.fagsakId()).orElseThrow()
            .getGjeldendeDekningsgrad();

        var grunnlag = mapFra(ref, beregningsresultat, annenPartsBeregningsresultat, gjeldendeDekningsgrad,
            arbeidstakerVedSTP, finnTigjengeligeFeriepengedager(ref, beregningsresultat));

        var resultat = BeregningsresultatRegler.fastsettFeriepenger(grunnlag);

        return SammenlignBeregningsresultatFeriepengerMedRegelResultat.erAvvik(beregningsresultat, resultat.resultat());
    }

    private Optional<Behandling> finnAnnenPartsBehandling(BehandlingReferanse ref) {
        return finnAnnenPartsFagsak(ref)
            .flatMap(fagsak -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()));
    }

    private Optional<Fagsak> finnAnnenPartsFagsak(BehandlingReferanse ref) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(ref.fagsakId())
            .flatMap(fagsakRelasjon -> fagsakRelasjon.getRelatertFagsakFraId(ref.fagsakId()));
    }

    protected abstract int finnTigjengeligeFeriepengedager(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat);
}
