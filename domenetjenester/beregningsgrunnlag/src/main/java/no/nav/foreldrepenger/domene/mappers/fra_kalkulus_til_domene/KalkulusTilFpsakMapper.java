package no.nav.foreldrepenger.domene.mappers.fra_kalkulus_til_domene;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.folketrygdloven.kalkulus.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.kalkulus.kodeverk.PeriodeÅrsak;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetAggregatDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetOverstyringDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetOverstyringerDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningRefusjonOverstyringDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningRefusjonOverstyringerDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagAktivitetStatusDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.FaktaAggregatDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.FaktaAktørDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.FaktaArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.SammenligningsgrunnlagPrStatusDto;
import no.nav.folketrygdloven.kalkulus.response.v1.besteberegning.BesteberegningGrunnlagDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulator_til_entitet.KodeverkFraKalkulusMapper;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitet;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyring;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringer;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonOverstyring;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonOverstyringer;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonPeriode;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.BesteberegningGrunnlag;
import no.nav.foreldrepenger.domene.modell.BesteberegningInntekt;
import no.nav.foreldrepenger.domene.modell.BesteberegningMånedsgrunnlag;
import no.nav.foreldrepenger.domene.modell.FaktaAggregat;
import no.nav.foreldrepenger.domene.modell.FaktaAktør;
import no.nav.foreldrepenger.domene.modell.FaktaArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagPrStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaVurderingKilde;
import no.nav.foreldrepenger.domene.modell.typer.FaktaVurdering;
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

    public static BeregningsgrunnlagGrunnlag map(BeregningsgrunnlagGrunnlagDto grunnlagDto, Optional<BesteberegningGrunnlagDto> besteberegningGrunnlagDto) {
        return BeregningsgrunnlagGrunnlagBuilder.nytt()
            .medFakta(grunnlagDto.getFaktaAggregat() == null ? null : mapFakta(grunnlagDto.getFaktaAggregat()))
            .medBeregningsgrunnlag(grunnlagDto.getBeregningsgrunnlag() == null ? null : mapGrunnlag(grunnlagDto.getBeregningsgrunnlag(), besteberegningGrunnlagDto))
            .medRegisterAktiviteter(grunnlagDto.getRegisterAktiviteter() == null ? null : mapAktiviteter(grunnlagDto.getRegisterAktiviteter()))
            .medSaksbehandletAktiviteter(grunnlagDto.getSaksbehandletAktiviteter() == null ? null : mapAktiviteter(grunnlagDto.getSaksbehandletAktiviteter()))
            .medOverstyring(grunnlagDto.getOverstyringer() == null ? null : mapAktivitetOverstyringer(grunnlagDto.getOverstyringer()))
            .medRefusjonOverstyring(grunnlagDto.getRefusjonOverstyringer() == null ? null : mapRefusjonoverstyringer(grunnlagDto.getRefusjonOverstyringer()))
            .build(KodeverkFraKalkulusMapper.mapTilstand(grunnlagDto.getBeregningsgrunnlagTilstand()));
    }

    private static FaktaAggregat mapFakta(FaktaAggregatDto faktaAggregat) {
        var builder = FaktaAggregat.builder();
        builder.medFaktaAktør(faktaAggregat.getFaktaAktør() == null ? null : mapFaktaAktør(faktaAggregat.getFaktaAktør()));
        faktaAggregat.getFaktaArbeidsforholdListe().stream().map(KalkulusTilFpsakMapper::mapFaktaArbeidsforhold).forEach(builder::erstattEksisterendeEllerLeggTil);
        return builder.build();
    }

    private static FaktaArbeidsforhold mapFaktaArbeidsforhold(FaktaArbeidsforholdDto fa) {
        return FaktaArbeidsforhold.builder(mapArbeidsgiver(fa.getArbeidsgiver()), fa.getArbeidsforholdRef() == null
                ? InternArbeidsforholdRef.nullRef()
                : InternArbeidsforholdRef.ref(fa.getArbeidsforholdRef().getAbakusReferanse()))
            .medErTidsbegrenset(fa.getErTidsbegrenset() == null ? null : new FaktaVurdering(fa.getErTidsbegrenset(), FaktaVurderingKilde.SAKSBEHANDLER))
            .medHarMottattYtelse(fa.getHarMottattYtelse() == null ? null : new FaktaVurdering(fa.getHarMottattYtelse(), FaktaVurderingKilde.SAKSBEHANDLER))
            .medHarLønnsendringIBeregningsperioden(fa.getHarLønnsendringIBeregningsperioden() == null ? null : new FaktaVurdering(fa.getHarLønnsendringIBeregningsperioden(), FaktaVurderingKilde.SAKSBEHANDLER))
            .build();
    }

    private static FaktaAktør mapFaktaAktør(FaktaAktørDto faktaAktør) {
        var builder = FaktaAktør.builder();

        if (faktaAktør.getSkalBeregnesSomMilitær() != null) {
            builder.medErMilitærSiviltjeneste(new FaktaVurdering(faktaAktør.getSkalBeregnesSomMilitær(), FaktaVurderingKilde.SAKSBEHANDLER));
        }
        if (faktaAktør.getMottarEtterlønnSluttpakke() != null) {
            builder.medMottarEtterlønnSluttpakke(new FaktaVurdering(faktaAktør.getMottarEtterlønnSluttpakke(), FaktaVurderingKilde.SAKSBEHANDLER));
        }
        if (faktaAktør.getErNyoppstartetFL() != null) {
            builder.medErNyoppstartetFL(new FaktaVurdering(faktaAktør.getErNyoppstartetFL(), FaktaVurderingKilde.SAKSBEHANDLER));
        }
        if (faktaAktør.getErNyIArbeidslivetSN() != null) {
            builder.medErNyIArbeidslivetSN(new FaktaVurdering(faktaAktør.getErNyIArbeidslivetSN(), FaktaVurderingKilde.SAKSBEHANDLER));
        }
        if (faktaAktør.getHarFLMottattYtelse() != null) {
            builder.medHarFLMottattYtelse(new FaktaVurdering(faktaAktør.getHarFLMottattYtelse(), FaktaVurderingKilde.SAKSBEHANDLER));
        }
        if (faktaAktør.getSkalBesteberegnes() != null) {
            builder.medSkalBesteberegnes(new FaktaVurdering(faktaAktør.getSkalBesteberegnes(), FaktaVurderingKilde.SAKSBEHANDLER));
        }
        return builder.build();
    }

    public static BesteberegningGrunnlag mapBesteberegning(BesteberegningGrunnlagDto bbg) {
        var builder = BesteberegningGrunnlag.ny().medAvvik(mapTilBigDecimal(bbg.avvikFørsteOgTredjeLedd()));
        bbg.seksBesteMåneder().stream().map(KalkulusTilFpsakMapper::mapBesteberegningMåned).sorted(Comparator.comparing(bb -> bb.getPeriode().getFomDato())).forEach(builder::leggTilMånedsgrunnlag);
        return builder.build();
    }

    private static BesteberegningMånedsgrunnlag mapBesteberegningMåned(BesteberegningGrunnlagDto.BesteberegningMånedDto m) {
        var builder = BesteberegningMånedsgrunnlag.ny().medPeriode(m.periode().getFom(), m.periode().getTom());
        m.inntekter().stream()
            .map(KalkulusTilFpsakMapper::maBesteberegningInntekt)
            .forEach(builder::leggTilInntekt);
        return builder.build();
    }

    private static BesteberegningInntekt maBesteberegningInntekt(BesteberegningGrunnlagDto.BesteberegningInntektDto i) {
        return BesteberegningInntekt.ny().medInntekt(mapTilBigDecimal(i.inntekt()))
            .medArbeidsgiver(i.arbeidsgiver() == null ? null : mapArbeidsgiver(i.arbeidsgiver()))
            .medOpptjeningAktivitetType(KodeverkFraKalkulusMapper.mapOpptjeningtype(i.opptjeningAktiviteterDto()))
            .medArbeidsforholdRef(i.internArbeidsforholdRefDto() == null ? null : InternArbeidsforholdRef.ref(i.internArbeidsforholdRefDto().getAbakusReferanse()))
            .build();
    }

    private static BeregningRefusjonOverstyringer mapRefusjonoverstyringer(BeregningRefusjonOverstyringerDto refusjonOverstyringer) {
        var builder = BeregningRefusjonOverstyringer.builder();
        refusjonOverstyringer.getOverstyringer().stream()
            .map(KalkulusTilFpsakMapper::mapRefusjonoverstyring)
            .sorted(Comparator.comparing(BeregningRefusjonOverstyring::getFørsteMuligeRefusjonFom, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(a -> a.getArbeidsgiver().getIdentifikator()))
            .forEach(builder::leggTilOverstyring);
        return builder.build();
    }

    private static BeregningRefusjonOverstyring mapRefusjonoverstyring(BeregningRefusjonOverstyringDto r) {
        if (r.getRefusjonPerioder() == null) {
            return new BeregningRefusjonOverstyring(mapArbeidsgiver(r.getArbeidsgiver()), r.getFørsteMuligeRefusjonFom(), Boolean.TRUE.equals(r.getErFristUtvidet()), Collections.emptyList());
        }
        var refusjonsperioder = r.getRefusjonPerioder()
            .stream()
            .map(rp -> new BeregningRefusjonPeriode(
                rp.getArbeidsforholdRef() == null ? InternArbeidsforholdRef.nullRef() : InternArbeidsforholdRef.ref(
                    rp.getArbeidsforholdRef().getAbakusReferanse()), rp.getStartdatoRefusjon()))
            .sorted(Comparator.comparing(BeregningRefusjonPeriode::getStartdatoRefusjon)
                .thenComparing(a -> a.getArbeidsforholdRef().getReferanse(), Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
        return new BeregningRefusjonOverstyring(mapArbeidsgiver(r.getArbeidsgiver()), r.getFørsteMuligeRefusjonFom(), Boolean.TRUE.equals(r.getErFristUtvidet()), refusjonsperioder);
    }

    private static BeregningAktivitetOverstyringer mapAktivitetOverstyringer(BeregningAktivitetOverstyringerDto overstyringer) {
        var builder = BeregningAktivitetOverstyringer.builder();
        overstyringer.getOverstyringer().stream().map(KalkulusTilFpsakMapper::mapAktivitetOverstyring)
            .sorted(Comparator.comparing(BeregningAktivitetOverstyring::getOpptjeningAktivitetType)
            .thenComparing(a -> a.getPeriode().getFomDato())
            .thenComparing(a -> a.getPeriode().getTomDato())
            .thenComparing(a -> a.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(a -> a.getArbeidsforholdRef() == null ? null : a.getArbeidsforholdRef().getReferanse(), Comparator.nullsLast(Comparator.naturalOrder())))
            .forEach(builder::leggTilOverstyring);
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
        registerAktiviteter.getAktiviteter().stream().map(KalkulusTilFpsakMapper::mapAktivitet)
            .sorted(Comparator.comparing(BeregningAktivitet::getOpptjeningAktivitetType)
                .thenComparing(a -> a.getPeriode().getFomDato())
                .thenComparing(a -> a.getPeriode().getTomDato())
                .thenComparing(a -> a.getArbeidsgiver() == null ? null : a.getArbeidsgiver().getIdentifikator(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(a -> a.getArbeidsforholdRef() == null ? null : a.getArbeidsforholdRef().getReferanse(), Comparator.nullsLast(Comparator.naturalOrder())))
            .forEach(builder::leggTilAktivitet);
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

    public static Beregningsgrunnlag mapGrunnlag(BeregningsgrunnlagDto beregningsgrunnlagDto,
                                                  Optional<BesteberegningGrunnlagDto> besteberegningGrunnlagDto) {
        var builder = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(beregningsgrunnlagDto.getSkjæringstidspunkt())
            .medGrunnbeløp(mapTilBeløp(beregningsgrunnlagDto.getGrunnbeløp()))
            .medOverstyring(beregningsgrunnlagDto.isOverstyrt());

        // Aktivitetstatuser
        beregningsgrunnlagDto.getAktivitetStatuserMedHjemmel()
            .stream()
            .sorted(Comparator.comparing(BeregningsgrunnlagAktivitetStatusDto::getAktivitetStatus))
            .map(KalkulusTilFpsakMapper::mapAktivitetstatusMedHjemmel)
            .forEach(builder::leggTilAktivitetStatus);

        // Fakta tilfeller
        beregningsgrunnlagDto.getFaktaOmBeregningTilfeller().stream()
            .sorted(Comparator.comparing(FaktaOmBeregningTilfelle::getKode))
            .map(KodeverkFraKalkulusMapper::mapFaktaTilfelle)
            .forEach(builder::leggTilFaktaOmBeregningTilfelle);

        // Beregningsgrunnlagperioder
        beregningsgrunnlagDto.getBeregningsgrunnlagPerioder().stream()
            .sorted(Comparator.comparing(p -> p.getPeriode().getFom()))
            .map(KalkulusTilFpsakMapper::mapBeregningsgrunnlagperiodePeriode)
            .forEach(builder::leggTilBeregningsgrunnlagPeriode);

        // Sammenligningsgrunnlag
        beregningsgrunnlagDto.getSammenligningsgrunnlagPrStatusListe().stream()
            .map(KalkulusTilFpsakMapper::mapSammenligningsgrunnlag)
            .forEach(builder::leggTilSammenligningsgrunnlagPrStatus);

        // Besteberegning
        besteberegningGrunnlagDto.ifPresent(bbg -> builder.medBesteberegningsgrunnlag(mapBesteberegning(bbg)));

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
        bgPeriodeDto.getPeriodeÅrsaker().stream().sorted(Comparator.comparing(PeriodeÅrsak::getKode)).map(KodeverkFraKalkulusMapper::mapPeriodeÅrsak).forEach(builder::leggTilPeriodeÅrsak);

        // Beregningsgrunnlagandeler
        bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .sorted(Comparator.comparing(BeregningsgrunnlagPrStatusOgAndelDto::getAndelsnr))
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
            .medManueltFordeltPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getManueltFordeltPrÅr()))
            .medBruttoPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getBruttoPrÅr()))
            .medInntektskategori(KodeverkFraKalkulusMapper.mapInntektskategori(beregningsgrunnlagPrStatusOgAndelDto.getInntektskategori()))
            .medLagtTilAvSaksbehandler(beregningsgrunnlagPrStatusOgAndelDto.getLagtTilAvSaksbehandler())
            .medMaksimalRefusjonPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getMaksimalRefusjonPrÅr()))
            .medOrginalDagsatsFraTilstøtendeYtelse(beregningsgrunnlagPrStatusOgAndelDto.getOrginalDagsatsFraTilstøtendeYtelse())
            .medOverstyrtPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getOverstyrtPrÅr()))
            .medBesteberegnetPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getBesteberegningPrÅr()))
            .medRedusertBrukersAndelPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getRedusertBrukersAndelPrÅr()))
            .medRedusertPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getRedusertPrÅr()))
            .medRedusertRefusjonPrÅr(mapTilBigDecimal(beregningsgrunnlagPrStatusOgAndelDto.getRedusertRefusjonPrÅr()))
            .medKilde(beregningsgrunnlagPrStatusOgAndelDto.getKilde() == null ? null : KodeverkFraKalkulusMapper.mapKilde(beregningsgrunnlagPrStatusOgAndelDto.getKilde()))
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
            .medArbeidsforholdRef(bgAndelArbeidsforhold.getArbeidsforholdRef() == null ? InternArbeidsforholdRef.nullRef() : InternArbeidsforholdRef.ref(bgAndelArbeidsforhold.getArbeidsforholdRef()))
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

    private static BeregningsgrunnlagAktivitetStatus mapAktivitetstatusMedHjemmel(BeregningsgrunnlagAktivitetStatusDto dto) {
        return BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(KodeverkFraKalkulusMapper.mapAktivitetstatus(dto.getAktivitetStatus()))
            .medHjemmel(KodeverkFraKalkulusMapper.mapHjemmel(dto.getHjemmel()))
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
