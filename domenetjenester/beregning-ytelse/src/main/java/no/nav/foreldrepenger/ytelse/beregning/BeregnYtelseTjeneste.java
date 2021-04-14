package no.nav.foreldrepenger.ytelse.beregning;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFraVLTilRegel;

@ApplicationScoped
public class BeregnYtelseTjeneste {
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private MapBeregningsresultatFraVLTilRegel mapBeregningsresultatFraVLTilRegel;

    public BeregnYtelseTjeneste() {
        // CDI
    }

    @Inject
    public BeregnYtelseTjeneste(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste,
                                UttakInputTjeneste uttakInputTjeneste,
                                MapBeregningsresultatFraVLTilRegel mapBeregningsresultatFraVLTilRegel) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.fastsettBeregningsresultatTjeneste = fastsettBeregningsresultatTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.mapBeregningsresultatFraVLTilRegel = mapBeregningsresultatFraVLTilRegel;
    }

    public BeregningsresultatEntitet beregnYtelse(BehandlingReferanse referanse) {
        var behandlingId = referanse.getBehandlingId();
        var input = uttakInputTjeneste.lagInput(behandlingId);

        var beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(behandlingId);

        // Map til regelmodell
        var regelmodell = mapBeregningsresultatFraVLTilRegel.mapFra(beregningsgrunnlag, input);

        // Verifiser input til regel
        if (andelerIBeregningMåLiggeIUttak(referanse)) {
            BeregningsresultatInputVerifiserer.verifiserAlleAndelerIBeregningErIUttak(regelmodell);
        } else {
            BeregningsresultatInputVerifiserer.verifiserAndelerIUttakLiggerIBeregning(regelmodell);
        }

        // Kalle regeltjeneste
        var beregningsresultat = fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(regelmodell);

        // Verifiser beregningsresultat
        BeregningsresultatOutputVerifiserer.verifiserOutput(beregningsresultat);

        return beregningsresultat;
    }

    private boolean andelerIBeregningMåLiggeIUttak(BehandlingReferanse ref) {
        return ref.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER);
    }
}
