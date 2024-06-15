package no.nav.foreldrepenger.ytelse.beregning;

import static no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra;

import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapInputFraVLTilRegelGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.adapter.SammenlignBeregningsresultatFeriepengerMedRegelResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegler;

public abstract class BeregnFeriepengerTjeneste {

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private MapInputFraVLTilRegelGrunnlag inputTjeneste;
    protected int antallDagerFeriepenger;

    protected BeregnFeriepengerTjeneste() {
        // CDI
    }

    protected BeregnFeriepengerTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                        MapInputFraVLTilRegelGrunnlag inputTjeneste,
                                        FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                        DekningsgradTjeneste dekningsgradTjeneste,
                                        int antallDagerFeriepenger) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        if (antallDagerFeriepenger == 0) {
            throw new IllegalStateException(
                "Injeksjon av antallDagerFeriepenger feilet. antallDagerFeriepenger kan ikke være 0.");
        }
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.inputTjeneste = inputTjeneste;
        this.antallDagerFeriepenger = antallDagerFeriepenger;
    }


    public BeregningsresultatFeriepenger beregnFeriepenger(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat) {

        var arbeidstakerVedSTP = inputTjeneste.arbeidstakerVedSkjæringstidspunkt(ref);
        var annenPartsBehandling = finnAnnenPartsBehandling(ref);
        var annenPartArbeidstakerVedSTP = annenPartsBehandling.map(BehandlingReferanse::fra)
            .map(inputTjeneste::arbeidstakerVedSkjæringstidspunkt).orElse(false);
        Optional<BeregningsresultatEntitet> annenPartsBeregningsresultat = annenPartArbeidstakerVedSTP ?
            annenPartsBehandling.map(Behandling::getId).flatMap(beregningsresultatRepository::hentUtbetBeregningsresultat) : Optional.empty();
        var gjeldendeDekningsgrad = dekningsgradTjeneste.finnGjeldendeDekningsgrad(ref);

        var grunnlag = mapFra(ref, beregningsresultat, annenPartsBeregningsresultat, gjeldendeDekningsgrad,
            arbeidstakerVedSTP, finnTigjengeligeFeriepengedager(ref, beregningsresultat));

        var resultat = BeregningsresultatRegler.fastsettFeriepenger(grunnlag);

        return MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(resultat);
    }

    public boolean avvikBeregnetFeriepengerBeregningsresultat(BehandlingReferanse ref) {
        return beregningsresultatRepository.hentBeregningsresultatAggregat(ref.behandlingId())
            .map(br -> avvikBeregnetFeriepengerBeregningsresultat(ref, br)).orElse(false);
    }

    public boolean avvikBeregnetFeriepengerBeregningsresultat(BehandlingReferanse ref, BehandlingBeregningsresultatEntitet beregningsresultat) {

        var arbeidstakerVedSTP = inputTjeneste.arbeidstakerVedSkjæringstidspunkt(ref);
        var annenPartsBehandling = finnAnnenPartsBehandling(ref);
        var annenPartArbeidstakerVedSTP = annenPartsBehandling.map(BehandlingReferanse::fra)
            .map(inputTjeneste::arbeidstakerVedSkjæringstidspunkt).orElse(false);
        Optional<BeregningsresultatEntitet> annenPartsBeregningsresultat = annenPartArbeidstakerVedSTP ?
            annenPartsBehandling.map(Behandling::getId).flatMap(beregningsresultatRepository::hentUtbetBeregningsresultat) : Optional.empty();
        var gjeldendeDekningsgrad = dekningsgradTjeneste.finnGjeldendeDekningsgrad(ref);

        var grunnlag = mapFra(ref, beregningsresultat.getGjeldendeBeregningsresultat(), annenPartsBeregningsresultat, gjeldendeDekningsgrad,
            arbeidstakerVedSTP, finnTigjengeligeFeriepengedager(ref, beregningsresultat.getGjeldendeBeregningsresultat()));

        var resultat = BeregningsresultatRegler.fastsettFeriepenger(grunnlag);

        return SammenlignBeregningsresultatFeriepengerMedRegelResultat.erAvvik(beregningsresultat.getGjeldendeFeriepenger(), resultat.resultat());
    }

    private Optional<Behandling> finnAnnenPartsBehandling(BehandlingReferanse ref) {
        return finnAnnenPartsFagsak(ref)
            .flatMap(fagsak -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()));
    }

    private Optional<Fagsak> finnAnnenPartsFagsak(BehandlingReferanse ref) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(ref.fagsakId())
            .flatMap(fagsakRelasjon -> fagsakRelasjon.getRelatertFagsakFraId(ref.fagsakId()));
    }

    protected abstract int finnTigjengeligeFeriepengedager(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat);
}
