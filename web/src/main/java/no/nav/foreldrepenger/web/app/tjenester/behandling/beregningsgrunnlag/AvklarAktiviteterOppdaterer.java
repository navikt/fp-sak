package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.AvklarteAktiviteterDto;
import no.nav.foreldrepenger.domene.rest.historikk.BeregningsaktivitetHistorikkKalkulusTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarteAktiviteterDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAktiviteterOppdaterer implements AksjonspunktOppdaterer<AvklarteAktiviteterDto> {

    private BeregningsaktivitetHistorikkKalkulusTjeneste beregningsaktivitetHistorikkKalkulusTjeneste;
    private BeregningTjeneste beregningTjeneste;

    AvklarAktiviteterOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAktiviteterOppdaterer(BeregningsaktivitetHistorikkKalkulusTjeneste beregningsaktivitetHistorikkKalkulusTjeneste,
                                       BeregningTjeneste beregningTjeneste) {
        this.beregningsaktivitetHistorikkKalkulusTjeneste = beregningsaktivitetHistorikkKalkulusTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarteAktiviteterDto dto, AksjonspunktOppdaterParameter param) {

        var behandlingReferanse = param.getRef();
        var endringsaggregat = beregningTjeneste.oppdaterBeregning(dto, behandlingReferanse);
        endringsaggregat.ifPresent(
            oppdaterBeregningsgrunnlagResultat -> beregningsaktivitetHistorikkKalkulusTjeneste.lagHistorikk(behandlingReferanse, dto.getBegrunnelse(),
                oppdaterBeregningsgrunnlagResultat));
        return OppdateringResultat.utenOverhopp();
    }

}
