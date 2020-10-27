package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.fra_kalkulus;

import java.util.List;

import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BGAndelArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagAktivitetStatusDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.SammenligningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.SammenligningsgrunnlagPrStatusDto;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AndelKilde;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Hjemmel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Inntektskategori;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.SammenligningsgrunnlagPrStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.SammenligningsgrunnlagType;


public class KalkulusTilBGMapper {
    public static Sammenligningsgrunnlag mapSammenligningsgrunnlag(SammenligningsgrunnlagDto fraKalkulus) {
        Sammenligningsgrunnlag.Builder builder = Sammenligningsgrunnlag.builder();
        builder.medAvvikPromille(fraKalkulus.getAvvikPromilleNy());
        builder.medRapportertPrÅr(fraKalkulus.getRapportertPrÅr());
        builder.medSammenligningsperiode(fraKalkulus.getSammenligningsperiodeFom(), fraKalkulus.getSammenligningsperiodeFom());
        return builder.build();
    }

    public static BeregningsgrunnlagAktivitetStatus.Builder mapAktivitetStatus(BeregningsgrunnlagAktivitetStatusDto fraKalkulus) {
        BeregningsgrunnlagAktivitetStatus.Builder builder = new BeregningsgrunnlagAktivitetStatus.Builder();
        builder.medAktivitetStatus(AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode()));
        builder.medHjemmel(Hjemmel.fraKode(fraKalkulus.getHjemmel().getKode()));

