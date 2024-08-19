package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.foreldrepenger.domene.rest.historikk.VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderVarigEndringEllerNyoppstartetSNDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderVarigEndringEllerNyoppstartetSNOppdaterer implements AksjonspunktOppdaterer<VurderVarigEndringEllerNyoppstartetSNDto> {

    private VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;
    private BeregningTjeneste beregningTjeneste;

    VurderVarigEndringEllerNyoppstartetSNOppdaterer() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstartetSNOppdaterer(VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste,
                                                           BeregningTjeneste beregningTjeneste) {
        this.vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste = vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderVarigEndringEllerNyoppstartetSNDto dto, AksjonspunktOppdaterParameter param) {
        beregningTjeneste.oppdaterBeregning(dto, param.getRef());
        vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste.lagHistorikkInnslag(param, dto);
        return OppdateringResultat.utenOverhopp();
    }
}
