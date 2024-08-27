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
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.kalkulus.FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBGTidsbegrensetArbeidsforholdDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBGTidsbegrensetArbeidsforholdOppdaterer implements AksjonspunktOppdaterer<FastsettBGTidsbegrensetArbeidsforholdDto> {

    private FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
    private FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste fastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste beregningTjeneste;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    FastsettBGTidsbegrensetArbeidsforholdOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBGTidsbegrensetArbeidsforholdOppdaterer(FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste,
                                                           FastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste fastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste,
                                                           BehandlingRepository behandlingRepository,
                                                           BeregningTjeneste beregningTjeneste,
                                                           HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste) {
        this.fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste = fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
        this.fastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste = fastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningTjeneste = beregningTjeneste;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBGTidsbegrensetArbeidsforholdDto dto, AksjonspunktOppdaterParameter param) {
        var referanse = param.getRef();
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var endringsaggregat = beregningTjeneste.oppdaterBeregning(dto, referanse);
        var aktivtGrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(param.getBehandlingId())
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);

        if (endringsaggregat.isPresent()) {
            fastsettBGTidsbegrensetArbeidsforholdHistorikkKalkulusTjeneste.lagHistorikk(param, endringsaggregat.get(), dto);
        } else {
            var gammeltGrunnlag = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitet(param.getBehandlingId(), BeregningsgrunnlagTilstand.FORESLÅTT_UT)
                .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
            fastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste.lagHistorikk(param, dto, aktivtGrunnlag.orElseThrow(), gammeltGrunnlag);
        }
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
