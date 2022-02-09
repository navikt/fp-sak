package no.nav.foreldrepenger.domene.mappers.fra_entitet_til_modell;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringer;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningAktivitetHandlingType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;


public class FraEntitetTilBehandlingsmodellMapper {

    public static BeregningsgrunnlagGrunnlag mapBeregningsgrunnlagGrunnlag(BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet) {
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(grunnlagEntitet.getBeregningsgrunnlag().map(FraEntitetTilBehandlingsmodellMapper::mapBeregningsgrunnlag).orElse(null))
            .medRegisterAktiviteter(mapBeregningAktivitetAggregat(grunnlagEntitet.getRegisterAktiviteter()))
            .medSaksbehandletAktiviteter(
                grunnlagEntitet.getSaksbehandletAktiviteter().map(FraEntitetTilBehandlingsmodellMapper::mapBeregningAktivitetAggregat).orElse(null))
            .medOverstyring(grunnlagEntitet.getOverstyring().map(FraEntitetTilBehandlingsmodellMapper::mapBeregningAktivitetOverstyringer).orElse(null))
            .build(BeregningsgrunnlagTilstand.fraKode(grunnlagEntitet.getBeregningsgrunnlagTilstand().getKode()));
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringer mapBeregningAktivitetOverstyringer(
        BeregningAktivitetOverstyringerEntitet overstyringer) {
        no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringer.Builder builder = BeregningAktivitetOverstyringer.builder();
        overstyringer.getOverstyringer().stream().map(FraEntitetTilBehandlingsmodellMapper::mapAktivitetOverstyring).forEach(builder::leggTilOverstyring);
        return builder.build();
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyring mapAktivitetOverstyring(BeregningAktivitetOverstyringEntitet beregningAktivitetOverstyringEntitet) {
        return no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyring.builder()
            .medArbeidsgiver(beregningAktivitetOverstyringEntitet.getArbeidsgiver().orElse(null))
            .medArbeidsforholdRef(beregningAktivitetOverstyringEntitet.getArbeidsforholdRef())
            .medHandling(BeregningAktivitetHandlingType.fraKode(beregningAktivitetOverstyringEntitet.getHandling().getKode()))
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(beregningAktivitetOverstyringEntitet.getPeriode().getFomDato(),
                beregningAktivitetOverstyringEntitet.getPeriode().getTomDato()))
            .build();
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat mapBeregningAktivitetAggregat(BeregningAktivitetAggregatEntitet registerAktiviteter) {
        no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat.Builder builder = BeregningAktivitetAggregat.builder()
            .medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        registerAktiviteter.getBeregningAktiviteter().stream().map(FraEntitetTilBehandlingsmodellMapper::mapBeregningAktivitet).forEach(builder::leggTilAktivitet);
        return builder.build();
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningAktivitet mapBeregningAktivitet(BeregningAktivitetEntitet beregningAktivitetDto) {
        return no.nav.foreldrepenger.domene.modell.BeregningAktivitet.builder()
            .medArbeidsforholdRef(beregningAktivitetDto.getArbeidsforholdRef())
            .medArbeidsgiver(beregningAktivitetDto.getArbeidsgiver())
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.fraKode(beregningAktivitetDto.getOpptjeningAktivitetType().getKode()))
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(beregningAktivitetDto.getPeriode().getFomDato(),
                beregningAktivitetDto.getPeriode().getTomDato()))
            .build();
    }

    public static Beregningsgrunnlag mapBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlagDto) {
        var builder = Beregningsgrunnlag.builder()
            .medOverstyring(beregningsgrunnlagDto.isOverstyrt())
            .medSkjæringstidspunkt(beregningsgrunnlagDto.getSkjæringstidspunkt())
            .medGrunnbeløp(beregningsgrunnlagDto.getGrunnbeløp());


        beregningsgrunnlagDto.getAktivitetStatuser()
            .forEach(aktivitetStatus -> builder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(AktivitetStatus.fraKode(aktivitetStatus.getAktivitetStatus().getKode()))));
        if (beregningsgrunnlagDto.getFaktaOmBeregningTilfeller() != null) {
            List<FaktaOmBeregningTilfelle> tilfeller = beregningsgrunnlagDto.getFaktaOmBeregningTilfeller()
                .stream()
                .map(t -> FaktaOmBeregningTilfelle.fraKode(t.getKode()))
                .collect(Collectors.toList());
            builder.leggTilFaktaOmBeregningTilfeller(tilfeller);
        }
        Beregningsgrunnlag bg = builder.build();

