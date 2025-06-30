package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsaktiviteterDto;
import no.nav.foreldrepenger.domene.rest.historikk.BeregningsaktivitetHistorikkKalkulusTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsaktiviteterDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsaktivitetOverstyringshåndterer implements Overstyringshåndterer<OverstyrBeregningsaktiviteterDto> {

    private BeregningTjeneste beregningTjeneste;
    private BeregningsaktivitetHistorikkKalkulusTjeneste beregningsaktivitetHistorikkKalkulusTjeneste;

    BeregningsaktivitetOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsaktivitetOverstyringshåndterer(BeregningTjeneste beregningTjeneste,
                                                    BeregningsaktivitetHistorikkKalkulusTjeneste beregningsaktivitetHistorikkKalkulusTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
        this.beregningsaktivitetHistorikkKalkulusTjeneste = beregningsaktivitetHistorikkKalkulusTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsaktiviteterDto dto, BehandlingReferanse ref) {
        var endringsaggregat = beregningTjeneste.overstyrBeregning(dto, ref);
        endringsaggregat.ifPresent(
            oppdaterBeregningsgrunnlagResultat -> beregningsaktivitetHistorikkKalkulusTjeneste.lagHistorikk(ref, dto.getBegrunnelse(),
                oppdaterBeregningsgrunnlagResultat));
        return OppdateringResultat.utenOverhopp();
    }
}
