package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrRefusjonDto;
import no.nav.foreldrepenger.domene.rest.historikk.FaktaBeregningHistorikkKalkulusTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrRefusjonDto.class, adapter = Overstyringshåndterer.class)
public class RefusjonOverstyringOppdaterer implements Overstyringshåndterer<OverstyrRefusjonDto> {

    private BeregningTjeneste beregningTjeneste;
    private FaktaBeregningHistorikkKalkulusTjeneste faktaBeregningHistorikkKalkulusTjeneste;

    RefusjonOverstyringOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public RefusjonOverstyringOppdaterer(FaktaBeregningHistorikkKalkulusTjeneste faktaBeregningHistorikkKalkulusTjeneste,
                                         BeregningTjeneste beregningTjeneste) {
        this.faktaBeregningHistorikkKalkulusTjeneste = faktaBeregningHistorikkKalkulusTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrRefusjonDto dto, BehandlingReferanse ref) {
        var endringsaggregat = beregningTjeneste.overstyrBeregning(dto, ref);
        endringsaggregat.ifPresent(
            oppdaterBeregningsgrunnlagResultat -> faktaBeregningHistorikkKalkulusTjeneste.lagHistorikk(ref, oppdaterBeregningsgrunnlagResultat,
                dto.getBegrunnelse()));
        var builder = OppdateringResultat.utenTransisjon();
        return builder.build();
    }
}
