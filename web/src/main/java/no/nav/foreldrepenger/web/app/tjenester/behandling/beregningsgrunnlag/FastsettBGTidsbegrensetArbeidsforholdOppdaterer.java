package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBGTidsbegrensetArbeidsforholdDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBGTidsbegrensetArbeidsforholdOppdaterer implements AksjonspunktOppdaterer<FastsettBGTidsbegrensetArbeidsforholdDto> {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;
    private BehandlingRepository behandlingRepository;

    FastsettBGTidsbegrensetArbeidsforholdOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBGTidsbegrensetArbeidsforholdOppdaterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                           FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste,
                                                           BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                                           BeregningHåndterer beregningHåndterer, BehandlingRepository behandlingRepository) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste = fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBGTidsbegrensetArbeidsforholdDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var aktivtBG = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(behandling.getId());
        var forrigeGrunnlag = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitet(param.getBehandlingId(), BeregningsgrunnlagTilstand.FORESLÅTT_UT)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        var tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().fagsakYtelseType());
        beregningHåndterer.håndterFastsettBGTidsbegrensetArbeidsforhold(tjeneste.lagInput(param.getRef().behandlingId()), OppdatererDtoMapper.mapFastsettBGTidsbegrensetArbeidsforholdDto(dto));
        fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste.lagHistorikk(param, aktivtBG, forrigeGrunnlag, dto);
        var builder = OppdateringResultat.utenTransisjon();
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
