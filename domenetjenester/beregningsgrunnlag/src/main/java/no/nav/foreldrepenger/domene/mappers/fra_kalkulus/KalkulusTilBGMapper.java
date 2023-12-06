package no.nav.foreldrepenger.domene.mappers.fra_kalkulus;

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
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.SammenligningsgrunnlagPrStatusDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.FaktaVurdering;
import no.nav.folketrygdloven.kalkulator.output.RegelSporingPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.SammenligningsgrunnlagPrStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.modell.kodeverk.SammenligningsgrunnlagType;

public final class KalkulusTilBGMapper {

    private KalkulusTilBGMapper() {
    }

    public static BeregningsgrunnlagAktivitetStatus.Builder mapAktivitetStatus(BeregningsgrunnlagAktivitetStatusDto fraKalkulus) {
        var builder = new BeregningsgrunnlagAktivitetStatus.Builder();
        builder.medAktivitetStatus(AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode()));
        builder.medHjemmel(Hjemmel.fraKode(fraKalkulus.getHjemmel().getKode()));

        return builder;
    }

    public static BeregningsgrunnlagPeriode.Builder mapBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriodeDto fraKalkulus,
                                                                                 Optional<FaktaAggregatDto> faktaAggregat,
                                                                                 List<RegelSporingPeriode> regelSporingerPeriode) {
        var builder = new BeregningsgrunnlagPeriode.Builder();

        // med
        builder.medAvkortetPrÅr(fraKalkulus.getAvkortetPrÅr());
        builder.medBeregningsgrunnlagPeriode(fraKalkulus.getBeregningsgrunnlagPeriodeFom(), fraKalkulus.getBeregningsgrunnlagPeriodeTom());
        builder.medBruttoPrÅr(fraKalkulus.getBruttoPrÅr());
        builder.medRedusertPrÅr(fraKalkulus.getRedusertPrÅr());
        regelSporingerPeriode.forEach(rs -> builder.medRegelEvaluering(rs.regelInput(), rs.regelEvaluering(),
            BeregningsgrunnlagPeriodeRegelType.fraKode(rs.regelType().getKode())));

        // legg til
        fraKalkulus.getPeriodeÅrsaker().forEach(periodeÅrsak -> builder.leggTilPeriodeÅrsak(PeriodeÅrsak.fraKode(periodeÅrsak.getKode())));
        fraKalkulus.getBeregningsgrunnlagPrStatusOgAndelList()
            .forEach(statusOgAndel -> builder.leggTilBeregningsgrunnlagPrStatusOgAndel(mapStatusOgAndel(statusOgAndel, faktaAggregat)));

        return builder;
    }

    public static SammenligningsgrunnlagPrStatus.Builder mapSammenligningsgrunnlagMedStatus(SammenligningsgrunnlagPrStatusDto fraKalkulus) {
        var builder = new SammenligningsgrunnlagPrStatus.Builder();
        builder.medAvvikPromille(fraKalkulus.getAvvikPromilleNy());
        builder.medRapportertPrÅr(fraKalkulus.getRapportertPrÅr());
        builder.medSammenligningsgrunnlagType(mapSammenligningstype(fraKalkulus.getSammenligningsgrunnlagType()));
        builder.medSammenligningsperiode(fraKalkulus.getSammenligningsperiodeFom(), fraKalkulus.getSammenligningsperiodeTom());

        return builder;
    }

    private static SammenligningsgrunnlagType mapSammenligningstype(no.nav.folketrygdloven.kalkulus.kodeverk.SammenligningsgrunnlagType fraKalkulus) {
        return switch (fraKalkulus) {
            case SAMMENLIGNING_AT -> SammenligningsgrunnlagType.SAMMENLIGNING_AT;
            case SAMMENLIGNING_FL -> SammenligningsgrunnlagType.SAMMENLIGNING_FL;
            case SAMMENLIGNING_AT_FL -> SammenligningsgrunnlagType.SAMMENLIGNING_AT_FL;
            case SAMMENLIGNING_SN -> SammenligningsgrunnlagType.SAMMENLIGNING_SN;

            // SAMMENLIGNING_ATFL_SN: Type som kun brukes i GUI for å vise gamle sammenligningsgrunnlag før migrering, skal ikke lagres entiteter med denne typen
            // SAMMENLIGNING_MIDL_INAKTIV: Type som ikke er relevant for fp / svp saker
            case SAMMENLIGNING_ATFL_SN, SAMMENLIGNING_MIDL_INAKTIV ->
                throw new IllegalStateException("FEIL: Mottok ugyldig sammenligningsgrunnlagtype " + fraKalkulus);
        };
    }

    private static BeregningsgrunnlagPrStatusOgAndel.Builder mapStatusOgAndel(BeregningsgrunnlagPrStatusOgAndelDto fraKalkulus,
                                                                              Optional<FaktaAggregatDto> faktaAggregat) {
        var faktaAktør = faktaAggregat.flatMap(FaktaAggregatDto::getFaktaAktør);
        var faktaArbeidsforhold = fraKalkulus.getBgAndelArbeidsforhold()
            .flatMap(arbeidsforhold -> faktaAggregat.map(FaktaAggregatDto::getFaktaArbeidsforhold)
                .orElse(Collections.emptyList())
                .stream()
                .filter(fa -> fa.gjelderFor(arbeidsforhold.getArbeidsgiver(), arbeidsforhold.getArbeidsforholdRef()))
                .findFirst());

        var builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode()))
            .medAndelsnr(fraKalkulus.getAndelsnr())
            .medArbforholdType(
                fraKalkulus.getArbeidsforholdType() == null ? null : OpptjeningAktivitetType.fraKode(fraKalkulus.getArbeidsforholdType().getKode()))
            .medAvkortetBrukersAndelPrÅr(fraKalkulus.getAvkortetBrukersAndelPrÅr())
            .medAvkortetPrÅr(fraKalkulus.getAvkortetPrÅr())
            .medAvkortetRefusjonPrÅr(fraKalkulus.getAvkortetRefusjonPrÅr())
            .medBeregnetPrÅr(fraKalkulus.getBeregnetPrÅr())
            .medBesteberegningPrÅr(fraKalkulus.getBesteberegningPrÅr())
            .medFastsattAvSaksbehandler(fraKalkulus.getFastsattAvSaksbehandler())
            .medOverstyrtPrÅr(fraKalkulus.getOverstyrtPrÅr())
            .medFordeltPrÅr(fraKalkulus.getFordeltPrÅr())
            .medManueltFordeltPrÅr(fraKalkulus.getManueltFordeltPrÅr())
            .medRedusertPrÅr(fraKalkulus.getRedusertPrÅr())
            .medRedusertBrukersAndelPrÅr(fraKalkulus.getRedusertBrukersAndelPrÅr())
            .medMaksimalRefusjonPrÅr(fraKalkulus.getMaksimalRefusjonPrÅr())
            .medRedusertRefusjonPrÅr(fraKalkulus.getRedusertRefusjonPrÅr())
            .medÅrsbeløpFraTilstøtendeYtelse(
                fraKalkulus.getÅrsbeløpFraTilstøtendeYtelse() == null ? null : fraKalkulus.getÅrsbeløpFraTilstøtendeYtelse().getVerdi())
            .medNyIArbeidslivet(erNyIarbeidslivet(fraKalkulus, faktaAktør).orElse(null))
            .medInntektskategori(inntektskategoriErSatt(fraKalkulus)
                && fraKalkulus.getFastsattInntektskategori().getInntektskategori() != null ? Inntektskategori.fraKode(
                fraKalkulus.getFastsattInntektskategori().getInntektskategori().getKode()) : null)
            .medInntektskategoriManuellFordeling(inntektskategoriErSatt(fraKalkulus)
                && fraKalkulus.getFastsattInntektskategori().getInntektskategoriManuellFordeling() != null ? Inntektskategori.fraKode(
                fraKalkulus.getFastsattInntektskategori().getInntektskategoriManuellFordeling().getKode()) : null)
            .medInntektskategoriAutomatiskFordeling(inntektskategoriErSatt(fraKalkulus)
                && fraKalkulus.getFastsattInntektskategori().getInntektskategoriAutomatiskFordeling() != null ? Inntektskategori.fraKode(
                fraKalkulus.getFastsattInntektskategori().getInntektskategoriAutomatiskFordeling().getKode()) : null)

            .medKilde(AndelKilde.fraKode(fraKalkulus.getKilde().getKode()))
            .medOrginalDagsatsFraTilstøtendeYtelse(fraKalkulus.getOrginalDagsatsFraTilstøtendeYtelse());

        if (fraKalkulus.getBeregningsperiodeFom() != null) {
            builder.medBeregningsperiode(fraKalkulus.getBeregningsperiodeFom(), fraKalkulus.getBeregningsperiodeTom());
        }

        if (fraKalkulus.getPgiSnitt() != null) {
            builder.medPgi(fraKalkulus.getPgiSnitt(), List.of(fraKalkulus.getPgi1(), fraKalkulus.getPgi2(), fraKalkulus.getPgi3()));
        }

        fraKalkulus.getBgAndelArbeidsforhold()
            .ifPresent(bgAndelArbeidsforhold -> builder.medBGAndelArbeidsforhold(
                KalkulusTilBGMapper.magBGAndelArbeidsforhold(bgAndelArbeidsforhold, faktaArbeidsforhold)));
        erNyoppstartetFL(fraKalkulus, faktaAktør).ifPresent(
            aBoolean -> builder.medNyoppstartet(aBoolean, AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode())));
        builder.medMottarYtelse(mapMottarYtelse(fraKalkulus, faktaAktør, faktaArbeidsforhold).orElse(null),
            AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode()));
        return builder;
    }

    private static boolean inntektskategoriErSatt(BeregningsgrunnlagPrStatusOgAndelDto fraKalkulus) {
        return fraKalkulus.getFastsattInntektskategori() != null;
    }

    private static Optional<Boolean> erNyoppstartetFL(BeregningsgrunnlagPrStatusOgAndelDto fraKalkulus, Optional<FaktaAktørDto> faktaAktør) {
        if (!fraKalkulus.getAktivitetStatus().erFrilanser()) {
            return Optional.empty();
        }
        return faktaAktør.map(vurdering -> mapVurdering(vurdering.getErNyoppstartetFL()));
    }

    private static Optional<Boolean> erNyIarbeidslivet(BeregningsgrunnlagPrStatusOgAndelDto fraKalkulus, Optional<FaktaAktørDto> faktaAktør) {
        if (!fraKalkulus.getAktivitetStatus().erSelvstendigNæringsdrivende()) {
            return Optional.empty();
        }
        return faktaAktør.map(vurdering -> mapVurdering(vurdering.getErNyIArbeidslivetSN()));
    }

    private static Optional<Boolean> mapMottarYtelse(BeregningsgrunnlagPrStatusOgAndelDto fraKalkulus,
                                                     Optional<FaktaAktørDto> faktaAktør,
                                                     Optional<FaktaArbeidsforholdDto> faktaArbeidsforhold) {
        if (fraKalkulus.getAktivitetStatus().erFrilanser()) {
            return faktaAktør.map(vurdering -> mapVurdering(vurdering.getHarFLMottattYtelse()));
        }
        if (fraKalkulus.getAktivitetStatus().erArbeidstaker() && faktaArbeidsforhold.isPresent()) {
            return Optional.ofNullable(mapVurdering(faktaArbeidsforhold.get().getHarMottattYtelse()));
        }
        return Optional.empty();
    }

    private static BGAndelArbeidsforhold.Builder magBGAndelArbeidsforhold(BGAndelArbeidsforholdDto fraKalkulus,
                                                                          Optional<FaktaArbeidsforholdDto> faktaArbeidsforhold) {
        var builder = BGAndelArbeidsforhold.builder();
        builder.medArbeidsforholdRef(KalkulusTilIAYMapper.mapArbeidsforholdRef(fraKalkulus.getArbeidsforholdRef()));
        builder.medArbeidsgiver(KalkulusTilIAYMapper.mapArbeidsgiver(fraKalkulus.getArbeidsgiver()));
        builder.medArbeidsperiodeFom(fraKalkulus.getArbeidsperiodeFom());
        faktaArbeidsforhold.map(FaktaArbeidsforholdDto::getHarLønnsendringIBeregningsperioden)
            .ifPresent(fakta -> builder.medLønnsendringIBeregningsperioden(mapVurdering(fakta)));
        faktaArbeidsforhold.map(FaktaArbeidsforholdDto::getErTidsbegrenset)
            .ifPresent(fakta -> builder.medTidsbegrensetArbeidsforhold(mapVurdering(fakta)));
        builder.medRefusjonskravPrÅr(fraKalkulus.getInnvilgetRefusjonskravPrÅr());
        builder.medSaksbehandletRefusjonPrÅr(fraKalkulus.getSaksbehandletRefusjonPrÅr());
        builder.medFordeltRefusjonPrÅr(fraKalkulus.getFordeltRefusjonPrÅr());
        fraKalkulus.getRefusjon().ifPresent(ref -> builder.medManueltFordeltRefusjonPrÅr(ref.getManueltFordeltRefusjonPrÅr()));
        fraKalkulus.getArbeidsperiodeTom().ifPresent(builder::medArbeidsperiodeTom);
        fraKalkulus.getNaturalytelseBortfaltPrÅr().ifPresent(builder::medNaturalytelseBortfaltPrÅr);
        fraKalkulus.getNaturalytelseTilkommetPrÅr().ifPresent(builder::medNaturalytelseTilkommetPrÅr);
        return builder;
    }

    private static Boolean mapVurdering(FaktaVurdering faktaVurdering) {
        return faktaVurdering == null ? null : faktaVurdering.getVurdering();
    }
}
