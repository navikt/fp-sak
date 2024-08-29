package no.nav.foreldrepenger.domene.rest.historikk;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFeltType;
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
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class FaktaBeregningHistorikkHåndterer {

    private HistorikkTjenesteAdapter historikkAdapter;
    private Instance<FaktaOmBeregningHistorikkTjeneste> faktaOmBeregningHistorikkTjeneste;
    private FaktaOmBeregningOverstyringHistorikkTjeneste faktaOmBeregningOverstyringHistorikkTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;


    FaktaBeregningHistorikkHåndterer() {
        // For CDI
    }

    @Inject
    public FaktaBeregningHistorikkHåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                            @Any Instance<FaktaOmBeregningHistorikkTjeneste> faktaOmBeregningHistorikkTjeneste,
                                            FaktaOmBeregningOverstyringHistorikkTjeneste faktaOmBeregningOverstyringHistorikkTjeneste,
                                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.faktaOmBeregningHistorikkTjeneste = faktaOmBeregningHistorikkTjeneste;
        this.faktaOmBeregningOverstyringHistorikkTjeneste = faktaOmBeregningOverstyringHistorikkTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }


    /**
     * Lager historikk for bekreftelse av aksjonpunkt 5058 i fakta om beregning
     *
     * @param param                       AksjonspunktOppdaterParameter for oppdatering
     * @param dto                         Dto for bekreftelse av aksjonspunkt for fakta om beregning
     * @param nyttBeregningsgrunnlag      Aktivt og oppdatert beregningsgrunnlag
     * @param forrigeGrunnlag             Forrige beregningsgrunnlag lagret på veg ut av fakta om beregning (KOFAKBER_UT)
     * @param inntektArbeidYtelseGrunnlag
     */
    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             VurderFaktaOmBeregningDto dto,
                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                             Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        var tekstBuilder = historikkAdapter.tekstBuilder();
        håndterTilfelleHistorikk(param.getBehandlingId(), dto.getFakta(), nyttBeregningsgrunnlag, forrigeGrunnlag,
            tekstBuilder, inntektArbeidYtelseGrunnlag);
        lagHistorikkInnslag(dto.getAksjonspunktDefinisjon(), dto.getBegrunnelse(), tekstBuilder, param.erBegrunnelseEndret());
    }

    /**
     * Lager historikk for overstyring av inntekter i fakta om beregning. Lager også historikk for tilfeller ettersom disse også blir lagret når man overstyrer.
     *
     * @param behandling      Aktuell behandling
     * @param dto             Dto for bekreftelse av overstyringsaksjonspunk for fakta om beregning (overstyring av innekter)
     * @param aktivtGrunnlag  Det aktive og oppdaterte beregningsgrunnlaget
     * @param forrigeGrunnlag Det forrige grunnlaget som ble lagret i fakta om beregning
     */
    public void lagHistorikkOverstyringInntekt(Behandling behandling,
                                               OverstyrBeregningsgrunnlagDto dto,
                                               BeregningsgrunnlagEntitet aktivtGrunnlag,
                                               Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {

        var tekstBuilder = historikkAdapter.tekstBuilder();
        var endretBegrunnelse = true;
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        håndterTilfelleHistorikk(behandling.getId(), dto.getFakta(), aktivtGrunnlag, forrigeGrunnlag, tekstBuilder,
            iayGrunnlag);
        faktaOmBeregningOverstyringHistorikkTjeneste.lagHistorikk(behandling.getId(), dto, tekstBuilder, aktivtGrunnlag,
            forrigeGrunnlag, iayGrunnlag);
        lagHistorikkInnslag(dto.getAksjonspunktDefinisjon(), dto.getBegrunnelse(), tekstBuilder, endretBegrunnelse);
    }

    private void håndterTilfelleHistorikk(Long behandlingId,
                                          FaktaBeregningLagreDto dto,
                                          BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                          Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                          HistorikkInnslagTekstBuilder tekstBuilder,
                                          InntektArbeidYtelseGrunnlag iayGrunnlag) {
        dto.getFaktaOmBeregningTilfeller()
            .stream()
            .map(kode -> FaktaOmBeregningTilfelleRef.Lookup.find(faktaOmBeregningHistorikkTjeneste, kode))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList()
            .forEach(historikkTjeneste -> historikkTjeneste.lagHistorikk(behandlingId, dto, tekstBuilder,
                nyttBeregningsgrunnlag, forrigeGrunnlag, iayGrunnlag));
    }

    private void lagHistorikkInnslag(AksjonspunktDefinisjon apDef,
                                     String begrunnelse,
                                     HistorikkInnslagTekstBuilder tekstBuilder,
                                     boolean endretBegrunnelse) {
        tekstBuilder.ferdigstillHistorikkinnslagDel();
        var historikkDeler = tekstBuilder.getHistorikkinnslagDeler();
        if (AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG.equals(apDef)) {
            settBegrunnelseUtenDiffsjekk(historikkDeler, tekstBuilder, begrunnelse);
        } else {
            settBegrunnelse(historikkDeler, tekstBuilder, begrunnelse, endretBegrunnelse);
        }
        settSkjermlenkeOmIkkjeSatt(historikkDeler, tekstBuilder);
    }

    private void settBegrunnelseUtenDiffsjekk(List<HistorikkinnslagDel> historikkDeler,
                                              HistorikkInnslagTekstBuilder tekstBuilder,
                                              String begrunnelse) {
        var erBegrunnelseSatt = historikkDeler.stream()
            .anyMatch(historikkDel -> historikkDel.getBegrunnelse().isPresent());
        if (!erBegrunnelseSatt) {
            tekstBuilder.medBegrunnelse(begrunnelse);
            settSkjermlenkeOmIkkjeSatt(historikkDeler, tekstBuilder);
            tekstBuilder.ferdigstillHistorikkinnslagDel();
        }
    }

    private void settBegrunnelse(List<HistorikkinnslagDel> historikkDeler,
                                 HistorikkInnslagTekstBuilder tekstBuilder,
                                 String begrunnelse,
                                 boolean endretBegrunnelse) {
        var erBegrunnelseSatt = historikkDeler.stream()
            .anyMatch(historikkDel -> historikkDel.getBegrunnelse().isPresent());
        if (!erBegrunnelseSatt && endretBegrunnelse) {
                tekstBuilder.medBegrunnelse(begrunnelse, true);
                settSkjermlenkeOmIkkjeSatt(historikkDeler, tekstBuilder);
                tekstBuilder.ferdigstillHistorikkinnslagDel();

        }
    }

    private void settSkjermlenkeOmIkkjeSatt(List<HistorikkinnslagDel> historikkDeler,
                                            HistorikkInnslagTekstBuilder tekstBuilder) {
        var erSkjermlenkeSatt = historikkDeler.stream()
            .anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
        if (!erSkjermlenkeSatt && !historikkDeler.isEmpty()) {
            var builder = HistorikkinnslagDel.builder(tekstBuilder.getHistorikkinnslagDeler().get(0));
            HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.SKJERMLENKE)
                .medTilVerdi(SkjermlenkeType.FAKTA_OM_BEREGNING)
                .build(builder);
            builder.build();
        }
    }


}
