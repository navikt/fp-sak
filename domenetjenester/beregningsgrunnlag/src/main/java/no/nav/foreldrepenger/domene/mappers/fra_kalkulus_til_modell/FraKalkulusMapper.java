package no.nav.foreldrepenger.domene.mappers.fra_kalkulus_til_modell;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetAggregatDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetOverstyringDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetOverstyringerDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitet;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyring;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringer;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningAktivitetHandlingType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;


public class FraKalkulusMapper {

    public static BeregningsgrunnlagGrunnlag mapBeregningsgrunnlagGrunnlag(BeregningsgrunnlagGrunnlagDto grunnlagDto) {
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(grunnlagDto.getBeregningsgrunnlag() == null ? null : mapBeregningsgrunnlag(grunnlagDto.getBeregningsgrunnlag()))
                .medRegisterAktiviteter(mapBeregningAktivitetAggregat(grunnlagDto.getRegisterAktiviteter()))
                .medSaksbehandletAktiviteter(grunnlagDto.getSaksbehandletAktiviteter() == null ? null : mapBeregningAktivitetAggregat(grunnlagDto.getSaksbehandletAktiviteter()))
                .medOverstyring(grunnlagDto.getOverstyringer() == null ? null : mapBeregningAktivitetOverstyringer(grunnlagDto.getOverstyringer()))
                .build(BeregningsgrunnlagTilstand.fraKode(grunnlagDto.getBeregningsgrunnlagTilstand().getKode()));
    }

    private static BeregningAktivitetOverstyringer mapBeregningAktivitetOverstyringer(BeregningAktivitetOverstyringerDto overstyringer) {
        BeregningAktivitetOverstyringer.Builder builder = BeregningAktivitetOverstyringer.builder();
        overstyringer.getOverstyringer().stream().map(FraKalkulusMapper::mapAktivitetOverstyring).forEach(builder::leggTilOverstyring);
        return builder.build();
    }

    private static BeregningAktivitetOverstyring mapAktivitetOverstyring(BeregningAktivitetOverstyringDto beregningAktivitetOverstyringDto) {
        return BeregningAktivitetOverstyring.builder()
                .medArbeidsgiver(beregningAktivitetOverstyringDto.getArbeidsgiver() == null ? null : mapArbeidsgiver(beregningAktivitetOverstyringDto.getArbeidsgiver()))
                .medArbeidsforholdRef(mapArbeidsforholdRef(beregningAktivitetOverstyringDto.getArbeidsforholdRef()))
                .medHandling(BeregningAktivitetHandlingType.fraKode(beregningAktivitetOverstyringDto.getHandlingType().getKode()))
                .medPeriode(
                    ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(beregningAktivitetOverstyringDto.getPeriode().getFom(), beregningAktivitetOverstyringDto.getPeriode().getTom()))
                .build();
    }

    private static InternArbeidsforholdRef mapArbeidsforholdRef(InternArbeidsforholdRefDto arbeidsforholdRef) {
        if (arbeidsforholdRef == null) {
            return InternArbeidsforholdRef.nullRef();
        }
        return InternArbeidsforholdRef.ref(arbeidsforholdRef.getAbakusReferanse());
    }

    private static BeregningAktivitetAggregat mapBeregningAktivitetAggregat(BeregningAktivitetAggregatDto registerAktiviteter) {
        BeregningAktivitetAggregat.Builder builder = BeregningAktivitetAggregat.builder()
                .medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        registerAktiviteter.getAktiviteter().stream()
                .map(FraKalkulusMapper::mapBeregningAktivitet)
                .forEach(builder::leggTilAktivitet);
        return builder
                .build();
    }

