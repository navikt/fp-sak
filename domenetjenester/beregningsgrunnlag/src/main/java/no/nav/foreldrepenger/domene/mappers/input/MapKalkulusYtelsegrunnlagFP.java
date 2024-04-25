package no.nav.foreldrepenger.domene.mappers.input;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulus.beregning.v1.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class MapKalkulusYtelsegrunnlagFP implements MapKalkulusYtelsegrunnlag {
    private DekningsgradTjeneste dekningsgradTjeneste;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    // private BeregningUttakTjeneste beregningUttakTjeneste;

    MapKalkulusYtelsegrunnlagFP() {
        // CDI
    }

    @Inject
    public MapKalkulusYtelsegrunnlagFP(DekningsgradTjeneste dekningsgradTjeneste,
                                       BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste) {
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
    }

    @Override
    public YtelsespesifiktGrunnlagDto mapYtelsegrunnlag(BehandlingReferanse referanse) {
        var dekningsgrad = BigDecimal.valueOf(dekningsgradTjeneste.finnGjeldendeDekningsgrad(referanse).getVerdi());
        var kanBesteberegnes = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(referanse);
        return new ForeldrepengerGrunnlag(dekningsgrad, kanBesteberegnes, null, Collections.emptyList());
    }
}
