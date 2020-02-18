package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulator.kontrakt.v1.ArbeidsgiverOpplysningerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BGAndelArbeidsforholdRestDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetAggregatRestDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetOverstyringDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetOverstyringerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetRestDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningRefusjonOverstyringDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningRefusjonOverstyringerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagAktivitetStatusRestDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagArbeidstakerAndelRestDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagFrilansAndelRestDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagRestDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagRestDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagPeriodeRestDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagPrStatusOgAndelRestDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagRestDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.SammenligningsgrunnlagPrStatusRestDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.SammenligningsgrunnlagRestDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingDto;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulator.modell.typer.AktørId;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.modell.virksomhet.Arbeidsgiver;
import no.nav.folketrygdloven.kalkulator.modell.virksomhet.ArbeidsgiverMedNavn;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningAktivitetHandlingType;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.Hjemmel;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.Inntektskategori;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.PeriodeÅrsak;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.SammenligningsgrunnlagType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.SammenligningsgrunnlagPrStatus;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

class BeregningsgrunnlagRestDtoMapper {


    public static BeregningsgrunnlagRestDto mapBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlagFraFpsak, Map<Arbeidsgiver, ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysninger, Collection<InntektsmeldingDto> inntektsmeldinger) {
        BeregningsgrunnlagRestDto.Builder builder = BeregningsgrunnlagRestDto.builder();

        //med
        builder.medGrunnbeløp(beregningsgrunnlagFraFpsak.getGrunnbeløp() == null ? null : beregningsgrunnlagFraFpsak.getGrunnbeløp().getVerdi());
        builder.medOverstyring(beregningsgrunnlagFraFpsak.isOverstyrt());
        builder.medSkjæringstidspunkt(beregningsgrunnlagFraFpsak.getSkjæringstidspunkt());
        if (beregningsgrunnlagFraFpsak.getSammenligningsgrunnlag() != null) {
            builder.medSammenligningsgrunnlag(mapSammenligningsgrunnlag(beregningsgrunnlagFraFpsak.getSammenligningsgrunnlag()));
        }

        //lister
        beregningsgrunnlagFraFpsak.getAktivitetStatuser().forEach(beregningsgrunnlagAktivitetStatus -> builder.leggTilAktivitetStatus(mapAktivitetStatus(beregningsgrunnlagAktivitetStatus)));
        beregningsgrunnlagFraFpsak.getBeregningsgrunnlagPerioder().forEach(beregningsgrunnlagPeriode -> builder.leggTilBeregningsgrunnlagPeriode(mapBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode, arbeidsgiverOpplysninger, inntektsmeldinger)));
        builder.leggTilFaktaOmBeregningTilfeller(beregningsgrunnlagFraFpsak.getFaktaOmBeregningTilfeller().stream().map(fakta -> FaktaOmBeregningTilfelle.fraKode(fakta.getKode())).collect(Collectors.toList()));
        beregningsgrunnlagFraFpsak.getSammenligningsgrunnlagPrStatusListe().forEach(sammenligningsgrunnlagPrStatus -> builder.leggTilSammenligningsgrunnlag(mapSammenligningsgrunnlagMedStatus(sammenligningsgrunnlagPrStatus)));

        return builder.build();
    }

    public static BeregningRefusjonOverstyringerDto mapRefusjonOverstyring(BeregningRefusjonOverstyringerEntitet refusjonOverstyringerFraFpsak) {
        BeregningRefusjonOverstyringerDto.Builder dtoBuilder = BeregningRefusjonOverstyringerDto.builder();

        refusjonOverstyringerFraFpsak.getRefusjonOverstyringer().forEach(beregningRefusjonOverstyring -> {
            BeregningRefusjonOverstyringDto dto = new BeregningRefusjonOverstyringDto(IAYMapperTilKalkulus.mapArbeidsgiver(beregningRefusjonOverstyring.getArbeidsgiver()), beregningRefusjonOverstyring.getFørsteMuligeRefusjonFom());
            dtoBuilder.leggTilOverstyring(dto);
        });
        return dtoBuilder.build();
    }

    public static BeregningAktivitetAggregatRestDto mapSaksbehandletAktivitet(BeregningAktivitetAggregatEntitet saksbehandletAktiviteterFraFpsak, Map<Arbeidsgiver, ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysninger) {
        BeregningAktivitetAggregatRestDto.Builder dtoBuilder = BeregningAktivitetAggregatRestDto.builder();
        dtoBuilder.medSkjæringstidspunktOpptjening(saksbehandletAktiviteterFraFpsak.getSkjæringstidspunktOpptjening());
        saksbehandletAktiviteterFraFpsak.getBeregningAktiviteter().forEach(mapAktivitet(dtoBuilder, arbeidsgiverOpplysninger));
        return dtoBuilder.build();
    }

    private static Consumer<BeregningAktivitetEntitet> mapAktivitet(BeregningAktivitetAggregatRestDto.Builder dtoBuilder, Map<Arbeidsgiver, ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysninger) {
        return beregningAktivitet -> {
            BeregningAktivitetRestDto.Builder builder = BeregningAktivitetRestDto.builder();
            builder.medArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef()));
            builder.medArbeidsgiver(beregningAktivitet.getArbeidsgiver() == null ? null : mapArbeidsgiver(beregningAktivitet.getArbeidsgiver(), arbeidsgiverOpplysninger));
            builder.medOpptjeningAktivitetType(OpptjeningAktivitetType.fraKode(beregningAktivitet.getOpptjeningAktivitetType().getKode()));
            builder.medPeriode(mapDatoIntervall(beregningAktivitet.getPeriode()));
            dtoBuilder.leggTilAktivitet(builder.build());
        };
    }

    private static Intervall mapDatoIntervall(ÅpenDatoIntervallEntitet periode) {
        return periode.getTomDato() == null ? Intervall.fraOgMed(periode.getFomDato()) : Intervall.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato());
    }

    public static ArbeidsgiverMedNavn mapArbeidsgiver(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver arbeidsgiver, Map<Arbeidsgiver, ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysningerDtoList) {
        ArbeidsgiverMedNavn arbeidsgiverMedNavn = arbeidsgiver.getErVirksomhet() ? ArbeidsgiverMedNavn.virksomhet(arbeidsgiver.getOrgnr()) :
            ArbeidsgiverMedNavn.fra(new AktørId(arbeidsgiver.getAktørId().getId()));

        Arbeidsgiver ag = arbeidsgiver.getErVirksomhet() ? Arbeidsgiver.virksomhet(arbeidsgiver.getOrgnr()) : Arbeidsgiver.person(new AktørId(arbeidsgiver.getAktørId().getId()));
        if (arbeidsgiverOpplysningerDtoList.containsKey(ag)) {
            arbeidsgiverMedNavn.setNavn(arbeidsgiverOpplysningerDtoList.get(ag).getNavn());
        }
        return arbeidsgiverMedNavn;
    }


    public static BeregningAktivitetOverstyringerDto mapAktivitetOverstyring(BeregningAktivitetOverstyringerEntitet beregningAktivitetOverstyringerFraFpsak) {
        BeregningAktivitetOverstyringerDto.Builder dtoBuilder = BeregningAktivitetOverstyringerDto.builder();
        beregningAktivitetOverstyringerFraFpsak.getOverstyringer().forEach(overstyring -> {
            BeregningAktivitetOverstyringDto.Builder builder = BeregningAktivitetOverstyringDto.builder();
            builder.medArbeidsforholdRef(overstyring.getArbeidsforholdRef() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(overstyring.getArbeidsforholdRef()));
            overstyring.getArbeidsgiver().ifPresent(arbeidsgiver -> builder.medArbeidsgiver(IAYMapperTilKalkulus.mapArbeidsgiver(arbeidsgiver)));
            builder.medHandling(overstyring.getHandling() == null ? null : BeregningAktivitetHandlingType.fraKode(overstyring.getHandling().getKode()));
            builder.medOpptjeningAktivitetType(OpptjeningAktivitetType.fraKode(overstyring.getOpptjeningAktivitetType().getKode()));
            builder.medPeriode(mapDatoIntervall(overstyring.getPeriode()));
            dtoBuilder.leggTilOverstyring(builder.build());
        });
        return dtoBuilder.build();
    }

    public static BeregningsgrunnlagGrunnlagRestDto mapGrunnlag(BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagFraFpsak, Map<Arbeidsgiver, ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysninger, Collection<InntektsmeldingDto> inntektsmeldinger) {
        BeregningsgrunnlagGrunnlagRestDtoBuilder oppdatere = BeregningsgrunnlagGrunnlagRestDtoBuilder.oppdatere(Optional.empty());

        beregningsgrunnlagFraFpsak.getBeregningsgrunnlag().ifPresent(BeregningsgrunnlagRestDto -> oppdatere.medBeregningsgrunnlag(mapBeregningsgrunnlag(BeregningsgrunnlagRestDto, arbeidsgiverOpplysninger, inntektsmeldinger)));
        beregningsgrunnlagFraFpsak.getOverstyring().ifPresent(beregningAktivitetOverstyringerDto -> oppdatere.medOverstyring(mapAktivitetOverstyring(beregningAktivitetOverstyringerDto)));
        oppdatere.medRegisterAktiviteter(mapRegisterAktiviteter(beregningsgrunnlagFraFpsak.getRegisterAktiviteter(), arbeidsgiverOpplysninger));
        beregningsgrunnlagFraFpsak.getSaksbehandletAktiviteter().ifPresent(BeregningAktivitetAggregatRestDto -> oppdatere.medSaksbehandletAktiviteter(mapSaksbehandletAktivitet(BeregningAktivitetAggregatRestDto, arbeidsgiverOpplysninger)));
        beregningsgrunnlagFraFpsak.getRefusjonOverstyringer().ifPresent(beregningRefusjonOverstyringerDto -> oppdatere.medRefusjonOverstyring(mapRefusjonOverstyring(beregningRefusjonOverstyringerDto)));

        return oppdatere.build(BeregningsgrunnlagTilstand.fraKode(beregningsgrunnlagFraFpsak.getBeregningsgrunnlagTilstand().getKode()));
    }

    private static BeregningAktivitetAggregatRestDto mapRegisterAktiviteter(BeregningAktivitetAggregatEntitet registerAktiviteter, Map<Arbeidsgiver, ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysninger) {
        BeregningAktivitetAggregatRestDto.Builder builder = BeregningAktivitetAggregatRestDto.builder();
        builder.medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        registerAktiviteter.getBeregningAktiviteter().forEach(mapAktivitet(builder, arbeidsgiverOpplysninger));
        return builder.build();
    }


    public static SammenligningsgrunnlagRestDto mapSammenligningsgrunnlag(Sammenligningsgrunnlag fraFpsak) {
        SammenligningsgrunnlagRestDto.Builder builder = SammenligningsgrunnlagRestDto.builder();
        builder.medAvvikPromilleNy(fraFpsak.getAvvikPromilleNy());
        builder.medRapportertPrÅr(fraFpsak.getRapportertPrÅr());
        builder.medSammenligningsperiode(fraFpsak.getSammenligningsperiodeFom(), fraFpsak.getSammenligningsperiodeFom());
        return builder.build();
    }

    public static BeregningsgrunnlagAktivitetStatusRestDto.Builder mapAktivitetStatus(BeregningsgrunnlagAktivitetStatus fraFpsak) {
        BeregningsgrunnlagAktivitetStatusRestDto.Builder builder = new BeregningsgrunnlagAktivitetStatusRestDto.Builder();
        builder.medAktivitetStatus(AktivitetStatus.fraKode(fraFpsak.getAktivitetStatus().getKode()));
        builder.medHjemmel(fraFpsak.getHjemmel() == null ? null : Hjemmel.fraKode(fraFpsak.getHjemmel().getKode()));

        return builder;
    }

    public static BeregningsgrunnlagPeriodeRestDto.Builder mapBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode fraFpsak, Map<Arbeidsgiver, ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysninger, Collection<InntektsmeldingDto> inntektsmeldinger) {
        BeregningsgrunnlagPeriodeRestDto.Builder builder = new BeregningsgrunnlagPeriodeRestDto.Builder();

        //med
        builder.medAvkortetPrÅr(fraFpsak.getAvkortetPrÅr());
        builder.medBeregningsgrunnlagPeriode(fraFpsak.getBeregningsgrunnlagPeriodeFom(), fraFpsak.getBeregningsgrunnlagPeriodeTom());
        builder.medBruttoPrÅr(fraFpsak.getBruttoPrÅr());
        builder.medRedusertPrÅr(fraFpsak.getRedusertPrÅr());

        //legg til
        fraFpsak.getPeriodeÅrsaker().forEach(periodeÅrsak -> builder.leggTilPeriodeÅrsak(PeriodeÅrsak.fraKode(periodeÅrsak.getKode())));
        fraFpsak.getBeregningsgrunnlagPrStatusOgAndelList().forEach( statusOgAndel -> builder.leggTilBeregningsgrunnlagPrStatusOgAndel(mapStatusOgAndel(statusOgAndel, arbeidsgiverOpplysninger, inntektsmeldinger)));

        return builder;
    }

    public static SammenligningsgrunnlagPrStatusRestDto.Builder mapSammenligningsgrunnlagMedStatus(SammenligningsgrunnlagPrStatus fraFpsak) {
        SammenligningsgrunnlagPrStatusRestDto.Builder builder = new SammenligningsgrunnlagPrStatusRestDto.Builder();
        builder.medAvvikPromilleNy(fraFpsak.getAvvikPromilleNy());
        builder.medRapportertPrÅr(fraFpsak.getRapportertPrÅr());
        builder.medSammenligningsgrunnlagType(SammenligningsgrunnlagType.fraKode(fraFpsak.getSammenligningsgrunnlagType().getKode()));
        builder.medSammenligningsperiode(fraFpsak.getSammenligningsperiodeFom(), fraFpsak.getSammenligningsperiodeTom());

        return builder;
    }

    private static BeregningsgrunnlagPrStatusOgAndelRestDto.Builder mapStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel fraFpsak, Map<Arbeidsgiver, ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysninger, Collection<InntektsmeldingDto> inntektsmeldinger) {
        BeregningsgrunnlagPrStatusOgAndelRestDto.Builder builder = BeregningsgrunnlagPrStatusOgAndelRestDto.kopier()
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
            .medNyIArbeidslivet(fraFpsak.getNyIArbeidslivet())
            .medInntektskategori(fraFpsak.getInntektskategori() == null ? null : Inntektskategori.fraKode(fraFpsak.getInntektskategori().getKode()))
            .medLagtTilAvSaksbehandler(fraFpsak.getLagtTilAvSaksbehandler())
            .medOrginalDagsatsFraTilstøtendeYtelse(fraFpsak.getOrginalDagsatsFraTilstøtendeYtelse());

        if (fraFpsak.getAktivitetStatus().erArbeidstaker()) {
            BeregningsgrunnlagArbeidstakerAndelRestDto beregningsgrunnlagArbeidstakerAndelDto = BeregningsgrunnlagArbeidstakerAndelRestDto.builder()
                .medHarInntektsmelding(inntektsmeldinger.stream().anyMatch(im -> gjelderInntektsmeldingFor(fraFpsak, im.getArbeidsgiver(), im.getArbeidsforholdRef())))
                .medMottarYtelse(fraFpsak.mottarYtelse().orElse(null))
                .build();
            builder.medBeregningsgrunnlagArbeidstakerAndel(beregningsgrunnlagArbeidstakerAndelDto);
        }


        if (fraFpsak.getAktivitetStatus().erFrilanser() && (fraFpsak.mottarYtelse().isPresent() || fraFpsak.erNyoppstartet().isPresent())) {
            builder.medBeregningsgrunnlagFrilansAndel(BeregningsgrunnlagFrilansAndelRestDto.builder()
                .medMottarYtelse(fraFpsak.mottarYtelse().orElse(null))
                .medNyoppstartet(fraFpsak.erNyoppstartet().orElse(null))
                .build());
        }

        if (fraFpsak.getBeregningsperiodeFom() != null) {
            builder.medBeregningsperiode(fraFpsak.getBeregningsperiodeFom(), fraFpsak.getBeregningsperiodeTom());
        }

        if (fraFpsak.getPgiSnitt() != null) {
            builder.medPgi(fraFpsak.getPgiSnitt(), List.of(fraFpsak.getPgi1(), fraFpsak.getPgi2(), fraFpsak.getPgi3()));
        }

        fraFpsak.getBgAndelArbeidsforhold().ifPresent(bgAndelArbeidsforhold -> builder.medBGAndelArbeidsforhold(magBGAndelArbeidsforhold(bgAndelArbeidsforhold, arbeidsgiverOpplysninger)));
        return builder;
    }

    private static boolean gjelderInntektsmeldingFor(BeregningsgrunnlagPrStatusOgAndel fraFpsak, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRefDto arbeidsforholdRef) {
        Optional<BGAndelArbeidsforhold> bgAndelArbeidsforholdOpt = fraFpsak.getBgAndelArbeidsforhold();
        if (!Objects.equals(fraFpsak.getAktivitetStatus(), no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.ARBEIDSTAKER) || !bgAndelArbeidsforholdOpt.isPresent()) {
            return false;
        }
        if (!Objects.equals(fraFpsak.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver).map(IAYMapperTilKalkulus::mapArbeidsgiver), Optional.of(arbeidsgiver))) {
            return false;
        }
        if (fraFpsak.getArbeidsforholdRef().isEmpty() || !fraFpsak.getArbeidsforholdRef().get().gjelderForSpesifiktArbeidsforhold()) {
            boolean harPeriodeAndelForSammeArbeidsgiverMedReferanse = fraFpsak.getBeregningsgrunnlagPeriode().getBeregningsgrunnlagPrStatusOgAndelList()
                .stream()
                .filter(a -> a.getAktivitetStatus().erArbeidstaker())
                .filter(a -> a.getArbeidsgiver().isPresent() && a.getArbeidsgiver().get().equals(arbeidsgiver))
                .anyMatch(a -> a.getArbeidsforholdRef().isPresent() && a.getArbeidsforholdRef().get().gjelderForSpesifiktArbeidsforhold());

            if (harPeriodeAndelForSammeArbeidsgiverMedReferanse) {
                return false;
            }
        }
        return  bgAndelArbeidsforholdOpt.map(BGAndelArbeidsforhold::getArbeidsforholdRef)
            .map(IAYMapperTilKalkulus::mapArbeidsforholdRef).get().gjelderFor(arbeidsforholdRef);
    }

    private static BGAndelArbeidsforholdRestDto.Builder magBGAndelArbeidsforhold(BGAndelArbeidsforhold fraFpsak, Map<Arbeidsgiver, ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysninger) {
        BGAndelArbeidsforholdRestDto.Builder builder = BGAndelArbeidsforholdRestDto.builder();
        builder.medArbeidsforholdRef(IAYMapperTilKalkulus.mapArbeidsforholdRef(fraFpsak.getArbeidsforholdRef()));
        builder.medArbeidsgiver(mapArbeidsgiver(fraFpsak.getArbeidsgiver(), arbeidsgiverOpplysninger));
        builder.medArbeidsperiodeFom(fraFpsak.getArbeidsperiodeFom());
        builder.medLønnsendringIBeregningsperioden(fraFpsak.erLønnsendringIBeregningsperioden());
        builder.medTidsbegrensetArbeidsforhold(fraFpsak.getErTidsbegrensetArbeidsforhold());
        builder.medRefusjonskravPrÅr(fraFpsak.getRefusjonskravPrÅr());

        fraFpsak.getArbeidsperiodeTom().ifPresent(builder::medArbeidsperiodeTom);
        fraFpsak.getNaturalytelseBortfaltPrÅr().ifPresent(builder::medNaturalytelseBortfaltPrÅr);
        fraFpsak.getNaturalytelseTilkommetPrÅr().ifPresent(builder::medNaturalytelseTilkommetPrÅr);
        return builder;
    }

}
