package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.overstyring.FaktaOmBeregningOverstyringHistorikkTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.tilfeller.FaktaOmBeregningHistorikkTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
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
     *  Lager historikk for bekreftelse av aksjonpunkt 5058 i fakta om beregning
     *  @param param AksjonspunktOppdaterParameter for oppdatering
     * @param dto Dto for bekreftelse av aksjonspunkt for fakta om beregning
     * @param nyttBeregningsgrunnlag Aktivt og oppdatert beregningsgrunnlag
     * @param forrigeGrunnlag Forrige beregningsgrunnlag lagret på veg ut av fakta om beregning (KOFAKBER_UT)
     * @param inntektArbeidYtelseGrunnlag
     */
    public void lagHistorikk(AksjonspunktOppdaterParameter param, VurderFaktaOmBeregningDto dto,
                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder();
        håndterTilfelleHistorikk(param.getBehandlingId(), dto.getFakta(), nyttBeregningsgrunnlag, forrigeGrunnlag, tekstBuilder, inntektArbeidYtelseGrunnlag);
        lagHistorikkInnslag(dto.getKode(), dto.getBegrunnelse(), tekstBuilder, param.erBegrunnelseEndret());
    }

    /**
     *  Lager historikk for overstyring av inntekter i fakta om beregning. Lager også historikk for tilfeller ettersom disse også blir lagret når man overstyrer.
     *
     * @param behandling Aktuell behandling
     * @param dto Dto for bekreftelse av overstyringsaksjonspunk for fakta om beregning (overstyring av innekter)
     * @param aktivtGrunnlag Det aktive og oppdaterte beregningsgrunnlaget
     * @param forrigeGrunnlag Det forrige grunnlaget som ble lagret i fakta om beregning
     */
    public void lagHistorikkOverstyringInntekt(Behandling behandling, OverstyrBeregningsgrunnlagDto dto,
                                               BeregningsgrunnlagEntitet aktivtGrunnlag,
                                               Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {

        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder();
        boolean endretBegrunnelse = true;
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        håndterTilfelleHistorikk(behandling.getId(), dto.getFakta(), aktivtGrunnlag, forrigeGrunnlag, tekstBuilder, iayGrunnlag);
        faktaOmBeregningOverstyringHistorikkTjeneste.lagHistorikk(behandling.getId(), dto, tekstBuilder, aktivtGrunnlag, forrigeGrunnlag, iayGrunnlag);
        lagHistorikkInnslag(dto.getKode(), dto.getBegrunnelse(), tekstBuilder, endretBegrunnelse);
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
            .collect(Collectors.toList())
            .forEach(historikkTjeneste -> historikkTjeneste.lagHistorikk(behandlingId, dto, tekstBuilder, nyttBeregningsgrunnlag, forrigeGrunnlag, iayGrunnlag));
    }

    private void lagHistorikkInnslag(String kode, String begrunnelse, HistorikkInnslagTekstBuilder tekstBuilder, boolean endretBegrunnelse) {
        tekstBuilder.ferdigstillHistorikkinnslagDel();
        List<HistorikkinnslagDel> historikkDeler = tekstBuilder.getHistorikkinnslagDeler();
        if (AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE.equals(kode)) {
            settBegrunnelseUtenDiffsjekk(historikkDeler, tekstBuilder, begrunnelse);
        } else {
            settBegrunnelse(historikkDeler, tekstBuilder, begrunnelse, endretBegrunnelse);
        }
        settSkjermlenkeOmIkkjeSatt(historikkDeler, tekstBuilder);
    }

    private void settBegrunnelseUtenDiffsjekk(List<HistorikkinnslagDel> historikkDeler,
                                              HistorikkInnslagTekstBuilder tekstBuilder,
                                              String begrunnelse) {
        boolean erBegrunnelseSatt = historikkDeler.stream()
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
        boolean erBegrunnelseSatt = historikkDeler.stream()
            .anyMatch(historikkDel -> historikkDel.getBegrunnelse().isPresent());
        if (!erBegrunnelseSatt) {
            if (endretBegrunnelse) {
                tekstBuilder.medBegrunnelse(begrunnelse, endretBegrunnelse);
                settSkjermlenkeOmIkkjeSatt(historikkDeler, tekstBuilder);
                tekstBuilder.ferdigstillHistorikkinnslagDel();
            }
        }
    }

    private void settSkjermlenkeOmIkkjeSatt(List<HistorikkinnslagDel> historikkDeler, HistorikkInnslagTekstBuilder tekstBuilder) {
        boolean erSkjermlenkeSatt = historikkDeler.stream()
            .anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
        if (!erSkjermlenkeSatt && !historikkDeler.isEmpty()) {
            HistorikkinnslagDel.Builder builder = HistorikkinnslagDel.builder(tekstBuilder.getHistorikkinnslagDeler().get(0));
            HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.SKJERMLENKE)
                .medTilVerdi(SkjermlenkeType.FAKTA_OM_BEREGNING)
                .build(builder);
            builder.build();
        }
    }


}
