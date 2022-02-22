package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsgrunnlagDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsgrunnlagOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyrBeregningsgrunnlagDto> {

    private BeregningTjeneste beregningTjeneste;

    BeregningsgrunnlagOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsgrunnlagOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter, BeregningTjeneste beregningTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG);
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsgrunnlagDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        beregningTjeneste.overstyr(BehandlingReferanse.fra(behandling), dto);
        var builder = OppdateringResultat.utenTransisjon();
        fjernOverstyrtAksjonspunkt(behandling).ifPresent(
            ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    private Optional<Aksjonspunkt> fjernOverstyrtAksjonspunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrBeregningsgrunnlagDto dto) {
        beregningTjeneste.lagOverstyrHistorikk(BehandlingReferanse.fra(behandling), dto, getHistorikkAdapter().tekstBuilder());
    }
}
