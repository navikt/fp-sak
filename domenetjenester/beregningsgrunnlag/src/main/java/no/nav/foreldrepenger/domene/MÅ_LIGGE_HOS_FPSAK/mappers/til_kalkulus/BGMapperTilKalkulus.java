package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus;

import java.util.List;

import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BGAndelArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagAktivitetStatusDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.SammenligningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.SammenligningsgrunnlagPrStatusDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.AndelKilde;
import no.nav.folketrygdloven.kalkulus.kodeverk.Hjemmel;
import no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.PeriodeÅrsak;
import no.nav.folketrygdloven.kalkulus.kodeverk.SammenligningsgrunnlagType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.SammenligningsgrunnlagPrStatus;


public class BGMapperTilKalkulus {
    public static SammenligningsgrunnlagDto mapSammenligningsgrunnlag(Sammenligningsgrunnlag fraFpsak) {
        SammenligningsgrunnlagDto.Builder builder = SammenligningsgrunnlagDto.builder();
        builder.medAvvikPromilleNy(fraFpsak.getAvvikPromille());
        builder.medRapportertPrÅr(fraFpsak.getRapportertPrÅr());
        builder.medSammenligningsperiode(fraFpsak.getSammenligningsperiodeFom(), fraFpsak.getSammenligningsperiodeFom());
        return builder.build();
    }

    public static BeregningsgrunnlagAktivitetStatusDto.Builder mapAktivitetStatus(BeregningsgrunnlagAktivitetStatus fraFpsak) {
        BeregningsgrunnlagAktivitetStatusDto.Builder builder = new BeregningsgrunnlagAktivitetStatusDto.Builder();
        builder.medAktivitetStatus(AktivitetStatus.fraKode(fraFpsak.getAktivitetStatus().getKode()));
        builder.medHjemmel(Hjemmel.fraKode(fraFpsak.getHjemmel().getKode()));

        return builder;
    }

