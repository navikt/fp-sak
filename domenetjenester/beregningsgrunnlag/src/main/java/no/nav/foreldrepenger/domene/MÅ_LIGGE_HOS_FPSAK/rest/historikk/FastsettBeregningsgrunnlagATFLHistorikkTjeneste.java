package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.InntektPrAndelDto;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

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
                                                           ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param, FastsettBeregningsgrunnlagATFLDto dto,
                             BeregningsgrunnlagEntitet forrigeGrunnlag) {
        BeregningsgrunnlagPeriode førstePeriode = forrigeGrunnlag.getBeregningsgrunnlagPerioder().get(0);

        List<BeregningsgrunnlagPrStatusOgAndel> atAndeler = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
            .collect(Collectors.toList());

        List<BeregningsgrunnlagPrStatusOgAndel> flAndeler = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))
            .collect(Collectors.toList());

        lagHistorikkInnslag(dto, param, atAndeler, flAndeler);
    }


    private void lagHistorikkInnslag(FastsettBeregningsgrunnlagATFLDto dto,
                                     AksjonspunktOppdaterParameter param,
                                     List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerList,
                                     List<BeregningsgrunnlagPrStatusOgAndel> frilanserList) {

        oppdaterVedEndretVerdi(param.getBehandlingId(), dto.getInntektPrAndelList(), arbeidstakerList, frilanserList, dto.getInntektFrilanser());

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.BEREGNING_FORELDREPENGER);
    }

    private void oppdaterVedEndretVerdi(Long behandlingId, List<InntektPrAndelDto> overstyrtList, List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerList, List<BeregningsgrunnlagPrStatusOgAndel> frilanserList, Integer inntektFrilanser) {
        if (arbeidstakerList.stream().noneMatch(bgpsa -> bgpsa.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))) {
            historikkAdapter.tekstBuilder().medResultat(HistorikkResultatType.BEREGNET_AARSINNTEKT);
        }

        if (inntektFrilanser != null && !frilanserList.isEmpty()) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.FRILANS_INNTEKT, null, inntektFrilanser);
        }

        if (overstyrtList != null) {
            oppdaterForOverstyrt(behandlingId, overstyrtList, arbeidstakerList);
        }

    }

    private void oppdaterForOverstyrt(Long behandlingId, List<InntektPrAndelDto> overstyrtList, List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerList) {
        List<ArbeidsforholdOverstyring> arbeidsforholOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        for (BeregningsgrunnlagPrStatusOgAndel prStatus : arbeidstakerList) {
            Optional<InntektPrAndelDto> overstyrt = overstyrtList.stream().filter(andelInntekt -> andelInntekt.getAndelsnr().equals(prStatus.getAndelsnr())).findFirst();
            if (overstyrt.isPresent()) {
                String visningsNavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(prStatus.getAktivitetStatus(), prStatus.getArbeidsgiver(), prStatus.getArbeidsforholdRef(), arbeidsforholOverstyringer);
                historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD, visningsNavn, null, overstyrt.get().getInntekt());
            }
        }
    }

}
