package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.mappers.endringutleder.UtledEndring;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus_rest.FraKalkulusMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.prosess.KalkulusTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsaktiviteterDto;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.BeregningsaktivitetHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.FaktaBeregningHistorikkHåndterer;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class BeregningKalkulus implements BeregningAPI {

    private BehandlingRepository behandlingRepository;
    private Instance<SkjæringstidspunktTjeneste> skjæringstidspunktTjeneste;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private KalkulusTjeneste kalkulusTjeneste;
    private BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste;
    private FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public BeregningKalkulus() {
    }

    @Inject
    public BeregningKalkulus(BehandlingRepository behandlingRepository,
                             @Any Instance<SkjæringstidspunktTjeneste> skjæringstidspunktTjeneste,
                             BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                             KalkulusTjeneste kalkulusTjeneste,
                             BeregningsaktivitetHistorikkTjeneste historikkTjeneste,
                             FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer,
                             InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsaktivitetHistorikkTjeneste = historikkTjeneste;
        this.faktaBeregningHistorikkHåndterer = faktaBeregningHistorikkHåndterer;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }


    /**
     * Kjører beregning for angitt steg
     *
     * @param behandlingId       behandlingId
     * @param behandlingStegType Stegtype
     * @return Resultatstruktur med aksjonspunkter og eventuell vilkårsvurdering
     */
    @Override
    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(Long behandlingId, BehandlingStegType behandlingStegType) {
        var behandlingReferanse = lagReferanseMedSkjæringstidspunkt(behandlingId);
        return kalkulusTjeneste.beregn(behandlingReferanse, behandlingStegType);
    }

    @Override
    public void overstyr(BehandlingReferanse behandlingReferanse, OverstyringAksjonspunktDto overstyringAksjonspunktDto) {
        kalkulusTjeneste.overstyr(behandlingReferanse, overstyringAksjonspunktDto);
    }

    @Override
    // TODO Send med endringsresultat fra kallet til kalkulus i staden for å utlede
    public void lagOverstyringHistorikk(BehandlingReferanse behandlingReferanse,
                                        OverstyringAksjonspunktDto overstyringAksjonspunktDto,
                                        HistorikkInnslagTekstBuilder tekstBuilder) {
        var grunnlag = hent(behandlingReferanse.getBehandlingId());
        var endringsresultat = grunnlag.map(gr -> UtledEndring.utled(gr, gr, Optional.empty(), overstyringAksjonspunktDto,
            inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())));
        if (overstyringAksjonspunktDto instanceof OverstyrBeregningsaktiviteterDto) {
            endringsresultat.ifPresent(e -> beregningsaktivitetHistorikkTjeneste.lagHistorikk(behandlingReferanse.getBehandlingId(), tekstBuilder,
                e.getBeregningAktiviteterEndring(), overstyringAksjonspunktDto.getBegrunnelse()));
        } else if (overstyringAksjonspunktDto instanceof OverstyrBeregningsgrunnlagDto dto) {
            endringsresultat.ifPresent(
                e -> faktaBeregningHistorikkHåndterer.lagHistorikkOverstyringInntekt(behandlingReferanse.getBehandlingId(), dto, e, tekstBuilder));
        }
    }

    @Override
    public OppdaterBeregningsgrunnlagResultat oppdater(AksjonspunktOppdaterParameter parameter, BekreftetAksjonspunktDto bekreftAksjonspunktDto) {
        var behandlingReferanse = lagReferanseMedSkjæringstidspunkt(parameter.getBehandlingId());
        return kalkulusTjeneste.oppdater(behandlingReferanse, bekreftAksjonspunktDto);
    }

    /**
     * Henter beregningsgrunnlag
     *
     * @param behandlingId behandlingId
     * @return BeregningsgrunnlagGrunnlag
     */
    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(Long behandlingId) {
        var behandlingReferanse = lagReferanseMedSkjæringstidspunkt(behandlingId);
        return kalkulusTjeneste.hentGrunnlag(behandlingReferanse).map(FraKalkulusMapper::mapBeregningsgrunnlagGrunnlag);
    }

    @Override
    public Optional<BeregningsgrunnlagDto> hentForGUI(Long behandlingId) {
        var behandlingReferanse = lagReferanseMedSkjæringstidspunkt(behandlingId);
        return kalkulusTjeneste.hentDtoForVisning(behandlingReferanse);

    }

    /**
     * Kopierer beregningsgrunnlag
     *
     * @param behandlingId behandlingId
     */
    @Override
    public void kopierFastsatt(Long behandlingId) {
        kalkulusTjeneste.kopier(behandlingId, StegType.FAST_BERGRUNN);
    }

    /**
     * Rydder beregningsgrunnlag og tilhørende resultat for et hopp bakover kall i oppgitt steg
     *
     * @param kontekst           Behandlingskontrollkontekst
     * @param behandlingStegType steget ryddkallet kjøres fra
     * @param tilSteg            Siste steg i hopp bakover transisjonen
     */
    @Override
    public void rydd(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, BehandlingStegType tilSteg) {
        ryddMedKalkulus(kontekst, behandlingStegType, tilSteg);
    }

    private void ryddMedKalkulus(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, BehandlingStegType tilSteg) {
        if (!behandlingStegType.equals(tilSteg)) {
            switch (behandlingStegType) {
                case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING:
                    kalkulusTjeneste.deaktiver(kontekst.getBehandlingId());
                case VURDER_VILKAR_BERGRUNN:
                    beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst);
            }
        }
    }

    private BehandlingReferanse lagReferanseMedSkjæringstidspunkt(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkt = getSkjæringstidspunkt(behandlingId, behandling.getFagsakYtelseType());
        return BehandlingReferanse.fra(behandling).medSkjæringstidspunkt(skjæringstidspunkt);
    }

    private Skjæringstidspunkt getSkjæringstidspunkt(Long behandlingId, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(skjæringstidspunktTjeneste, fagsakYtelseType)
            .map(t -> t.getSkjæringstidspunkter(behandlingId))
            .orElse(null);
    }


}
