package no.nav.foreldrepenger.ytelse.beregning;

import static no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra;

import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.RegelmodellOversetter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.adapter.SammenlignBeregningsresultatFeriepengerMedRegelResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.foreldrepenger.ytelse.beregning.regler.feriepenger.RegelBeregnFeriepenger;

public abstract class BeregnFeriepengerTjeneste {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    protected int antallDagerFeriepenger;

    protected BeregnFeriepengerTjeneste() {
        //NOSONAR
    }

    public BeregnFeriepengerTjeneste(BehandlingRepositoryProvider repositoryProvider, int antallDagerFeriepenger) {
        if (antallDagerFeriepenger == 0) {
            throw new IllegalStateException(
                "Injeksjon av antallDagerFeriepenger feilet. antallDagerFeriepenger kan ikke være 0.");
        }
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.antallDagerFeriepenger = antallDagerFeriepenger;
    }

    public void beregnFeriepenger(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {

        var annenPartsBehandling = finnAnnenPartsBehandling(behandling);
        var annenPartsBeregningsresultat = annenPartsBehandling.map(Behandling::getId)
            .flatMap(beregningsresultatRepository::hentUtbetBeregningsresultat);
        var gjeldendeDekningsgrad = fagsakRelasjonRepository.finnRelasjonFor(behandling.getFagsak())
            .getGjeldendeDekningsgrad();

        var regelModell = mapFra(behandling, beregningsresultat, annenPartsBeregningsresultat, gjeldendeDekningsgrad,
            finnTigjengeligeFeriepengedager());
        var regelInput = toJson(regelModell);

        var regelBeregnFeriepenger = new RegelBeregnFeriepenger();
        var evaluation = regelBeregnFeriepenger.evaluer(regelModell);
        var sporing = RegelmodellOversetter.getSporing(evaluation);

        MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(beregningsresultat, regelModell, regelInput, sporing);
    }

    public boolean avvikBeregnetFeriepengerBeregningsresultat(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {

        var annenPartsBehandling = finnAnnenPartsBehandling(behandling);
        var annenPartsBeregningsresultat = annenPartsBehandling.map(Behandling::getId)
            .flatMap(beregningsresultatRepository::hentUtbetBeregningsresultat);
        var gjeldendeDekningsgrad = fagsakRelasjonRepository.finnRelasjonFor(behandling.getFagsak())
            .getGjeldendeDekningsgrad();

        var regelModell = mapFra(behandling, beregningsresultat, annenPartsBeregningsresultat, gjeldendeDekningsgrad,
            finnTigjengeligeFeriepengedager());

        var regelBeregnFeriepenger = new RegelBeregnFeriepenger();
        regelBeregnFeriepenger.evaluer(regelModell);

        return SammenlignBeregningsresultatFeriepengerMedRegelResultat.erAvvik(beregningsresultat, regelModell);
    }

    private Optional<Behandling> finnAnnenPartsBehandling(Behandling behandling) {
        var annenFagsakOpt = finnAnnenPartsFagsak(behandling.getFagsak());
        return annenFagsakOpt.flatMap(
            fagsak -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()));
    }

    private Optional<Fagsak> finnAnnenPartsFagsak(Fagsak fagsak) {
        var optionalFagsakRelasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak);
        return optionalFagsakRelasjon.flatMap(fagsakRelasjon -> fagsakRelasjon.getFagsakNrEn()
            .equals(fagsak) ? fagsakRelasjon.getFagsakNrTo() : Optional.of(fagsakRelasjon.getFagsakNrEn()));
    }

    private String toJson(BeregningsresultatFeriepengerRegelModell grunnlag) {
        return StandardJsonConfig.toJson(grunnlag);
    }

    protected abstract int finnTigjengeligeFeriepengedager();
}
