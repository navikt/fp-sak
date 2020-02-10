package no.nav.foreldrepenger.ytelse.beregning.adapter;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.UttakResultatRepoMapper;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;

@ApplicationScoped
public class MapBeregningsresultatFraVLTilRegel {

    private Instance<UttakResultatRepoMapper> uttakResultatRepoMapper;

    MapBeregningsresultatFraVLTilRegel() {
        // for CDI
    }

    @Inject
    MapBeregningsresultatFraVLTilRegel(@Any Instance<UttakResultatRepoMapper> uttakResultatRepoMapper) {
        this.uttakResultatRepoMapper = uttakResultatRepoMapper;
    }

    public BeregningsresultatRegelmodell mapFra(BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                UttakInput input) {
        var mapper = FagsakYtelseTypeRef.Lookup.find(this.uttakResultatRepoMapper, input.getFagsakYtelseType()).orElseThrow();
        var regelBeregningsgrunnlag = MapBeregningsgrunnlagFraVLTilRegel.map(beregningsgrunnlag);
        UttakResultat regelUttakResultat = mapper.hentOgMapUttakResultat(input);
        return new BeregningsresultatRegelmodell(regelBeregningsgrunnlag, regelUttakResultat);
    }

}
