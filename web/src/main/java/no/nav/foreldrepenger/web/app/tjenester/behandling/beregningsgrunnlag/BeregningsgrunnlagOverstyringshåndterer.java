package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.FaktaBeregningHistorikkKalkulusTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsgrunnlagDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsgrunnlagOverstyringshåndterer implements Overstyringshåndterer<OverstyrBeregningsgrunnlagDto> {

    private BeregningTjeneste beregningTjeneste;
    private FaktaBeregningHistorikkKalkulusTjeneste faktaBeregningHistorikkKalkulusTjeneste;
    private BehandlingRepository behandlingRepository;

    BeregningsgrunnlagOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsgrunnlagOverstyringshåndterer(FaktaBeregningHistorikkKalkulusTjeneste faktaBeregningHistorikkKalkulusTjeneste,
                                                   BeregningTjeneste beregningTjeneste, BehandlingRepository behandlingRepository) {
        this.faktaBeregningHistorikkKalkulusTjeneste = faktaBeregningHistorikkKalkulusTjeneste;
        this.beregningTjeneste = beregningTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsgrunnlagDto dto, BehandlingReferanse ref) {
        var endringsaggregat = beregningTjeneste.overstyrBeregning(dto, ref);
        endringsaggregat.ifPresent(
            oppdaterBeregningsgrunnlagResultat -> faktaBeregningHistorikkKalkulusTjeneste.lagHistorikk(ref, oppdaterBeregningsgrunnlagResultat,
                dto.getBegrunnelse()));
        var builder = OppdateringResultat.utenTransisjon();
        fjernOverstyrtAksjonspunkt(ref)
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    private Optional<Aksjonspunkt> fjernOverstyrtAksjonspunkt(BehandlingReferanse ref) {
        return behandlingRepository.hentBehandling(ref.behandlingId())
            .getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
    }
}
