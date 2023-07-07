package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.foreldrepenger.domene.rest.historikk.VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;

import java.util.Objects;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderVarigEndringEllerNyoppstartetSNDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderVarigEndringEllerNyoppstartetSNOppdaterer implements AksjonspunktOppdaterer<VurderVarigEndringEllerNyoppstartetSNDto> {

    private VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;

    VurderVarigEndringEllerNyoppstartetSNOppdaterer() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstartetSNOppdaterer(VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste,
                                                           BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                                           BeregningHåndterer beregningHåndterer) {
        this.vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste = vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(VurderVarigEndringEllerNyoppstartetSNDto dto, AksjonspunktOppdaterParameter param) {
        if (dto.getErVarigEndretNaering()) {
            Objects.requireNonNull(dto.getBruttoBeregningsgrunnlag(), "næringsinntekt");
            var tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().fagsakYtelseType());
            var input = tjeneste.lagInput(param.getRef().behandlingId());
            beregningHåndterer.håndterVurderVarigEndretNyoppstartetSN(input, dto.getBruttoBeregningsgrunnlag());
        }
        vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste.lagHistorikkInnslag(param, dto);
        return OppdateringResultat.utenOveropp();
    }
}