        return builder;
    }

    public static BeregningsgrunnlagPeriode.Builder mapBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriodeDto fraKalkulus) {
        BeregningsgrunnlagPeriode.Builder builder = new BeregningsgrunnlagPeriode.Builder();

        //med
        builder.medAvkortetPrÅr(fraKalkulus.getAvkortetPrÅr());
        builder.medBeregningsgrunnlagPeriode(fraKalkulus.getBeregningsgrunnlagPeriodeFom(), fraKalkulus.getBeregningsgrunnlagPeriodeTom());
        builder.medBruttoPrÅr(fraKalkulus.getBruttoPrÅr());
        builder.medRedusertPrÅr(fraKalkulus.getRedusertPrÅr());
        builder.medRegelEvalueringFastsett(fraKalkulus.getRegelInputFastsett(), fraKalkulus.getRegelEvalueringFastsett());
        builder.medRegelEvalueringFinnGrenseverdi(fraKalkulus.getRegelInputFinnGrenseverdi(), fraKalkulus.getRegelEvalueringFinnGrenseverdi());
        builder.medRegelEvalueringFordel(fraKalkulus.getRegelInputFordel(), fraKalkulus.getRegelEvalueringFordel());
        builder.medRegelEvalueringForeslå(fraKalkulus.getRegelInput(), fraKalkulus.getRegelEvaluering());
        builder.medRegelEvalueringVilkårsvurdering(fraKalkulus.getRegelInputVilkårvurdering(), fraKalkulus.getRegelEvalueringVilkårvurdering());

        //legg til
        fraKalkulus.getPeriodeÅrsaker().forEach(periodeÅrsak -> builder.leggTilPeriodeÅrsak(PeriodeÅrsak.fraKode(periodeÅrsak.getKode())));
        fraKalkulus.getBeregningsgrunnlagPrStatusOgAndelList().forEach( statusOgAndel -> builder.leggTilBeregningsgrunnlagPrStatusOgAndel(mapStatusOgAndel(statusOgAndel)));

        return builder;
    }

    public static SammenligningsgrunnlagPrStatus.Builder mapSammenligningsgrunnlagMedStatus(SammenligningsgrunnlagPrStatusDto fraKalkulus) {
        SammenligningsgrunnlagPrStatus.Builder builder = new SammenligningsgrunnlagPrStatus.Builder();
        builder.medAvvikPromille(fraKalkulus.getAvvikPromilleNy());
        builder.medRapportertPrÅr(fraKalkulus.getRapportertPrÅr());
        builder.medSammenligningsgrunnlagType(SammenligningsgrunnlagType.fraKode(fraKalkulus.getSammenligningsgrunnlagType().getKode()));
        builder.medSammenligningsperiode(fraKalkulus.getSammenligningsperiodeFom(), fraKalkulus.getSammenligningsperiodeTom());

        return builder;
    }

    private static BeregningsgrunnlagPrStatusOgAndel.Builder mapStatusOgAndel(BeregningsgrunnlagPrStatusOgAndelDto fraKalkulus) {
        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode()))
            .medAndelsnr(fraKalkulus.getAndelsnr())
            .medArbforholdType(fraKalkulus.getArbeidsforholdType() == null ? null : OpptjeningAktivitetType.fraKode(fraKalkulus.getArbeidsforholdType().getKode()))
            .medAvkortetBrukersAndelPrÅr(fraKalkulus.getAvkortetBrukersAndelPrÅr())
            .medAvkortetPrÅr(fraKalkulus.getAvkortetPrÅr())
            .medAvkortetRefusjonPrÅr(fraKalkulus.getAvkortetRefusjonPrÅr())
            .medBeregnetPrÅr(fraKalkulus.getBeregnetPrÅr())
            .medBesteberegningPrÅr(fraKalkulus.getBesteberegningPrÅr())
            .medFastsattAvSaksbehandler(fraKalkulus.getFastsattAvSaksbehandler())
            .medOverstyrtPrÅr(fraKalkulus.getOverstyrtPrÅr())
            .medFordeltPrÅr(fraKalkulus.getFordeltPrÅr())
            .medRedusertPrÅr(fraKalkulus.getRedusertPrÅr())
            .medRedusertBrukersAndelPrÅr(fraKalkulus.getRedusertBrukersAndelPrÅr())
            .medMaksimalRefusjonPrÅr(fraKalkulus.getMaksimalRefusjonPrÅr())
            .medRedusertRefusjonPrÅr(fraKalkulus.getRedusertRefusjonPrÅr())
            .medÅrsbeløpFraTilstøtendeYtelse(fraKalkulus.getÅrsbeløpFraTilstøtendeYtelse() == null ? null : fraKalkulus.getÅrsbeløpFraTilstøtendeYtelse().getVerdi())
            .medNyIArbeidslivet(fraKalkulus.getNyIArbeidslivet())
            .medInntektskategori(fraKalkulus.getInntektskategori() == null ? null : Inntektskategori.fraKode(fraKalkulus.getInntektskategori().getKode()))
            .medKilde(AndelKilde.fraKode(fraKalkulus.getKilde().getKode()))
            .medLagtTilAvSaksbehandler(fraKalkulus.getLagtTilAvSaksbehandler())
            .medOrginalDagsatsFraTilstøtendeYtelse(fraKalkulus.getOrginalDagsatsFraTilstøtendeYtelse());

        if (fraKalkulus.getBeregningsperiodeFom() != null) {
            builder.medBeregningsperiode(fraKalkulus.getBeregningsperiodeFom(), fraKalkulus.getBeregningsperiodeTom());
        }

        if (fraKalkulus.getPgiSnitt() != null) {
            builder.medPgi(fraKalkulus.getPgiSnitt(), List.of(fraKalkulus.getPgi1(), fraKalkulus.getPgi2(), fraKalkulus.getPgi3()));
        }

        fraKalkulus.getBgAndelArbeidsforhold().ifPresent(bgAndelArbeidsforhold -> builder.medBGAndelArbeidsforhold(KalkulusTilBGMapper.mapBGAndelArbeidsforhold(bgAndelArbeidsforhold)));
        fraKalkulus.erNyoppstartet().ifPresent(aBoolean -> builder.medNyoppstartet(aBoolean, AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode())));
        fraKalkulus.mottarYtelse().ifPresent(aBoolean -> builder.medMottarYtelse(aBoolean, AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode())));
        return builder;
    }

    private static BGAndelArbeidsforhold.Builder mapBGAndelArbeidsforhold(BGAndelArbeidsforholdDto fraKalkulus) {
        BGAndelArbeidsforhold.Builder builder = BGAndelArbeidsforhold.builder();
        builder.medArbeidsforholdRef(KalkulusTilIAYMapper.mapArbeidsforholdRed(fraKalkulus.getArbeidsforholdRef()));
        builder.medArbeidsgiver(KalkulusTilIAYMapper.mapArbeidsgiver(fraKalkulus.getArbeidsgiver()));
        builder.medArbeidsperiodeFom(fraKalkulus.getArbeidsperiodeFom());
        builder.medLønnsendringIBeregningsperioden(fraKalkulus.erLønnsendringIBeregningsperioden());
        builder.medTidsbegrensetArbeidsforhold(fraKalkulus.getErTidsbegrensetArbeidsforhold());
        builder.medRefusjonskravPrÅr(fraKalkulus.getRefusjonskravPrÅr());
        builder.medSaksbehandletRefusjonPrÅr(fraKalkulus.getSaksbehandletRefusjonPrÅr());
        builder.medFordeltRefusjonPrÅr(fraKalkulus.getFordeltRefusjonPrÅr());
        fraKalkulus.getArbeidsperiodeTom().ifPresent(builder::medArbeidsperiodeTom);
        fraKalkulus.getNaturalytelseBortfaltPrÅr().ifPresent(builder::medNaturalytelseBortfaltPrÅr);
        fraKalkulus.getNaturalytelseTilkommetPrÅr().ifPresent(builder::medNaturalytelseTilkommetPrÅr);
        return builder;
    }
}
