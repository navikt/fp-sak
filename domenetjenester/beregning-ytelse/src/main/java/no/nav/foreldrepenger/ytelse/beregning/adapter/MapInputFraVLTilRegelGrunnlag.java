package no.nav.foreldrepenger.ytelse.beregning.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.UttakResultatRepoMapper;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatGrunnlag;

@ApplicationScoped
public class MapInputFraVLTilRegelGrunnlag {
    private BeregningTjeneste beregningTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private Instance<UttakResultatRepoMapper> uttakResultatRepoMapper;

    MapInputFraVLTilRegelGrunnlag() {
        // for CDI
    }

    @Inject
    MapInputFraVLTilRegelGrunnlag(BeregningTjeneste beregningTjeneste,
                                  UttakInputTjeneste uttakInputTjeneste,
                                  @Any Instance<UttakResultatRepoMapper> uttakResultatRepoMapper) {
        this.beregningTjeneste = beregningTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.uttakResultatRepoMapper = uttakResultatRepoMapper;
    }

    public BeregningsresultatGrunnlag mapFra(BehandlingReferanse ref) {
        var beregningsgrunnlag = beregningTjeneste.hent(ref).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .orElseThrow(() -> new IllegalArgumentException("Forventer aktivt beregningsgrunnlag under beregning av tilkjent ytelse"));
        var input = uttakInputTjeneste.lagInput(ref.behandlingId());
        var mapper = FagsakYtelseTypeRef.Lookup.find(this.uttakResultatRepoMapper, input.getFagsakYtelseType()).orElseThrow();
        var regelBeregningsgrunnlag = MapBeregningsgrunnlagFraVLTilRegel.map(beregningsgrunnlag);
        var regelUttakResultat = mapper.hentOgMapUttakResultat(input);
        return new BeregningsresultatGrunnlag(regelBeregningsgrunnlag, regelUttakResultat);
    }

    public boolean arbeidstakerVedSkjæringstidspunkt(BehandlingReferanse ref) {
        var beregningsgrunnlag = beregningTjeneste.hent(ref);
        return beregningsgrunnlag.flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .map(MapBeregningsgrunnlagFraVLTilRegel::arbeidstakerVedSkjæringstidspunkt)
            .orElse(false);
    }

}