        mapPerioder(beregningsgrunnlagDto.getBeregningsgrunnlagPerioder()).forEach(periodeBuilder -> periodeBuilder.build(bg));
        if (beregningsgrunnlagDto.getSammenligningsgrunnlag().isPresent()) {
            mapSammenligningsgrunnlag(beregningsgrunnlagDto.getSammenligningsgrunnlag().get()).build(bg);
        }
        return bg;
    }

    private static List<no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode.Builder> mapPerioder(List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder) {
        return beregningsgrunnlagPerioder.stream().map(FraEntitetTilBehandlingsmodellMapper::mapPeriode).collect(Collectors.toList());
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode.Builder mapPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriodeDto) {
        no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode.Builder periodeBuilder = no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPeriodeFom(),
                beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPeriodeTom())
            .medAvkortetPrÅr(beregningsgrunnlagPeriodeDto.getAvkortetPrÅr())
            .medBruttoPrÅr(beregningsgrunnlagPeriodeDto.getBruttoPrÅr())
            .medRedusertPrÅr(beregningsgrunnlagPeriodeDto.getRedusertPrÅr());
        mapAndeler(beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList()).forEach(
            periodeBuilder::leggTilBeregningsgrunnlagPrStatusOgAndel);
        return periodeBuilder;
    }

    private static List<no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel.Builder> mapAndeler(List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList) {
        return beregningsgrunnlagPrStatusOgAndelList.stream().map(FraEntitetTilBehandlingsmodellMapper::mapAndel).collect(Collectors.toList());
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel.Builder mapAndel(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndelDto) {
        no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel.Builder builder = no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.fraKode(beregningsgrunnlagPrStatusOgAndelDto.getAktivitetStatus().getKode()))
            .medAndelsnr(beregningsgrunnlagPrStatusOgAndelDto.getAndelsnr())
            .medArbforholdType(OpptjeningAktivitetType.fraKode(beregningsgrunnlagPrStatusOgAndelDto.getArbeidsforholdType().getKode()))
            .medAvkortetBrukersAndelPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetBrukersAndelPrÅr())
            .medAvkortetPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetPrÅr())
            .medAvkortetRefusjonPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetRefusjonPrÅr())
            .medBeregnetPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getBeregnetPrÅr())
            .medBeregningsperiode(beregningsgrunnlagPrStatusOgAndelDto.getBeregningsperiodeFom(),
                beregningsgrunnlagPrStatusOgAndelDto.getBeregningsperiodeTom())
            .medFastsattAvSaksbehandler(beregningsgrunnlagPrStatusOgAndelDto.getFastsattAvSaksbehandler())
            .medFordeltPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getFordeltPrÅr())
            .medInntektskategori(Inntektskategori.fraKode(beregningsgrunnlagPrStatusOgAndelDto.getInntektskategori().getKode()))
            .medLagtTilAvSaksbehandler(beregningsgrunnlagPrStatusOgAndelDto.erLagtTilAvSaksbehandler())
            .medMaksimalRefusjonPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getMaksimalRefusjonPrÅr())
            .medOrginalDagsatsFraTilstøtendeYtelse(beregningsgrunnlagPrStatusOgAndelDto.getOrginalDagsatsFraTilstøtendeYtelse())
            .medOverstyrtPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getOverstyrtPrÅr())
            .medRedusertBrukersAndelPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getRedusertBrukersAndelPrÅr())
            .medRedusertPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getRedusertPrÅr())
            .medRedusertRefusjonPrÅr(beregningsgrunnlagPrStatusOgAndelDto.getRedusertRefusjonPrÅr())
            .medÅrsbeløpFraTilstøtendeYtelse(beregningsgrunnlagPrStatusOgAndelDto.getÅrsbeløpFraTilstøtendeYtelse().getVerdi());

        if (beregningsgrunnlagPrStatusOgAndelDto.getBgAndelArbeidsforhold().isPresent()) {
            builder.medBGAndelArbeidsforhold(
                FraEntitetTilBehandlingsmodellMapper.mapBgAndelArbeidsforhold(beregningsgrunnlagPrStatusOgAndelDto.getBgAndelArbeidsforhold().get()));
        }

        if (beregningsgrunnlagPrStatusOgAndelDto.getPgiSnitt() != null) {
            builder.medPgi(beregningsgrunnlagPrStatusOgAndelDto.getPgiSnitt(),
                List.of(beregningsgrunnlagPrStatusOgAndelDto.getPgi1(), beregningsgrunnlagPrStatusOgAndelDto.getPgi2(),
                    beregningsgrunnlagPrStatusOgAndelDto.getPgi3()));
        }
        return builder;
    }

    private static no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold.Builder mapBgAndelArbeidsforhold(BGAndelArbeidsforhold bgAndelArbeidsforhold) {
        return no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold.builder()
            .medArbeidsforholdRef(bgAndelArbeidsforhold.getArbeidsforholdRef())
            .medArbeidsgiver(bgAndelArbeidsforhold.getArbeidsgiver())
            .medArbeidsperiodeFom(bgAndelArbeidsforhold.getArbeidsperiodeFom())
            .medArbeidsperiodeTom(bgAndelArbeidsforhold.getArbeidsperiodeFom())
            .medNaturalytelseBortfaltPrÅr(bgAndelArbeidsforhold.getNaturalytelseBortfaltPrÅr().orElse(null))
            .medNaturalytelseTilkommetPrÅr(bgAndelArbeidsforhold.getNaturalytelseTilkommetPrÅr().orElse(null));
    }

    private static no.nav.foreldrepenger.domene.modell.Sammenligningsgrunnlag.Builder mapSammenligningsgrunnlag(Sammenligningsgrunnlag sammenligningsgrunnlag) {
        return no.nav.foreldrepenger.domene.modell.Sammenligningsgrunnlag.builder()
            .medSammenligningsperiode(sammenligningsgrunnlag.getSammenligningsperiodeFom(), sammenligningsgrunnlag.getSammenligningsperiodeTom())
            .medRapportertPrÅr(sammenligningsgrunnlag.getRapportertPrÅr())
            .medAvvikPromille(sammenligningsgrunnlag.getAvvikPromille().longValue());
    }
}
