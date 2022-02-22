package no.nav.foreldrepenger.domene.rest.historikk;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
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


    public FaktaBeregningHistorikkHåndterer() {
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
     * @param oppdaterResultat            Endringsresultat
     * @param dto                         Dto for bekreftelse av aksjonspunkt for fakta om beregning
     * @param inntektArbeidYtelseGrunnlag IAY grunnlag
     */
    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             VurderFaktaOmBeregningDto dto,
                             InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        var tekstBuilder = historikkAdapter.tekstBuilder();
        håndterTilfelleHistorikk(param.getBehandlingId(), oppdaterResultat, dto.getFakta(), tekstBuilder, inntektArbeidYtelseGrunnlag);
        lagHistorikkInnslag(dto.getAksjonspunktDefinisjon(), dto.getBegrunnelse(), tekstBuilder, param.erBegrunnelseEndret());
    }

    /**
     * Lager historikk for overstyring av inntekter i fakta om beregning. Lager også historikk for tilfeller ettersom disse også blir lagret når man overstyrer.
     *
     * @param behandlingId     BehandlingId
     * @param dto              Dto for bekreftelse av overstyringsaksjonspunk for fakta om beregning (overstyring av innekter)
     * @param oppdaterResultat Endringsresultat
     * @param tekstBuilder Tekstbuilder
     */
    public void lagHistorikkOverstyringInntekt(Long behandlingId,
                                               OverstyrBeregningsgrunnlagDto dto,
                                               OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                                               HistorikkInnslagTekstBuilder tekstBuilder) {

        var endretBegrunnelse = true;
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        håndterTilfelleHistorikk(behandlingId, oppdaterResultat, dto.getFakta(), tekstBuilder, iayGrunnlag);
        faktaOmBeregningOverstyringHistorikkTjeneste.lagHistorikk(oppdaterResultat, dto, tekstBuilder, iayGrunnlag);
        lagHistorikkInnslag(dto.getAksjonspunktDefinisjon(), dto.getBegrunnelse(), tekstBuilder, endretBegrunnelse);
    }

    private void håndterTilfelleHistorikk(Long behandlingId,
                                          OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                                          FaktaBeregningLagreDto dto,
                                          HistorikkInnslagTekstBuilder tekstBuilder,
                                          InntektArbeidYtelseGrunnlag iayGrunnlag) {
        dto.getFaktaOmBeregningTilfeller()
            .stream()
            .map(kode -> FaktaOmBeregningTilfelleRef.Lookup.find(faktaOmBeregningHistorikkTjeneste, kode))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList())
            .forEach(historikkTjeneste -> historikkTjeneste.lagHistorikk(behandlingId, oppdaterResultat, dto, tekstBuilder, iayGrunnlag));
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
        var erBegrunnelseSatt = historikkDeler.stream().anyMatch(historikkDel -> historikkDel.getBegrunnelse().isPresent());
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
        var erBegrunnelseSatt = historikkDeler.stream().anyMatch(historikkDel -> historikkDel.getBegrunnelse().isPresent());
        if (!erBegrunnelseSatt) {
            if (endretBegrunnelse) {
                tekstBuilder.medBegrunnelse(begrunnelse, endretBegrunnelse);
                settSkjermlenkeOmIkkjeSatt(historikkDeler, tekstBuilder);
                tekstBuilder.ferdigstillHistorikkinnslagDel();
            }
        }
    }

    private void settSkjermlenkeOmIkkjeSatt(List<HistorikkinnslagDel> historikkDeler, HistorikkInnslagTekstBuilder tekstBuilder) {
        var erSkjermlenkeSatt = historikkDeler.stream().anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
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
