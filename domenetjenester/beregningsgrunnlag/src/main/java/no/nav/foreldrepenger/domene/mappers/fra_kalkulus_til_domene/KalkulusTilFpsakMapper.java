package no.nav.foreldrepenger.domene.mappers.fra_kalkulus_til_domene;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetAggregatDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetOverstyringDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetOverstyringerDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningRefusjonOverstyringDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningRefusjonOverstyringerDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.SammenligningsgrunnlagPrStatusDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulator_til_entitet.KodeverkFraKalkulusMapper;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitet;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyring;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringer;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonOverstyring;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonOverstyringer;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagPrStatus;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

/**
 * Skal etterhvert benytte seg av kontrakten som skal lages i ft-Kalkulus, benytter foreløping en, en-til-en mapping på klassenivå...
 */
public final class KalkulusTilFpsakMapper {

    private KalkulusTilFpsakMapper() {
        // Hindrer instansiering
    }

    public static BeregningsgrunnlagGrunnlag map(BeregningsgrunnlagGrunnlagDto grunnlagDto) {
        return BeregningsgrunnlagGrunnlagBuilder.nytt()
            .medBeregningsgrunnlag(grunnlagDto.getBeregningsgrunnlag() == null ? null : mapGrunnlag(grunnlagDto.getBeregningsgrunnlag()))
            .medRegisterAktiviteter(grunnlagDto.getRegisterAktiviteter() == null ? null : mapAktiviteter(grunnlagDto.getRegisterAktiviteter()))
            .medSaksbehandletAktiviteter(grunnlagDto.getSaksbehandletAktiviteter() == null ? null : mapAktiviteter(grunnlagDto.getSaksbehandletAktiviteter()))
            .medOverstyring(grunnlagDto.getOverstyringer() == null ? null : mapAktivitetOverstyringer(grunnlagDto.getOverstyringer()))
            .medRefusjonOverstyring(grunnlagDto.getRefusjonOverstyringer() == null ? null : mapRefusjonoverstyringer(grunnlagDto.getRefusjonOverstyringer()))
            .build(KodeverkFraKalkulusMapper.mapTilstand(grunnlagDto.getBeregningsgrunnlagTilstand()));
    }

    private static BeregningRefusjonOverstyringer mapRefusjonoverstyringer(BeregningRefusjonOverstyringerDto refusjonOverstyringer) {
        var builder = BeregningRefusjonOverstyringer.builder();
        refusjonOverstyringer.getOverstyringer().stream().map(KalkulusTilFpsakMapper::mapRefusjonoverstyring).forEach(builder::leggTilOverstyring);
        return builder.build();
    }

    private static BeregningRefusjonOverstyring mapRefusjonoverstyring(BeregningRefusjonOverstyringDto r) {
        return new BeregningRefusjonOverstyring(mapArbeidsgiver(r.getArbeidsgiver()), r.getFørsteMuligeRefusjonFom());
    }

    private static BeregningAktivitetOverstyringer mapAktivitetOverstyringer(BeregningAktivitetOverstyringerDto overstyringer) {
        var builder = BeregningAktivitetOverstyringer.builder();
        overstyringer.getOverstyringer().stream().map(KalkulusTilFpsakMapper::mapAktivitetOverstyring).forEach(builder::leggTilOverstyring);
        return builder.build();
    }

