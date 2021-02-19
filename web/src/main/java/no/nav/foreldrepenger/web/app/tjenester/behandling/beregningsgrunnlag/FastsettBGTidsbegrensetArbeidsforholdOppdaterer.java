package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBGTidsbegrensetArbeidsforholdDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBGTidsbegrensetArbeidsforholdOppdaterer implements AksjonspunktOppdaterer<FastsettBGTidsbegrensetArbeidsforholdDto> {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;

    FastsettBGTidsbegrensetArbeidsforholdOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBGTidsbegrensetArbeidsforholdOppdaterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                           FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste,
                                                           BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                                           BeregningHåndterer beregningHåndterer) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste = fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBGTidsbegrensetArbeidsforholdDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        BeregningsgrunnlagEntitet aktivtBG = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(behandling.getId());
        Optional<BeregningsgrunnlagEntitet> forrigeGrunnlag = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitet(param.getBehandlingId(), BeregningsgrunnlagTilstand.FORESLÅTT_UT)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        BeregningsgrunnlagInputFelles tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().getFagsakYtelseType());
        beregningHåndterer.håndterFastsettBGTidsbegrensetArbeidsforhold(tjeneste.lagInput(param.getRef().getBehandlingId()), OppdatererDtoMapper.mapFastsettBGTidsbegrensetArbeidsforholdDto(dto));
        fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste.lagHistorikk(param, aktivtBG, forrigeGrunnlag, dto);
        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
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
         return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
    }

}
