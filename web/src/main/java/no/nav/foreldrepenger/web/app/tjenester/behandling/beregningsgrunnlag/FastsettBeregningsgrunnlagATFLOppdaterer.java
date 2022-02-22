package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBeregningsgrunnlagATFLHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBeregningsgrunnlagATFLDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBeregningsgrunnlagATFLOppdaterer implements AksjonspunktOppdaterer<FastsettBeregningsgrunnlagATFLDto> {

    private FastsettBeregningsgrunnlagATFLHistorikkTjeneste fastsettBeregningsgrunnlagATFLHistorikkTjeneste;
    private BeregningTjeneste beregningTjeneste;

    protected FastsettBeregningsgrunnlagATFLOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLOppdaterer(FastsettBeregningsgrunnlagATFLHistorikkTjeneste fastsettBeregningsgrunnlagATFLHistorikkTjeneste,
                                                    BeregningTjeneste beregningTjeneste) {
        this.fastsettBeregningsgrunnlagATFLHistorikkTjeneste = fastsettBeregningsgrunnlagATFLHistorikkTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBeregningsgrunnlagATFLDto dto, AksjonspunktOppdaterParameter param) {
        var oppdaterResultat = beregningTjeneste.oppdater(param, dto);
        oppdaterResultat.getBeregningsgrunnlagEndring().ifPresent(e -> fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(param, dto, e));
        var builder = OppdateringResultat.utenTransisjon();
        håndterEventueltOverflødigAksjonspunkt(param.getBehandling()).ifPresent(
            ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    /*
        Ved tilbakehopp hender det at avbrutte aksjonspunkter gjenopprettes feilaktig. Denne funksjonen sørger for å rydde opp
        i dette for det ene scenarioet det er mulig i beregningsmodulen. Også implementert i det motsatte tilfellet.
        Se https://jira.adeo.no/browse/PFP-2042 for mer informasjon.
     */
    private Optional<Aksjonspunkt> håndterEventueltOverflødigAksjonspunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
    }
}
