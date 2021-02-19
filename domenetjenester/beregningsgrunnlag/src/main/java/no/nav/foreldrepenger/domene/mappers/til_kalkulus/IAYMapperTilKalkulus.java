package no.nav.foreldrepenger.domene.mappers.til_kalkulus;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsgiverOpplysningerDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.AktivitetsAvtaleDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdInformasjonDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdInformasjonDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdOverstyringDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdReferanseDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.BekreftetPermisjonDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseAggregatBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektspostDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.NaturalYtelseDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.OppgittAnnenAktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.OppgittFrilansDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.OppgittOpptjeningDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.RefusjonDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.RefusjonskravDatoDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.VersjonTypeDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YrkesaktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YrkesaktivitetDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseAnvistDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseDtoBuilder;
import no.nav.folketrygdloven.kalkulus.typer.AktørId;
import no.nav.folketrygdloven.kalkulator.modell.typer.EksternArbeidsforholdRef;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.Stillingsprosent;
import no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver;
import no.nav.folketrygdloven.kalkulus.kodeverk.TemaUnderkategori;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidsforholdHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.BekreftetPermisjonStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NaturalYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NæringsinntektType;
import no.nav.folketrygdloven.kalkulus.kodeverk.OffentligYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.PensjonTrygdType;
import no.nav.folketrygdloven.kalkulus.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.AktørInntekt;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.RefusjonskravDato;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.YtelseType;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class IAYMapperTilKalkulus {

    public static InternArbeidsforholdRefDto mapArbeidsforholdRef(no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef arbeidsforholdRef) {
        return InternArbeidsforholdRefDto.ref(arbeidsforholdRef.getReferanse());
    }

    public static EksternArbeidsforholdRef mapArbeidsforholdEksternRef(no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef arbeidsforholdRef) {
        return EksternArbeidsforholdRef.ref(arbeidsforholdRef.getReferanse());
    }

    public static Arbeidsgiver mapArbeidsgiver(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver.getErVirksomhet() ? Arbeidsgiver.virksomhet(arbeidsgiver.getOrgnr()) :
            Arbeidsgiver.fra(new AktørId(arbeidsgiver.getAktørId().getId()));
    }

    public static InntektArbeidYtelseGrunnlagDto mapGrunnlag(InntektArbeidYtelseGrunnlag iayGrunnlag, no.nav.foreldrepenger.domene.typer.AktørId aktørId) {
        InntektArbeidYtelseGrunnlagDtoBuilder builder = InntektArbeidYtelseGrunnlagDtoBuilder.nytt();
        iayGrunnlag.getRegisterVersjon().ifPresent(aggregat -> builder.medData(mapAggregat(aggregat, VersjonTypeDto.REGISTER, aktørId)));
        iayGrunnlag.getSaksbehandletVersjon().ifPresent(aggregat -> builder.medData(mapAggregat(aggregat, VersjonTypeDto.SAKSBEHANDLET, aktørId)));
        iayGrunnlag.getInntektsmeldinger().ifPresent(aggregat -> builder.setInntektsmeldinger(mapInntektsmelding(aggregat, iayGrunnlag.getArbeidsforholdInformasjon())));
        iayGrunnlag.getArbeidsforholdInformasjon().ifPresent(arbeidsforholdInformasjon -> builder.medInformasjon(mapArbeidsforholdInformasjon(arbeidsforholdInformasjon)));
        iayGrunnlag.getOppgittOpptjening().ifPresent(oppgittOpptjening -> builder.medOppgittOpptjening(mapOppgittOpptjening(oppgittOpptjening)));
        builder.medErAktivtGrunnlag(iayGrunnlag.isAktiv());
        return builder.build();
    }

    public static List<ArbeidsgiverOpplysningerDto> mapArbeidsforholdOpplysninger(Map<no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver, ArbeidsgiverOpplysninger> arbeidsgiverOpplysninger, List<ArbeidsforholdOverstyring> overstyringer) {
        List<ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysningerDtos = new ArrayList<>();
        arbeidsgiverOpplysninger.forEach((key, value) -> arbeidsgiverOpplysningerDtos.add(mapOpplysning(key, value)));
        overstyringer
            .stream()
            .filter(overstyring -> overstyring.getArbeidsgiverNavn() != null) // Vi er kun interessert i overstyringer der SBH har endret navn på arbeidsgiver
            .forEach(arbeidsforhold -> arbeidsgiverOpplysningerDtos.add(new ArbeidsgiverOpplysningerDto(arbeidsforhold.getArbeidsgiver().getIdentifikator(), arbeidsforhold.getArbeidsgiverNavn())));
        return arbeidsgiverOpplysningerDtos;

    }

    public  static ArbeidsgiverOpplysningerDto mapOpplysning(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver key, ArbeidsgiverOpplysninger arbeidsgiverOpplysninger) {
        return new ArbeidsgiverOpplysningerDto(key.getIdentifikator(), arbeidsgiverOpplysninger.getNavn(), arbeidsgiverOpplysninger.getFødselsdato());
    }

    private static OppgittOpptjeningDtoBuilder mapOppgittOpptjening(OppgittOpptjening oppgittOpptjening) {
        OppgittOpptjeningDtoBuilder builder = OppgittOpptjeningDtoBuilder.ny();
        oppgittOpptjening.getFrilans().ifPresent(oppgittFrilans -> builder.leggTilFrilansOpplysninger(new OppgittFrilansDto(oppgittFrilans.getErNyoppstartet())));

        oppgittOpptjening.getAnnenAktivitet().forEach(oppgittAnnenAktivitet -> builder.leggTilAnnenAktivitet(new OppgittAnnenAktivitetDto(mapDatoIntervall(oppgittAnnenAktivitet.getPeriode()), ArbeidType.fraKode(oppgittAnnenAktivitet.getArbeidType().getKode()))));
        oppgittOpptjening.getEgenNæring().forEach(oppgittEgenNæring -> builder.leggTilEgneNæring(mapEgenNæring(oppgittEgenNæring)));
        oppgittOpptjening.getOppgittArbeidsforhold().forEach(oppgittArbeidsforhold -> builder.leggTilOppgittArbeidsforhold(mapOppgittArbeidsforhold(oppgittArbeidsforhold)));

        return builder;
    }

    private static OppgittOpptjeningDtoBuilder.OppgittArbeidsforholdBuilder mapOppgittArbeidsforhold(OppgittArbeidsforhold oppgittArbeidsforhold) {
        return OppgittOpptjeningDtoBuilder.OppgittArbeidsforholdBuilder.ny()
            .medPeriode(mapDatoIntervall(oppgittArbeidsforhold.getPeriode()));
    }

    private static Intervall mapDatoIntervall(no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet periode) {
        return Intervall.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato());
    }

    private static OppgittOpptjeningDtoBuilder.EgenNæringBuilder mapEgenNæring(OppgittEgenNæring oppgittEgenNæring) {
        OppgittOpptjeningDtoBuilder.EgenNæringBuilder egenNæringBuilder = OppgittOpptjeningDtoBuilder.EgenNæringBuilder.ny()
            .medPeriode(mapDatoIntervall(oppgittEgenNæring.getPeriode()))
            .medBruttoInntekt(oppgittEgenNæring.getBruttoInntekt())
            .medEndringDato(oppgittEgenNæring.getEndringDato())
            .medNyIArbeidslivet(oppgittEgenNæring.getNyIArbeidslivet())
            .medVirksomhetType(VirksomhetType.fraKode(oppgittEgenNæring.getVirksomhetType().getKode()))
            .medVarigEndring(oppgittEgenNæring.getVarigEndring())
            .medBegrunnelse(oppgittEgenNæring.getBegrunnelse())
            .medNyoppstartet(oppgittEgenNæring.getNyoppstartet());
        if (oppgittEgenNæring.getOrgnr() != null) {
            egenNæringBuilder.medVirksomhet(oppgittEgenNæring.getOrgnr());
        }
        return egenNæringBuilder;
    }

    private static ArbeidsforholdInformasjonDto mapArbeidsforholdInformasjon(ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        ArbeidsforholdInformasjonDtoBuilder builder = ArbeidsforholdInformasjonDtoBuilder.builder(Optional.empty());
        arbeidsforholdInformasjon.getArbeidsforholdReferanser().forEach(arbeidsforholdReferanse -> builder.leggTilNyReferanse(mapRefDto(arbeidsforholdReferanse)));
        arbeidsforholdInformasjon.getOverstyringer().forEach(arbeidsforholdOverstyring -> builder.leggTil(mapOverstyringerDto(arbeidsforholdOverstyring)));
        return builder.build();
    }

    private static ArbeidsforholdOverstyringDtoBuilder mapOverstyringerDto(ArbeidsforholdOverstyring arbeidsforholdOverstyring) {
        ArbeidsforholdOverstyringDtoBuilder builder = ArbeidsforholdOverstyringDtoBuilder.oppdatere(Optional.empty());
        arbeidsforholdOverstyring.getArbeidsforholdOverstyrtePerioder().forEach(arbeidsforholdOverstyrtePerioder -> builder.leggTilOverstyrtPeriode(arbeidsforholdOverstyrtePerioder.getOverstyrtePeriode().getFomDato(), arbeidsforholdOverstyrtePerioder.getOverstyrtePeriode().getTomDato()));
        arbeidsforholdOverstyring.getBekreftetPermisjon().ifPresent(bekreftetPermisjon -> builder.medBekreftetPermisjon(mapBekreftetPermisjonDto(bekreftetPermisjon)));
        builder.medHandling(ArbeidsforholdHandlingType.fraKode(arbeidsforholdOverstyring.getHandling().getKode()));
        builder.medArbeidsgiver(mapArbeidsgiver(arbeidsforholdOverstyring.getArbeidsgiver()));
        builder.medAngittArbeidsgiverNavn(arbeidsforholdOverstyring.getArbeidsgiverNavn());
        builder.medAngittStillingsprosent(arbeidsforholdOverstyring.getStillingsprosent() == null ? null : new Stillingsprosent(arbeidsforholdOverstyring.getStillingsprosent().getVerdi()));
        builder.medArbeidsforholdRef(arbeidsforholdOverstyring.getArbeidsforholdRef() == null ? null : mapArbeidsforholdRef(arbeidsforholdOverstyring.getArbeidsforholdRef()));
        builder.medNyArbeidsforholdRef(arbeidsforholdOverstyring.getNyArbeidsforholdRef() == null ? null : mapArbeidsforholdRef(arbeidsforholdOverstyring.getNyArbeidsforholdRef()));
        builder.medBeskrivelse(arbeidsforholdOverstyring.getBegrunnelse());

        return builder;
    }

    private static BekreftetPermisjonDto mapBekreftetPermisjonDto(BekreftetPermisjon bekreftetPermisjon) {
        return new BekreftetPermisjonDto(bekreftetPermisjon.getPeriode().getFomDato(), bekreftetPermisjon.getPeriode().getTomDato(), BekreftetPermisjonStatus.fraKode(bekreftetPermisjon.getStatus().getKode()));
    }

    private static ArbeidsforholdReferanseDto mapRefDto(ArbeidsforholdReferanse arbeidsforholdReferanse) {
        return new ArbeidsforholdReferanseDto(mapArbeidsgiver(arbeidsforholdReferanse.getArbeidsgiver()),
            mapArbeidsforholdRef(arbeidsforholdReferanse.getInternReferanse()),
            mapArbeidsforholdEksternRef(arbeidsforholdReferanse.getEksternReferanse()));
    }

    public static InntektsmeldingAggregatDto mapInntektsmelding(InntektsmeldingAggregat aggregat, Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        InntektsmeldingAggregatDto.InntektsmeldingAggregatDtoBuilder builder = InntektsmeldingAggregatDto.InntektsmeldingAggregatDtoBuilder.ny();
        aggregat.getAlleInntektsmeldinger().forEach(inntektsmelding -> builder.leggTil(mapInntektsmeldingDto(inntektsmelding)));
        arbeidsforholdInformasjon.ifPresent(info -> builder.medArbeidsforholdInformasjonDto(mapArbeidsforholdInformasjon(info)));
        return builder.build();
    }

    public static InntektsmeldingDto mapInntektsmeldingDto(Inntektsmelding inntektsmelding) {
        InntektsmeldingDtoBuilder builder = InntektsmeldingDtoBuilder.builder();
        builder.medArbeidsgiver(mapArbeidsgiver(inntektsmelding.getArbeidsgiver()));
        builder.medArbeidsforholdId(mapArbeidsforholdRef(inntektsmelding.getArbeidsforholdRef()));
        builder.medRefusjon(inntektsmelding.getRefusjonBeløpPerMnd() == null ? null : inntektsmelding.getRefusjonBeløpPerMnd().getVerdi(), inntektsmelding.getRefusjonOpphører());
        builder.medBeløp(inntektsmelding.getInntektBeløp().getVerdi());
        inntektsmelding.getNaturalYtelser().stream().map(IAYMapperTilKalkulus::mapNaturalYtelse).forEach(builder::leggTil);
        inntektsmelding.getStartDatoPermisjon().ifPresent(builder::medStartDatoPermisjon);
        inntektsmelding.getEndringerRefusjon().forEach(refusjon -> builder.leggTil(mapRefusjon(refusjon)));
        return builder.build(true);
    }

    private static NaturalYtelseDto mapNaturalYtelse(NaturalYtelse naturalYtelse) {
        return new NaturalYtelseDto(naturalYtelse.getPeriode().getFomDato(), naturalYtelse.getPeriode().getTomDato(), naturalYtelse.getBeloepPerMnd().getVerdi(), NaturalYtelseType.fraKode(naturalYtelse.getType().getKode()));
    }

    private static RefusjonDto mapRefusjon(Refusjon refusjon) {
        return new RefusjonDto(refusjon.getRefusjonsbeløp().getVerdi(), refusjon.getFom());
    }

    private static AktivitetsAvtaleDtoBuilder mapAktivitetsAvtale(AktivitetsAvtale aktivitetsAvtale) {
        return AktivitetsAvtaleDtoBuilder.ny()
            .medPeriode(Intervall.fraOgMedTilOgMed(aktivitetsAvtale.getPeriode().getFomDato(), aktivitetsAvtale.getPeriode().getTomDato()))
            .medSisteLønnsendringsdato(aktivitetsAvtale.getSisteLønnsendringsdato())
            .medErAnsettelsesPeriode(aktivitetsAvtale.erAnsettelsesPeriode());
    }

    private static YrkesaktivitetDto mapYrkesaktivitet(Yrkesaktivitet yrkesaktivitet) {
        YrkesaktivitetDtoBuilder dtoBuilder = YrkesaktivitetDtoBuilder.oppdatere(Optional.empty());
        yrkesaktivitet.getAlleAktivitetsAvtaler().forEach(aktivitetsAvtale -> dtoBuilder.leggTilAktivitetsAvtale(mapAktivitetsAvtale(aktivitetsAvtale)));
        dtoBuilder.medArbeidsforholdId(mapArbeidsforholdRef(yrkesaktivitet.getArbeidsforholdRef()));
        dtoBuilder.medArbeidsgiver(yrkesaktivitet.getArbeidsgiver() == null ? null : mapArbeidsgiver(yrkesaktivitet.getArbeidsgiver()));
        dtoBuilder.medArbeidType(ArbeidType.fraKode(yrkesaktivitet.getArbeidType().getKode()));
        return dtoBuilder.build();
    }


    private static InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder mapAktørInntekt(AktørInntekt aktørInntekt){
        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder builder = InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder.oppdatere(Optional.empty());
        aktørInntekt.getInntekt().forEach(inntekt -> builder.leggTilInntekt(mapInntekt(inntekt)));
        return builder;
    }

    private static InntektDtoBuilder mapInntekt(Inntekt inntekt) {
        InntektDtoBuilder builder = InntektDtoBuilder.oppdatere(Optional.empty());
        inntekt.getAlleInntektsposter().forEach(inntektspost -> builder.leggTilInntektspost(mapInntektspost(inntektspost)));
        builder.medArbeidsgiver(inntekt.getArbeidsgiver() == null ? null : mapArbeidsgiver(inntekt.getArbeidsgiver()));
        builder.medInntektsKilde(InntektskildeType.fraKode(inntekt.getInntektsKilde().getKode()));
        return builder;
    }

    private static InntektspostDtoBuilder mapInntektspost(Inntektspost inntektspost) {
        InntektspostDtoBuilder builder = InntektspostDtoBuilder.ny();
        builder.medBeløp(inntektspost.getBeløp().getVerdi());
        builder.medInntektspostType(inntektspost.getInntektspostType().getKode());
        builder.medPeriode(inntektspost.getPeriode().getFomDato(), inntektspost.getPeriode().getTomDato());
        builder.medSkatteOgAvgiftsregelType(inntektspost.getSkatteOgAvgiftsregelType().getKode());
        builder.medYtelse(mapUtbetaltYtelseTypeTilGrunnlag(inntektspost.getYtelseType()));

        return builder;
    }

    private static InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder mapAktørArbeid(AktørArbeid aktørArbeid) {
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder builder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty());
        aktørArbeid.hentAlleYrkesaktiviteter().forEach(yrkesaktivitet -> builder.leggTilYrkesaktivitet(mapYrkesaktivitet(yrkesaktivitet)));
        return builder;
    }

    private static InntektArbeidYtelseAggregatBuilder mapAggregat(InntektArbeidYtelseAggregat aggregat, VersjonTypeDto versjonTypeDto, no.nav.foreldrepenger.domene.typer.AktørId aktørId) {
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), versjonTypeDto);
        aggregat.getAktørArbeid().stream().filter(aa -> aa.getAktørId().equals(aktørId)).findFirst().ifPresent(aktørArbeid -> builder.leggTilAktørArbeid(mapAktørArbeid(aktørArbeid)));
        aggregat.getAktørInntekt().stream().filter(ai -> ai.getAktørId().equals(aktørId)).findFirst().ifPresent((aktørInntekt -> builder.leggTilAktørInntekt(mapAktørInntekt(aktørInntekt))));
        aggregat.getAktørYtelse().stream().filter(ay -> ay.getAktørId().equals(aktørId)).findFirst().ifPresent((aktørYtelse -> builder.leggTilAktørYtelse(mapAktørYtelse(aktørYtelse))));
        return builder;

    }

    private static InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder mapAktørYtelse(AktørYtelse aktørYtelse) {
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder builder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty());
        aktørYtelse.getAlleYtelser().forEach(ytelse -> builder.leggTilYtelse(mapYtelse(ytelse)));
        return builder;

    }

    private static YtelseDtoBuilder mapYtelse(Ytelse ytelse) {
        YtelseDtoBuilder builder = YtelseDtoBuilder.oppdatere(Optional.empty());
        ytelse.getYtelseAnvist().forEach(ytelseAnvist -> builder.leggTilYtelseAnvist(mapYtelseAnvist(ytelseAnvist)));
        ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getVedtaksDagsats).map(Beløp::getVerdi).ifPresent(builder::medVedtaksDagsats);
        builder.medBehandlingsTema(TemaUnderkategori.fraKode(ytelse.getBehandlingsTema().getKode()));
        builder.medPeriode(mapDatoIntervall(ytelse.getPeriode()));
        builder.medYtelseType(FagsakYtelseType.fraKode(ytelse.getRelatertYtelseType().getKode()));
        return builder;
    }

    private static YtelseAnvistDto mapYtelseAnvist(YtelseAnvist ytelseAnvist) {
        YtelseAnvistDtoBuilder builder = YtelseAnvistDtoBuilder.ny();
        builder.medAnvistPeriode(Intervall.fraOgMedTilOgMed(ytelseAnvist.getAnvistFOM(), ytelseAnvist.getAnvistTOM()));
        ytelseAnvist.getBeløp().ifPresent(beløp -> builder.medBeløp(beløp.getVerdi()));
        ytelseAnvist.getDagsats().ifPresent(dagsats -> builder.medDagsats(dagsats.getVerdi()));
        ytelseAnvist.getUtbetalingsgradProsent().ifPresent(stillingsprosent -> builder.medUtbetalingsgradProsent(stillingsprosent.getVerdi()));
        return builder.build();
    }

    static no.nav.folketrygdloven.kalkulus.kodeverk.YtelseType mapUtbetaltYtelseTypeTilGrunnlag(YtelseType type) {
        if (type == null)
            return OffentligYtelseType.UDEFINERT;
        String kodeverk = type.getKodeverk();
        String kode = type.getKode();
        switch (kodeverk) {
            case no.nav.foreldrepenger.domene.iay.modell.kodeverk.OffentligYtelseType.KODEVERK:
                return OffentligYtelseType.fraKode(kode);
            case no.nav.foreldrepenger.domene.iay.modell.kodeverk.NæringsinntektType.KODEVERK:
                return NæringsinntektType.fraKode(kode);
            case no.nav.foreldrepenger.domene.iay.modell.kodeverk.PensjonTrygdType.KODEVERK:
                return PensjonTrygdType.fraKode(kode);
            default:
                throw new IllegalArgumentException("Ukjent UtbetaltYtelseType: " + type);
        }
    }

    public static List<RefusjonskravDatoDto> mapRefusjonskravDatoer(List<RefusjonskravDato> refusjonskravDatoer) {
        return refusjonskravDatoer.stream()
            .map(IAYMapperTilKalkulus::mapRefusjonskravDato)
            .collect(Collectors.toList());
    }

    private static RefusjonskravDatoDto mapRefusjonskravDato(RefusjonskravDato refusjonskravDato) {
        return new RefusjonskravDatoDto(
            mapArbeidsgiver(refusjonskravDato.getArbeidsgiver()),
            refusjonskravDato.getFørsteDagMedRefusjonskrav().orElse(null),
            refusjonskravDato.getFørsteInnsendingAvRefusjonskrav(),
            refusjonskravDato.harRefusjonFraStart()
        );
    }
}
