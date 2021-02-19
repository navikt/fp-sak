package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.fra_kalkulus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BGAndelArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagAktivitetStatusDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.FaktaAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.FaktaAktørDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.FaktaArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.SammenligningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.SammenligningsgrunnlagPrStatusDto;
import no.nav.folketrygdloven.kalkulator.output.RegelSporingPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.AndelKilde;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.Hjemmel;
import no.nav.foreldrepenger.domene.modell.Inntektskategori;
import no.nav.foreldrepenger.domene.modell.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.modell.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagPrStatus;
import no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagType;

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

    public static BeregningsgrunnlagPeriode.Builder mapBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriodeDto fraKalkulus,
            Optional<FaktaAggregatDto> faktaAggregat, List<RegelSporingPeriode> regelSporingerPeriode) {
        BeregningsgrunnlagPeriode.Builder builder = new BeregningsgrunnlagPeriode.Builder();

        // med
        builder.medAvkortetPrÅr(fraKalkulus.getAvkortetPrÅr());
        builder.medBeregningsgrunnlagPeriode(fraKalkulus.getBeregningsgrunnlagPeriodeFom(), fraKalkulus.getBeregningsgrunnlagPeriodeTom());
        builder.medBruttoPrÅr(fraKalkulus.getBruttoPrÅr());
        builder.medRedusertPrÅr(fraKalkulus.getRedusertPrÅr());
        regelSporingerPeriode.forEach(rs -> builder.medRegelEvaluering(rs.getRegelInput(), rs.getRegelEvaluering(),
                BeregningsgrunnlagPeriodeRegelType.fraKode(rs.getRegelType().getKode())));

        // legg til
        fraKalkulus.getPeriodeÅrsaker().forEach(periodeÅrsak -> builder.leggTilPeriodeÅrsak(PeriodeÅrsak.fraKode(periodeÅrsak.getKode())));
        fraKalkulus.getBeregningsgrunnlagPrStatusOgAndelList()
                .forEach(statusOgAndel -> builder.leggTilBeregningsgrunnlagPrStatusOgAndel(mapStatusOgAndel(statusOgAndel, faktaAggregat)));

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

    private static BeregningsgrunnlagPrStatusOgAndel.Builder mapStatusOgAndel(BeregningsgrunnlagPrStatusOgAndelDto fraKalkulus,
            Optional<FaktaAggregatDto> faktaAggregat) {
        Optional<FaktaAktørDto> faktaAktør = faktaAggregat.flatMap(FaktaAggregatDto::getFaktaAktør);
        Optional<FaktaArbeidsforholdDto> faktaArbeidsforhold = fraKalkulus.getBgAndelArbeidsforhold()
                .flatMap(arbeidsforhold -> faktaAggregat.map(FaktaAggregatDto::getFaktaArbeidsforhold).orElse(Collections.emptyList())
                        .stream().filter(fa -> fa.gjelderFor(arbeidsforhold.getArbeidsgiver(), arbeidsforhold.getArbeidsforholdRef()))
                        .findFirst());

        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode()))
                .medAndelsnr(fraKalkulus.getAndelsnr())
                .medArbforholdType(fraKalkulus.getArbeidsforholdType() == null ? null
                        : OpptjeningAktivitetType.fraKode(fraKalkulus.getArbeidsforholdType().getKode()))
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
                .medÅrsbeløpFraTilstøtendeYtelse(
                        fraKalkulus.getÅrsbeløpFraTilstøtendeYtelse() == null ? null : fraKalkulus.getÅrsbeløpFraTilstøtendeYtelse().getVerdi())
                .medNyIArbeidslivet(erNyIarbeidslivet(fraKalkulus, faktaAktør))
                .medInntektskategori(
                        fraKalkulus.getInntektskategori() == null ? null : Inntektskategori.fraKode(fraKalkulus.getInntektskategori().getKode()))
                .medKilde(AndelKilde.fraKode(fraKalkulus.getKilde().getKode()))
                .medOrginalDagsatsFraTilstøtendeYtelse(fraKalkulus.getOrginalDagsatsFraTilstøtendeYtelse());

        if (fraKalkulus.getBeregningsperiodeFom() != null) {
            builder.medBeregningsperiode(fraKalkulus.getBeregningsperiodeFom(), fraKalkulus.getBeregningsperiodeTom());
        }

        if (fraKalkulus.getPgiSnitt() != null) {
            builder.medPgi(fraKalkulus.getPgiSnitt(), List.of(fraKalkulus.getPgi1(), fraKalkulus.getPgi2(), fraKalkulus.getPgi3()));
        }

        fraKalkulus.getBgAndelArbeidsforhold().ifPresent(bgAndelArbeidsforhold -> builder
                .medBGAndelArbeidsforhold(KalkulusTilBGMapper.magBGAndelArbeidsforhold(bgAndelArbeidsforhold, faktaArbeidsforhold)));
        erNyoppstartetFL(fraKalkulus, faktaAktør)
                .ifPresent(aBoolean -> builder.medNyoppstartet(aBoolean, AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode())));
        builder.medMottarYtelse(mapMottarYtelse(fraKalkulus, faktaAktør, faktaArbeidsforhold),
                AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode()));
        return builder;
    }

    private static Optional<Boolean> erNyoppstartetFL(BeregningsgrunnlagPrStatusOgAndelDto fraKalkulus, Optional<FaktaAktørDto> faktaAktør) {
        if (!fraKalkulus.getAktivitetStatus().erFrilanser()) {
            return Optional.empty();
        }
        return faktaAktør.map(FaktaAktørDto::getErNyoppstartetFL);
    }

    private static Boolean erNyIarbeidslivet(BeregningsgrunnlagPrStatusOgAndelDto fraKalkulus, Optional<FaktaAktørDto> faktaAktør) {
        if (!fraKalkulus.getAktivitetStatus().erSelvstendigNæringsdrivende()) {
            return null;
        }
        return faktaAktør.map(FaktaAktørDto::getErNyIArbeidslivetSN).orElse(null);
    }

    private static Boolean mapMottarYtelse(BeregningsgrunnlagPrStatusOgAndelDto fraKalkulus, Optional<FaktaAktørDto> faktaAktør,
            Optional<FaktaArbeidsforholdDto> faktaArbeidsforhold) {
        if (fraKalkulus.getAktivitetStatus().erFrilanser()) {
            return faktaAktør.map(FaktaAktørDto::getHarFLMottattYtelse).orElse(null);
        } else if (fraKalkulus.getAktivitetStatus().erArbeidstaker() && faktaArbeidsforhold.isPresent()) {
            return faktaArbeidsforhold.get().getHarMottattYtelse();
        }
        return null;
    }

    private static BGAndelArbeidsforhold.Builder magBGAndelArbeidsforhold(BGAndelArbeidsforholdDto fraKalkulus,
            Optional<FaktaArbeidsforholdDto> faktaArbeidsforhold) {
        BGAndelArbeidsforhold.Builder builder = BGAndelArbeidsforhold.builder();
        builder.medArbeidsforholdRef(KalkulusTilIAYMapper.mapArbeidsforholdRef(fraKalkulus.getArbeidsforholdRef()));
        builder.medArbeidsgiver(KalkulusTilIAYMapper.mapArbeidsgiver(fraKalkulus.getArbeidsgiver()));
        builder.medArbeidsperiodeFom(fraKalkulus.getArbeidsperiodeFom());
        faktaArbeidsforhold.map(FaktaArbeidsforholdDto::getHarLønnsendringIBeregningsperioden).ifPresent(builder::medLønnsendringIBeregningsperioden);
        faktaArbeidsforhold.map(FaktaArbeidsforholdDto::getErTidsbegrenset).ifPresent(builder::medTidsbegrensetArbeidsforhold);
        builder.medRefusjonskravPrÅr(fraKalkulus.getRefusjonskravPrÅr());
        builder.medSaksbehandletRefusjonPrÅr(fraKalkulus.getSaksbehandletRefusjonPrÅr());
        builder.medFordeltRefusjonPrÅr(fraKalkulus.getFordeltRefusjonPrÅr());
        fraKalkulus.getArbeidsperiodeTom().ifPresent(builder::medArbeidsperiodeTom);
        fraKalkulus.getNaturalytelseBortfaltPrÅr().ifPresent(builder::medNaturalytelseBortfaltPrÅr);
        fraKalkulus.getNaturalytelseTilkommetPrÅr().ifPresent(builder::medNaturalytelseTilkommetPrÅr);
        return builder;
    }
}