    private static BeregningAktivitet mapBeregningAktivitet(BeregningAktivitetDto beregningAktivitetDto) {
        return BeregningAktivitet.builder()
                .medArbeidsforholdRef(mapArbeidsforholdRef(beregningAktivitetDto.getArbeidsforholdRef()))
                .medArbeidsgiver(beregningAktivitetDto.getArbeidsgiver() == null ? null : mapArbeidsgiver(beregningAktivitetDto.getArbeidsgiver()))
                .medOpptjeningAktivitetType(OpptjeningAktivitetType.fraKode(beregningAktivitetDto.getOpptjeningAktivitetType().getKode()))
                .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(beregningAktivitetDto.getPeriode().getFom(), beregningAktivitetDto.getPeriode().getTom()))
                .build();
    }

    public static Beregningsgrunnlag mapBeregningsgrunnlag(BeregningsgrunnlagDto beregningsgrunnlagDto) {
        var builder = Beregningsgrunnlag.builder()
                .medOverstyring(beregningsgrunnlagDto.isOverstyrt())
                .medSkjæringstidspunkt(beregningsgrunnlagDto.getSkjæringstidspunkt())
                .medGrunnbeløp(beregningsgrunnlagDto.getGrunnbeløp());


        beregningsgrunnlagDto.getAktivitetStatuser().forEach(aktivitetStatus -> {
            builder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.fraKode(aktivitetStatus.getKode())));
        });
        if (beregningsgrunnlagDto.getFaktaOmBeregningTilfeller() != null) {
            List<FaktaOmBeregningTilfelle> tilfeller = beregningsgrunnlagDto.getFaktaOmBeregningTilfeller().stream()
                .map(t -> FaktaOmBeregningTilfelle.fraKode(t.getKode()))
                .collect(Collectors.toList());
            builder.leggTilFaktaOmBeregningTilfeller(tilfeller);
        }
        Beregningsgrunnlag bg = builder.build();

        mapPerioder(beregningsgrunnlagDto.getBeregningsgrunnlagPerioder())
                .forEach(periodeBuilder -> periodeBuilder.build(bg));
        if (beregningsgrunnlagDto.getSammenligningsgrunnlag() != null) {
            mapSammenligningsgrunnlag(beregningsgrunnlagDto.getSammenligningsgrunnlag()).build(bg);
        }
        return bg;
    }

    private static List<BeregningsgrunnlagPeriode.Builder> mapPerioder(List<BeregningsgrunnlagPeriodeDto> beregningsgrunnlagPerioder) {
        return beregningsgrunnlagPerioder.stream()
                .map(FraKalkulusMapper::mapPeriode)
                .collect(Collectors.toList());
    }

    private static BeregningsgrunnlagPeriode.Builder mapPeriode(BeregningsgrunnlagPeriodeDto beregningsgrunnlagPeriodeDto) {
        BeregningsgrunnlagPeriode.Builder periodeBuilder = BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPeriodeFom(), beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPeriodeTom())
                .medAvkortetPrÅr(beregningsgrunnlagPeriodeDto.getAvkortetPrÅr())
                .medBruttoPrÅr(beregningsgrunnlagPeriodeDto.getBruttoPrÅr())
                .medRedusertPrÅr(beregningsgrunnlagPeriodeDto.getRedusertPrÅr());
        mapAndeler(beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList()).forEach(periodeBuilder::leggTilBeregningsgrunnlagPrStatusOgAndel);
        return periodeBuilder;
    }

    private static List<BeregningsgrunnlagPrStatusOgAndel.Builder> mapAndeler(List<BeregningsgrunnlagPrStatusOgAndelDto> beregningsgrunnlagPrStatusOgAndelList) {
        return beregningsgrunnlagPrStatusOgAndelList.stream()
                .map(FraKalkulusMapper::mapAndel)
                .collect(Collectors.toList());
    }

    private static BeregningsgrunnlagPrStatusOgAndel.Builder mapAndel(BeregningsgrunnlagPrStatusOgAndelDto beregningsgrunnlagPrStatusOgAndelDto) {
        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(AktivitetStatus.fraKode(beregningsgrunnlagPrStatusOgAndelDto.getAktivitetStatus().getKode()))
                .medAndelsnr(beregningsgrunnlagPrStatusOgAndelDto.getAndelsnr())
                .medArbforholdType(OpptjeningAktivitetType.fraKode(beregningsgrunnlagPrStatusOgAndelDto.getArbeidsforholdType().getKode()))
                .medAvkortetBrukersAndelPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetBrukersAndelPrÅr())
                .medAvkortetPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetPrÅr())
                .medAvkortetRefusjonPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetRefusjonPrÅr())
                .medBeregnetPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getBeregnetPrÅr())
                .medBeregningsperiode(beregningsgrunnlagPrStatusOgAndelDto.getBeregningsperiodeFom(), beregningsgrunnlagPrStatusOgAndelDto.getBeregningsperiodeTom())
                .medFastsattAvSaksbehandler(beregningsgrunnlagPrStatusOgAndelDto.getFastsattAvSaksbehandler())
                .medFordeltPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getFordeltPrÅr())
                .medInntektskategori(Inntektskategori.fraKode(beregningsgrunnlagPrStatusOgAndelDto.getInntektskategori().getKode()))
                .medLagtTilAvSaksbehandler(beregningsgrunnlagPrStatusOgAndelDto.getLagtTilAvSaksbehandler())
                .medMaksimalRefusjonPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getMaksimalRefusjonPrÅr())
                .medOrginalDagsatsFraTilstøtendeYtelse(beregningsgrunnlagPrStatusOgAndelDto.getOrginalDagsatsFraTilstøtendeYtelse())
                .medOverstyrtPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getOverstyrtPrÅr())
                .medRedusertBrukersAndelPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getRedusertBrukersAndelPrÅr())
                .medRedusertPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getRedusertPrÅr())
                .medRedusertRefusjonPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getRedusertRefusjonPrÅr())
                .medÅrsbeløpFraTilstøtendeYtelse(beregningsgrunnlagPrStatusOgAndelDto.getÅrsbeløpFraTilstøtendeYtelse());

        if (beregningsgrunnlagPrStatusOgAndelDto.getBgAndelArbeidsforhold() != null) {
            builder.medBGAndelArbeidsforhold(FraKalkulusMapper.mapBgAndelArbeidsforhold(beregningsgrunnlagPrStatusOgAndelDto.getBgAndelArbeidsforhold()));
        }

        if (beregningsgrunnlagPrStatusOgAndelDto.getPgiSnitt() != null) {
            builder.medPgi(beregningsgrunnlagPrStatusOgAndelDto.getPgiSnitt(), List.of(beregningsgrunnlagPrStatusOgAndelDto.getPgi1(), beregningsgrunnlagPrStatusOgAndelDto.getPgi2(), beregningsgrunnlagPrStatusOgAndelDto.getPgi3()));
        }
        return builder;
    }

    private static no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold.Builder mapBgAndelArbeidsforhold(BGAndelArbeidsforhold bgAndelArbeidsforhold) {
        return no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold.builder()
                .medArbeidsforholdRef(bgAndelArbeidsforhold.getArbeidsforholdRef())
                .medArbeidsgiver(mapArbeidsgiver(bgAndelArbeidsforhold.getArbeidsgiver()))
                .medArbeidsperiodeFom(bgAndelArbeidsforhold.getArbeidsperiodeFom())
                .medArbeidsperiodeTom(bgAndelArbeidsforhold.getArbeidsperiodeFom())
                .medNaturalytelseBortfaltPrÅr(bgAndelArbeidsforhold.getNaturalytelseBortfaltPrÅr())
                .medNaturalytelseTilkommetPrÅr(bgAndelArbeidsforhold.getNaturalytelseTilkommetPrÅr());
    }

    private static Arbeidsgiver mapArbeidsgiver(no.nav.folketrygdloven.kalkulus.response.v1.Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver.getArbeidsgiverOrgnr() != null) {
            return Arbeidsgiver.virksomhet(arbeidsgiver.getArbeidsgiverOrgnr());
        }
        return Arbeidsgiver.person(new AktørId(arbeidsgiver.getArbeidsgiverAktørId()));
    }

    private static Sammenligningsgrunnlag.Builder mapSammenligningsgrunnlag(no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.Sammenligningsgrunnlag sammenligningsgrunnlag) {
        return Sammenligningsgrunnlag.builder()
                .medSammenligningsperiode(sammenligningsgrunnlag.getSammenligningsperiodeFom(), sammenligningsgrunnlag.getSammenligningsperiodeTom())
                .medRapportertPrÅr(sammenligningsgrunnlag.getRapportertPrÅr())
                .medAvvikPromille(sammenligningsgrunnlag.getAvvikPromilleNy().longValue());
    }
}
