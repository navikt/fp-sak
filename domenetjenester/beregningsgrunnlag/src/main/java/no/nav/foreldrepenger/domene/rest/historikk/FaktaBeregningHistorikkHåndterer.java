package no.nav.foreldrepenger.domene.rest.historikk;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.rest.historikk.overstyring.FaktaOmBeregningOverstyringHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.tilfeller.FaktaOmBeregningHistorikkTjeneste;

@ApplicationScoped
public class FaktaBeregningHistorikkHåndterer {

    private Instance<FaktaOmBeregningHistorikkTjeneste> faktaOmBeregningHistorikkTjeneste;
    private FaktaOmBeregningOverstyringHistorikkTjeneste faktaOmBeregningOverstyringHistorikkTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;


    FaktaBeregningHistorikkHåndterer() {
        // For CDI
    }

    @Inject
    public FaktaBeregningHistorikkHåndterer(@Any Instance<FaktaOmBeregningHistorikkTjeneste> faktaOmBeregningHistorikkTjeneste,
                                            FaktaOmBeregningOverstyringHistorikkTjeneste faktaOmBeregningOverstyringHistorikkTjeneste,
                                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                            HistorikkinnslagRepository historikkinnslagRepository) {
        this.faktaOmBeregningHistorikkTjeneste = faktaOmBeregningHistorikkTjeneste;
        this.faktaOmBeregningOverstyringHistorikkTjeneste = faktaOmBeregningOverstyringHistorikkTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }


    /**
     * Lager historikk for bekreftelse av aksjonpunkt 5058 i fakta om beregning
     *
     * @param param                       AksjonspunktOppdaterParameter for oppdatering
     * @param dto                         Dto for bekreftelse av aksjonspunkt for fakta om beregning
     * @param nyttBeregningsgrunnlag      Aktivt og oppdatert beregningsgrunnlag
     * @param forrigeGrunnlag             Forrige beregningsgrunnlag lagret på veg ut av fakta om beregning (KOFAKBER_UT)
     */
    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             VurderFaktaOmBeregningDto dto,
                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                             Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        var linjeBuilder = håndterTilfelleHistorikk(dto.getFakta(), nyttBeregningsgrunnlag, forrigeGrunnlag,
            inntektArbeidYtelseGrunnlag);
        lagHistorikkInnslag(param.getRef(),linjeBuilder, dto.getBegrunnelse());
    }

    /**
     * Lager historikk for overstyring av inntekter i fakta om beregning. Lager også historikk for tilfeller ettersom disse også blir lagret når man overstyrer.
     *
     * @param behandling      Aktuell behandling
     * @param dto             Dto for bekreftelse av overstyringsaksjonspunk for fakta om beregning (overstyring av innekter)
     * @param aktivtGrunnlag  Det aktive og oppdaterte beregningsgrunnlaget
     * @param forrigeGrunnlag Det forrige grunnlaget som ble lagret i fakta om beregning
     */
    public void lagHistorikkOverstyringInntekt(BehandlingReferanse ref,
                                               OverstyrBeregningsgrunnlagDto dto,
                                               BeregningsgrunnlagEntitet aktivtGrunnlag,
                                               Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {

        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.behandlingId());
        var linjeBuilder = håndterTilfelleHistorikk(dto.getFakta(), aktivtGrunnlag, forrigeGrunnlag, iayGrunnlag);
        linjeBuilder.addAll(faktaOmBeregningOverstyringHistorikkTjeneste.lagHistorikk(dto, aktivtGrunnlag, forrigeGrunnlag, iayGrunnlag));
        lagHistorikkInnslag(ref, linjeBuilder, dto.getBegrunnelse());
    }

    private List<HistorikkinnslagLinjeBuilder> håndterTilfelleHistorikk(FaktaBeregningLagreDto dto,
                                                                        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                                        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                                        InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        dto.getFaktaOmBeregningTilfeller()
            .stream()
            .map(kode -> FaktaOmBeregningTilfelleRef.Lookup.find(faktaOmBeregningHistorikkTjeneste, kode))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList()
            .forEach(historikkTjeneste -> linjeBuilder.addAll(
                historikkTjeneste.lagHistorikk(dto, nyttBeregningsgrunnlag, forrigeGrunnlag, iayGrunnlag)));

        return linjeBuilder;
    }

    private void lagHistorikkInnslag(BehandlingReferanse behandlingRef,
                                     List<HistorikkinnslagLinjeBuilder> linjeBuilder,
                                     String begrunnelse) {
        linjeBuilder.add(new HistorikkinnslagLinjeBuilder().tekst(begrunnelse));
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(behandlingRef.behandlingId())
            .medFagsakId(behandlingRef.fagsakId())
            .medTittel(SkjermlenkeType.FAKTA_OM_BEREGNING)
            .medLinjer(linjeBuilder)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