    public static BeregningsgrunnlagPeriodeDto.Builder mapBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode fraFpsak) {
        BeregningsgrunnlagPeriodeDto.Builder builder = new BeregningsgrunnlagPeriodeDto.Builder();

        //med
        builder.medAvkortetPrÅr(fraFpsak.getAvkortetPrÅr());
        builder.medBeregningsgrunnlagPeriode(fraFpsak.getBeregningsgrunnlagPeriodeFom(), fraFpsak.getBeregningsgrunnlagPeriodeTom());
        builder.medBruttoPrÅr(fraFpsak.getBruttoPrÅr());
        builder.medRedusertPrÅr(fraFpsak.getRedusertPrÅr());

        //legg til
        fraFpsak.getPeriodeÅrsaker().forEach(periodeÅrsak -> builder.leggTilPeriodeÅrsak(PeriodeÅrsak.fraKode(periodeÅrsak.getKode())));
        fraFpsak.getBeregningsgrunnlagPrStatusOgAndelList().forEach( statusOgAndel -> builder.leggTilBeregningsgrunnlagPrStatusOgAndel(mapStatusOgAndel(statusOgAndel)));

        return builder;
    }

    public static SammenligningsgrunnlagPrStatusDto.Builder mapSammenligningsgrunnlagMedStatus(SammenligningsgrunnlagPrStatus fraFpsak) {
        SammenligningsgrunnlagPrStatusDto.Builder builder = new SammenligningsgrunnlagPrStatusDto.Builder();
        builder.medAvvikPromilleNy(fraFpsak.getAvvikPromille());
        builder.medRapportertPrÅr(fraFpsak.getRapportertPrÅr());
        builder.medSammenligningsgrunnlagType(SammenligningsgrunnlagType.fraKode(fraFpsak.getSammenligningsgrunnlagType().getKode()));
        builder.medSammenligningsperiode(fraFpsak.getSammenligningsperiodeFom(), fraFpsak.getSammenligningsperiodeTom());

        return builder;
    }

    private static BeregningsgrunnlagPrStatusOgAndelDto.Builder mapStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel fraFpsak) {
        BeregningsgrunnlagPrStatusOgAndelDto.Builder builder = BeregningsgrunnlagPrStatusOgAndelDto.ny()
            .medAktivitetStatus(AktivitetStatus.fraKode(fraFpsak.getAktivitetStatus().getKode()))
            .medAndelsnr(fraFpsak.getAndelsnr())
            .medArbforholdType(fraFpsak.getArbeidsforholdType() == null ? null : OpptjeningAktivitetType.fraKode(fraFpsak.getArbeidsforholdType().getKode()))
            .medAvkortetBrukersAndelPrÅr(fraFpsak.getAvkortetBrukersAndelPrÅr())
            .medAvkortetPrÅr(fraFpsak.getAvkortetPrÅr())
            .medAvkortetRefusjonPrÅr(fraFpsak.getAvkortetRefusjonPrÅr())
            .medBeregnetPrÅr(fraFpsak.getBeregnetPrÅr())
            .medBesteberegningPrÅr(fraFpsak.getBesteberegningPrÅr())
            .medFastsattAvSaksbehandler(fraFpsak.getFastsattAvSaksbehandler())
            .medOverstyrtPrÅr(fraFpsak.getOverstyrtPrÅr())
            .medFordeltPrÅr(fraFpsak.getFordeltPrÅr())
            .medRedusertPrÅr(fraFpsak.getRedusertPrÅr())
            .medRedusertBrukersAndelPrÅr(fraFpsak.getRedusertBrukersAndelPrÅr())
            .medMaksimalRefusjonPrÅr(fraFpsak.getMaksimalRefusjonPrÅr())
            .medRedusertRefusjonPrÅr(fraFpsak.getRedusertRefusjonPrÅr())
            .medÅrsbeløpFraTilstøtendeYtelse(fraFpsak.getÅrsbeløpFraTilstøtendeYtelse() == null ? null : fraFpsak.getÅrsbeløpFraTilstøtendeYtelse().getVerdi())
            .medInntektskategori(fraFpsak.getInntektskategori() == null ? null : Inntektskategori.fraKode(fraFpsak.getInntektskategori().getKode()))
            .medKilde(AndelKilde.fraKode(fraFpsak.getKilde().getKode()))
            .medOrginalDagsatsFraTilstøtendeYtelse(fraFpsak.getOrginalDagsatsFraTilstøtendeYtelse());


        if (fraFpsak.getBeregningsperiodeFom() != null) {
            builder.medBeregningsperiode(fraFpsak.getBeregningsperiodeFom(), fraFpsak.getBeregningsperiodeTom());
        }

        if (fraFpsak.getPgiSnitt() != null) {
            builder.medPgi(fraFpsak.getPgiSnitt(), List.of(fraFpsak.getPgi1(), fraFpsak.getPgi2(), fraFpsak.getPgi3()));
        }

        fraFpsak.getBgAndelArbeidsforhold().ifPresent(bgAndelArbeidsforhold -> builder.medBGAndelArbeidsforhold(BGMapperTilKalkulus.magBGAndelArbeidsforhold(bgAndelArbeidsforhold)));
        return builder;
    }

    private static BGAndelArbeidsforholdDto.Builder magBGAndelArbeidsforhold(BGAndelArbeidsforhold fraFpsak) {
        BGAndelArbeidsforholdDto.Builder builder = BGAndelArbeidsforholdDto.builder();
        builder.medArbeidsforholdRef(IAYMapperTilKalkulus.mapArbeidsforholdRef(fraFpsak.getArbeidsforholdRef()));
        builder.medArbeidsgiver(IAYMapperTilKalkulus.mapArbeidsgiver(fraFpsak.getArbeidsgiver()));
        builder.medArbeidsperiodeFom(fraFpsak.getArbeidsperiodeFom());
        builder.medRefusjonskravPrÅr(fraFpsak.getRefusjonskravPrÅr());
        builder.medSaksbehandletRefusjonPrÅr(fraFpsak.getSaksbehandletRefusjonPrÅr());
        builder.medFordeltRefusjonPrÅr(fraFpsak.getFordeltRefusjonPrÅr());
        fraFpsak.getArbeidsperiodeTom().ifPresent(builder::medArbeidsperiodeTom);
        fraFpsak.getNaturalytelseBortfaltPrÅr().ifPresent(builder::medNaturalytelseBortfaltPrÅr);
        fraFpsak.getNaturalytelseTilkommetPrÅr().ifPresent(builder::medNaturalytelseTilkommetPrÅr);
        return builder;
    }
}
