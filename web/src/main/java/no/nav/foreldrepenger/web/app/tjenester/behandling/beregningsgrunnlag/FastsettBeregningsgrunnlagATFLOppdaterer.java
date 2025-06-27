package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBeregningsgrunnlagATFLDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBeregningsgrunnlagATFLOppdaterer implements AksjonspunktOppdaterer<FastsettBeregningsgrunnlagATFLDto> {

    private BehandlingRepository behandlingRepository;
    private FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste fastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste;
    private BeregningTjeneste beregningTjeneste;

    protected FastsettBeregningsgrunnlagATFLOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLOppdaterer(BehandlingRepository behandlingRepository,
                                                    FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste fastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste,
                                                    BeregningTjeneste beregningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste = fastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBeregningsgrunnlagATFLDto dto, AksjonspunktOppdaterParameter param) {
        var ref = param.getRef();
        var endringsaggregat = beregningTjeneste.oppdaterBeregning(dto, ref);
        endringsaggregat.ifPresent(
            oppdaterBeregningsgrunnlagResultat -> fastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste.lagHistorikk(param, dto,
                oppdaterBeregningsgrunnlagResultat));
        var builder = OppdateringResultat.utenTransisjon();
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        håndterEventueltOverflødigAksjonspunkt(behandling)
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
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
