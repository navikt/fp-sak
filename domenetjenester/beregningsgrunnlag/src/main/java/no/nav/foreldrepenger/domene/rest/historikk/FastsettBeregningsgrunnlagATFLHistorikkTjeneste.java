package no.nav.foreldrepenger.domene.rest.historikk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.InntektPrAndelDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

import java.util.List;

@ApplicationScoped
public class FastsettBeregningsgrunnlagATFLHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    FastsettBeregningsgrunnlagATFLHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLHistorikkTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                                           ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                           InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             FastsettBeregningsgrunnlagATFLDto dto,
                             BeregningsgrunnlagEntitet forrigeGrunnlag) {
        var førstePeriode = forrigeGrunnlag.getBeregningsgrunnlagPerioder().get(0);

        var atAndeler = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
            .toList();

        var flAndeler = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))
            .toList();

        lagHistorikkInnslag(dto, param, atAndeler, flAndeler);
    }


    private void lagHistorikkInnslag(FastsettBeregningsgrunnlagATFLDto dto,
                                     AksjonspunktOppdaterParameter param,
                                     List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerList,
                                     List<BeregningsgrunnlagPrStatusOgAndel> frilanserList) {

        oppdaterVedEndretVerdi(param.getBehandlingId(), dto.getInntektPrAndelList(), arbeidstakerList, frilanserList,
            dto.getInntektFrilanser());

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.BEREGNING_FORELDREPENGER);
    }

    private void oppdaterVedEndretVerdi(Long behandlingId,
                                        List<InntektPrAndelDto> overstyrtList,
                                        List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerList,
                                        List<BeregningsgrunnlagPrStatusOgAndel> frilanserList,
                                        Integer inntektFrilanser) {
        if (arbeidstakerList.stream()
            .noneMatch(bgpsa -> bgpsa.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))) {
            historikkAdapter.tekstBuilder().medResultat(HistorikkResultatType.BEREGNET_AARSINNTEKT);
        }

        if (inntektFrilanser != null && !frilanserList.isEmpty()) {
            historikkAdapter.tekstBuilder()
                .medEndretFelt(HistorikkEndretFeltType.FRILANS_INNTEKT, null, inntektFrilanser);
        }

        if (overstyrtList != null) {
            oppdaterForOverstyrt(behandlingId, overstyrtList, arbeidstakerList);
        }

    }

    private void oppdaterForOverstyrt(Long behandlingId,
                                      List<InntektPrAndelDto> overstyrtList,
                                      List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerList) {
        var arbeidsforholOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
            .getArbeidsforholdOverstyringer();
        for (var prStatus : arbeidstakerList) {
            var overstyrt = overstyrtList.stream()
                .filter(andelInntekt -> andelInntekt.getAndelsnr().equals(prStatus.getAndelsnr()))
                .findFirst();
            if (overstyrt.isPresent()) {
                var visningsNavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
                    prStatus.getAktivitetStatus(), prStatus.getArbeidsgiver(), prStatus.getArbeidsforholdRef(),
                    arbeidsforholOverstyringer);
                historikkAdapter.tekstBuilder()
                    .medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD, visningsNavn, null,
                        overstyrt.get().getInntekt());
            }
        }
    }

}
