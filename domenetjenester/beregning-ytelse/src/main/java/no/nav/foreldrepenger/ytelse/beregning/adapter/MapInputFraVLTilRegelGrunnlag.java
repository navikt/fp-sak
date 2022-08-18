package no.nav.foreldrepenger.ytelse.beregning.adapter;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.UttakResultatRepoMapper;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;

@ApplicationScoped
public class MapInputFraVLTilRegelGrunnlag {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private Instance<UttakResultatRepoMapper> uttakResultatRepoMapper;

    MapInputFraVLTilRegelGrunnlag() {
        // for CDI
    }

    @Inject
    MapInputFraVLTilRegelGrunnlag(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                  UttakInputTjeneste uttakInputTjeneste,
                                  @Any Instance<UttakResultatRepoMapper> uttakResultatRepoMapper) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.uttakResultatRepoMapper = uttakResultatRepoMapper;
    }

    public BeregningsresultatRegelmodell mapFra(BehandlingReferanse ref) {
        var beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(ref.behandlingId());
        var input = uttakInputTjeneste.lagInput(ref.behandlingId());
        var mapper = FagsakYtelseTypeRef.Lookup.find(this.uttakResultatRepoMapper, input.getFagsakYtelseType()).orElseThrow();
        var regelBeregningsgrunnlag = MapBeregningsgrunnlagFraVLTilRegel.map(beregningsgrunnlag);
        var regelUttakResultat = mapper.hentOgMapUttakResultat(input);
        return new BeregningsresultatRegelmodell(regelBeregningsgrunnlag, regelUttakResultat);
    }

    public boolean arbeidstakerVedSkjæringstidspunkt(BehandlingReferanse ref) {
        return beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(ref.behandlingId())
            .map(MapBeregningsgrunnlagFraVLTilRegel::arbeidstakerVedSkjæringstidspunkt)
            .orElse(false);
    }

}