    private static BeregningAktivitetOverstyring mapAktivitetOverstyring(BeregningAktivitetOverstyringDto aktivitetDto) {
        return BeregningAktivitetOverstyring.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(aktivitetDto.getPeriode().getFom(), aktivitetDto.getPeriode().getTom()))
            .medArbeidsgiver(aktivitetDto.getArbeidsgiver() == null ? null : mapArbeidsgiver(aktivitetDto.getArbeidsgiver()))
            .medArbeidsforholdRef(aktivitetDto.getArbeidsforholdRef() == null ? InternArbeidsforholdRef.nullRef() : InternArbeidsforholdRef.ref(aktivitetDto.getArbeidsforholdRef().getAbakusReferanse()))
            .medOpptjeningAktivitetType(KodeverkFraKalkulusMapper.mapOpptjeningtype(aktivitetDto.getOpptjeningAktivitetType()))
            .medHandling(KodeverkFraKalkulusMapper.mapHandling(aktivitetDto.getHandlingType()))
            .build();
    }

    private static BeregningAktivitetAggregat mapAktiviteter(BeregningAktivitetAggregatDto registerAktiviteter) {
        var builder = BeregningAktivitetAggregat.builder().medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        registerAktiviteter.getAktiviteter().stream().map(KalkulusTilFpsakMapper::mapAktivitet).forEach(builder::leggTilAktivitet);
        return builder.build();
    }

    private static BeregningAktivitet mapAktivitet(BeregningAktivitetDto aktivitetDto) {
        return BeregningAktivitet.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(aktivitetDto.getPeriode().getFom(), aktivitetDto.getPeriode().getTom()))
            .medArbeidsforholdRef(aktivitetDto.getArbeidsforholdRef() == null ? InternArbeidsforholdRef.nullRef() : InternArbeidsforholdRef.ref(aktivitetDto.getArbeidsforholdRef().getAbakusReferanse()))
            .medArbeidsgiver(aktivitetDto.getArbeidsgiver() == null ? null : mapArbeidsgiver(aktivitetDto.getArbeidsgiver()))
            .medOpptjeningAktivitetType(KodeverkFraKalkulusMapper.mapOpptjeningtype(aktivitetDto.getOpptjeningAktivitetType()))
            .build();
    }

    private static Beregningsgrunnlag mapGrunnlag(BeregningsgrunnlagDto beregningsgrunnlagDto) {
        var builder = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(beregningsgrunnlagDto.getSkjæringstidspunkt())
            .medGrunnbeløp(mapTilBeløp(beregningsgrunnlagDto.getGrunnbeløp()))
            .medOverstyring(beregningsgrunnlagDto.isOverstyrt());

        // Aktivitetstatuser
        beregningsgrunnlagDto.getAktivitetStatuser().stream()
            .map(KalkulusTilFpsakMapper::mapAktivitetstatus)
            .forEach(builder::leggTilAktivitetStatus);

        // Fakta tilfeller
        beregningsgrunnlagDto.getFaktaOmBeregningTilfeller().stream()
            .map(KodeverkFraKalkulusMapper::mapFaktaTilfelle)
            .forEach(builder::leggTilFaktaOmBeregningTilfelle);

        // Beregningsgrunnlagperioder
        beregningsgrunnlagDto.getBeregningsgrunnlagPerioder().stream()
            .map(KalkulusTilFpsakMapper::mapBeregningsgrunnlagperiodePeriode)
            .forEach(builder::leggTilBeregningsgrunnlagPeriode);

        // Sammenligningsgrunnlag
        beregningsgrunnlagDto.getSammenligningsgrunnlagPrStatusListe().stream()
            .map(KalkulusTilFpsakMapper::mapSammenligningsgrunnlag)
            .forEach(builder::leggTilSammenligningsgrunnlagPrStatus);

        return builder.build();
    }

    private static SammenligningsgrunnlagPrStatus mapSammenligningsgrunnlag(SammenligningsgrunnlagPrStatusDto sg) {
        return SammenligningsgrunnlagPrStatus.builder()
            .medSammenligningsgrunnlagType(KodeverkFraKalkulusMapper.mapSammenligningsgrunnlagType(sg.getSammenligningsgrunnlagType()))
            .medRapportertPrÅr(mapTilBigDecimal(sg.getRapportertPrÅr()))
            .medSammenligningsperiode(sg.getSammenligningsperiodeFom(), sg.getSammenligningsperiodeTom())
            .medAvvikPromille(sg.getAvvikPromilleNy().longValue())
            .build();
    }

    private static BeregningsgrunnlagPeriode mapBeregningsgrunnlagperiodePeriode(BeregningsgrunnlagPeriodeDto bgPeriodeDto) {
        var builder = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(bgPeriodeDto.getBeregningsgrunnlagPeriodeFom(), bgPeriodeDto.getBeregningsgrunnlagPeriodeTom())
            .medBruttoPrÅr(mapTilBigDecimal(bgPeriodeDto.getBruttoPrÅr()))
            .medRedusertPrÅr(mapTilBigDecimal(bgPeriodeDto.getRedusertPrÅr()))
            .medAvkortetPrÅr(mapTilBigDecimal(bgPeriodeDto.getAvkortetPrÅr()))
            .medDagsats(bgPeriodeDto.getDagsats());

        // Periodeårsaker
        bgPeriodeDto.getPeriodeÅrsaker().stream().map(KodeverkFraKalkulusMapper::mapPeriodeÅrsak).forEach(builder::leggTilPeriodeÅrsak);

        // Beregningsgrunnlagandeler
        bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(KalkulusTilFpsakMapper::mapAndel)
            .forEach(builder::leggTilBeregningsgrunnlagPrStatusOgAndel);

        return builder.build();
    }

    private static BeregningsgrunnlagPrStatusOgAndel mapAndel(BeregningsgrunnlagPrStatusOgAndelDto beregningsgrunnlagPrStatusOgAndelDto) {
        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(KodeverkFraKalkulusMapper.mapAktivitetstatus(beregningsgrunnlagPrStatusOgAndelDto.getAktivitetStatus()))
            .medAndelsnr(beregningsgrunnlagPrStatusOgAndelDto.getAndelsnr())
            .medArbforholdType(KodeverkFraKalkulusMapper.mapOpptjeningtype(beregningsgrunnlagPrStatusOgAndelDto.getArbeidsforholdType()))
            .medAvkortetBrukersAndelPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetBrukersAndelPrÅr()))
            .medAvkortetPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetPrÅr()))
            .medAvkortetRefusjonPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetRefusjonPrÅr()))
            .medBeregnetPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getBeregnetPrÅr()))
            .medBeregningsperiode(beregningsgrunnlagPrStatusOgAndelDto.getBeregningsperiodeFom(), beregningsgrunnlagPrStatusOgAndelDto.getBeregningsperiodeTom())
            .medFastsattAvSaksbehandler(beregningsgrunnlagPrStatusOgAndelDto.getFastsattAvSaksbehandler())
            .medFordeltPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getFordeltPrÅr()))
            .medBruttoPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getBruttoPrÅr()))
            .medInntektskategori(KodeverkFraKalkulusMapper.mapInntektskategori(beregningsgrunnlagPrStatusOgAndelDto.getInntektskategori()))
            .medLagtTilAvSaksbehandler(beregningsgrunnlagPrStatusOgAndelDto.getLagtTilAvSaksbehandler())
            .medMaksimalRefusjonPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getMaksimalRefusjonPrÅr()))
            .medOrginalDagsatsFraTilstøtendeYtelse(beregningsgrunnlagPrStatusOgAndelDto.getOrginalDagsatsFraTilstøtendeYtelse())
            .medOverstyrtPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getOverstyrtPrÅr()))
            .medRedusertBrukersAndelPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getRedusertBrukersAndelPrÅr()))
            .medRedusertPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getRedusertPrÅr()))
            .medRedusertRefusjonPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getRedusertRefusjonPrÅr()))
            .medÅrsbeløpFraTilstøtendeYtelse(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getÅrsbeløpFraTilstøtendeYtelse()))
            .medDagsatsBruker(beregningsgrunnlagPrStatusOgAndelDto.getDagsatsBruker())
            .medDagsatsArbeidsgiver(beregningsgrunnlagPrStatusOgAndelDto.getDagsatsArbeidsgiver());

        if (beregningsgrunnlagPrStatusOgAndelDto.getBgAndelArbeidsforhold() != null) {
            builder.medBGAndelArbeidsforhold(mapBgAndelArbeidsforhold(beregningsgrunnlagPrStatusOgAndelDto.getBgAndelArbeidsforhold()));
        }

        if (beregningsgrunnlagPrStatusOgAndelDto.getPgiSnitt() != null) {
            builder.medPgi(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getPgiSnitt()),
                List.of(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getPgi1()),
                    mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getPgi2()),
                    mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getPgi3())));
        }
        return builder.build();
    }

    private static BGAndelArbeidsforhold mapBgAndelArbeidsforhold(no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BGAndelArbeidsforhold bgAndelArbeidsforhold) {
        return BGAndelArbeidsforhold.builder()
            .medArbeidsforholdRef(Optional.ofNullable(bgAndelArbeidsforhold.getArbeidsforholdRef()).map(UUID::toString).orElse(null))
            .medArbeidsgiver(mapArbeidsgiver(bgAndelArbeidsforhold.getArbeidsgiver()))
            .medArbeidsperiodeFom(bgAndelArbeidsforhold.getArbeidsperiodeFom())
            .medArbeidsperiodeTom(bgAndelArbeidsforhold.getArbeidsperiodeTom())
            .medRefusjonskravPrÅr(mapTilBigDecimal(bgAndelArbeidsforhold.getRefusjonskravPrÅr()))
            .medNaturalytelseBortfaltPrÅr(no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.safeVerdi(bgAndelArbeidsforhold.getNaturalytelseBortfaltPrÅr()))
            .medNaturalytelseTilkommetPrÅr(no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.safeVerdi(bgAndelArbeidsforhold.getNaturalytelseTilkommetPrÅr()))
            .build();
    }

    private static BigDecimal mapTilBigDecimal(no.nav.folketrygdloven.kalkulus.felles.v1.Beløp beløp) {
        return beløp == null ? null : beløp.verdi();
    }

    private static BeregningsgrunnlagAktivitetStatus mapAktivitetstatus(AktivitetStatus aktivitetstatusDto) {
        return BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(KodeverkFraKalkulusMapper.mapAktivitetstatus(aktivitetstatusDto)) // TODO TFP-5742 Trengs hjemmel?
            .build();
    }

    private static Beløp mapTilBeløp(no.nav.folketrygdloven.kalkulus.felles.v1.Beløp beløpDto) {
        return beløpDto == null ? null : Beløp.fra(beløpDto.verdi());
    }

    private static Arbeidsgiver mapArbeidsgiver(no.nav.folketrygdloven.kalkulus.response.v1.Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver.getArbeidsgiverOrgnr() != null) {
            return Arbeidsgiver.virksomhet(arbeidsgiver.getArbeidsgiverOrgnr());
        }
        return Arbeidsgiver.person(new AktørId(arbeidsgiver.getArbeidsgiverAktørId()));
    }

}
