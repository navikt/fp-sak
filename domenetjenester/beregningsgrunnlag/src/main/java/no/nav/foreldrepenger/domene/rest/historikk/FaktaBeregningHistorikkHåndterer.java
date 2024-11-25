package no.nav.foreldrepenger.domene.rest.historikk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
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
    private Historikkinnslag2Repository historikkinnslagRepository;


    FaktaBeregningHistorikkHåndterer() {
        // For CDI
    }

    @Inject
    public FaktaBeregningHistorikkHåndterer(@Any Instance<FaktaOmBeregningHistorikkTjeneste> faktaOmBeregningHistorikkTjeneste,
                                            FaktaOmBeregningOverstyringHistorikkTjeneste faktaOmBeregningOverstyringHistorikkTjeneste,
                                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                            Historikkinnslag2Repository historikkinnslagRepository) {
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
        var tekstlinjerBuilder = håndterTilfelleHistorikk(dto.getFakta(), nyttBeregningsgrunnlag, forrigeGrunnlag,
            inntektArbeidYtelseGrunnlag);
        lagHistorikkInnslag(param.getRef(),tekstlinjerBuilder, dto.getBegrunnelse());
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

        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        var tekstlinjerBuilder = håndterTilfelleHistorikk(dto.getFakta(), aktivtGrunnlag, forrigeGrunnlag, iayGrunnlag);
        tekstlinjerBuilder.addAll(faktaOmBeregningOverstyringHistorikkTjeneste.lagHistorikk(dto, aktivtGrunnlag, forrigeGrunnlag, iayGrunnlag));
        lagHistorikkInnslag(BehandlingReferanse.fra(behandling), tekstlinjerBuilder, dto.getBegrunnelse());
    }

    private List<HistorikkinnslagTekstlinjeBuilder> håndterTilfelleHistorikk(FaktaBeregningLagreDto dto,
                                                                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                                             Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        dto.getFaktaOmBeregningTilfeller()
            .stream()
            .map(kode -> FaktaOmBeregningTilfelleRef.Lookup.find(faktaOmBeregningHistorikkTjeneste, kode))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList()
            .forEach(historikkTjeneste -> tekstlinjerBuilder.addAll(
                historikkTjeneste.lagHistorikk(dto, nyttBeregningsgrunnlag, forrigeGrunnlag, iayGrunnlag)));

        return tekstlinjerBuilder;
    }

    private void lagHistorikkInnslag(BehandlingReferanse behandlingRef,
                                     List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder,
                                     String begrunnelse) {
        tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().tekst(begrunnelse));
        if (!tekstlinjerBuilder.isEmpty()) {
            var historikkinnslag = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medBehandlingId(behandlingRef.behandlingId())
                .medFagsakId(behandlingRef.fagsakId())
                .medTittel(SkjermlenkeType.FAKTA_OM_BEREGNING)
                .medTekstlinjer(tekstlinjerBuilder)
                .build();
            historikkinnslagRepository.lagre(historikkinnslag);
        }
    }
}
