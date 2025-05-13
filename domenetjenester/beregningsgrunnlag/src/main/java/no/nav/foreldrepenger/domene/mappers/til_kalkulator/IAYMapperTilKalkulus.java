package no.nav.foreldrepenger.domene.mappers.til_kalkulator;


import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.folketrygdloven.kalkulator.modell.iay.AktivitetsAvtaleDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdInformasjonDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdInformasjonDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdOverstyringDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.ArbeidsforholdReferanseDto;
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
import no.nav.folketrygdloven.kalkulator.modell.iay.VersjonTypeDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YrkesaktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YrkesaktivitetDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseAnvistDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.permisjon.PermisjonDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver;
import no.nav.folketrygdloven.kalkulator.modell.typer.EksternArbeidsforholdRef;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.Stillingsprosent;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.PermisjonsbeskrivelseType;
import no.nav.folketrygdloven.kalkulus.typer.AktørId;
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
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class IAYMapperTilKalkulus {

    private IAYMapperTilKalkulus() {
    }

    public static InternArbeidsforholdRefDto mapArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
        return InternArbeidsforholdRefDto.ref(arbeidsforholdRef.getReferanse());
    }

    public static EksternArbeidsforholdRef mapArbeidsforholdEksternRef(no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef arbeidsforholdRef) {
        return EksternArbeidsforholdRef.ref(arbeidsforholdRef.getReferanse());
    }

    public static Arbeidsgiver mapArbeidsgiver(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver.getErVirksomhet() ? Arbeidsgiver.virksomhet(arbeidsgiver.getOrgnr()) :
            Arbeidsgiver.fra(new AktørId(arbeidsgiver.getAktørId().getId()));
    }

    public static InntektArbeidYtelseGrunnlagDto mapGrunnlag(InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                             List<Inntektsmelding> inntektsmeldinger,
                                                             no.nav.foreldrepenger.domene.typer.AktørId aktørId) {
        var builder = InntektArbeidYtelseGrunnlagDtoBuilder.nytt();
        iayGrunnlag.getRegisterVersjon().ifPresent(aggregat -> builder.medData(mapAggregat(aggregat, VersjonTypeDto.REGISTER, aktørId, iayGrunnlag.getArbeidsforholdOverstyringer())));
        iayGrunnlag.getSaksbehandletVersjon().ifPresent(aggregat -> builder.medData(mapAggregat(aggregat, VersjonTypeDto.SAKSBEHANDLET, aktørId, iayGrunnlag.getArbeidsforholdOverstyringer())));
        if (!inntektsmeldinger.isEmpty()) {
            builder.setInntektsmeldinger(mapInntektsmelding(inntektsmeldinger, iayGrunnlag.getArbeidsforholdInformasjon()));
        }
        iayGrunnlag.getArbeidsforholdInformasjon().ifPresent(arbeidsforholdInformasjon -> builder.medInformasjon(mapArbeidsforholdInformasjon(arbeidsforholdInformasjon)));
        iayGrunnlag.getGjeldendeOppgittOpptjening().ifPresent(oppgittOpptjening -> builder.medOppgittOpptjening(mapOppgittOpptjening(oppgittOpptjening)));
        builder.medErAktivtGrunnlag(iayGrunnlag.isAktiv());
        return builder.build();
    }

    private static OppgittOpptjeningDtoBuilder mapOppgittOpptjening(OppgittOpptjening oppgittOpptjening) {
        var builder = OppgittOpptjeningDtoBuilder.ny();
        oppgittOpptjening.getFrilans().ifPresent(oppgittFrilans -> builder.leggTilFrilansOpplysninger(new OppgittFrilansDto(oppgittFrilans.getErNyoppstartet())));

        oppgittOpptjening.getAnnenAktivitet().forEach(oppgittAnnenAktivitet -> builder.leggTilAnnenAktivitet(new OppgittAnnenAktivitetDto(mapDatoIntervall(oppgittAnnenAktivitet.getPeriode()), KodeverkTilKalkulusMapper.mapArbeidtype(oppgittAnnenAktivitet.getArbeidType()))));
        oppgittOpptjening.getEgenNæring().forEach(oppgittEgenNæring -> builder.leggTilEgneNæring(mapEgenNæring(oppgittEgenNæring)));
        oppgittOpptjening.getOppgittArbeidsforhold().forEach(oppgittArbeidsforhold -> builder.leggTilOppgittArbeidsforhold(mapOppgittArbeidsforhold(oppgittArbeidsforhold)));

        return builder;
    }

    private static OppgittOpptjeningDtoBuilder.OppgittArbeidsforholdBuilder mapOppgittArbeidsforhold(OppgittArbeidsforhold oppgittArbeidsforhold) {
        return OppgittOpptjeningDtoBuilder.OppgittArbeidsforholdBuilder.ny()
            .medPeriode(mapDatoIntervall(oppgittArbeidsforhold.getPeriode()));
    }

    private static Intervall mapDatoIntervall(DatoIntervallEntitet periode) {
        return Intervall.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato());
    }

    private static OppgittOpptjeningDtoBuilder.EgenNæringBuilder mapEgenNæring(OppgittEgenNæring oppgittEgenNæring) {
        var egenNæringBuilder = OppgittOpptjeningDtoBuilder.EgenNæringBuilder.ny()
            .medPeriode(mapDatoIntervall(oppgittEgenNæring.getPeriode()))
            .medBruttoInntekt(mapTilBeløp(oppgittEgenNæring.getBruttoInntekt()))
            .medEndringDato(oppgittEgenNæring.getEndringDato())
            .medNyIArbeidslivet(oppgittEgenNæring.getNyIArbeidslivet())
            .medVirksomhetType(KodeverkTilKalkulusMapper.mapVirksomhetstype(oppgittEgenNæring.getVirksomhetType()))
            .medVarigEndring(oppgittEgenNæring.getVarigEndring())
            .medBegrunnelse(oppgittEgenNæring.getBegrunnelse())
            .medNyoppstartet(oppgittEgenNæring.getNyoppstartet());
        if (oppgittEgenNæring.getOrgnr() != null) {
            egenNæringBuilder.medVirksomhet(oppgittEgenNæring.getOrgnr());
        }
        return egenNæringBuilder;
    }

    private static ArbeidsforholdInformasjonDto mapArbeidsforholdInformasjon(ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        var builder = ArbeidsforholdInformasjonDtoBuilder.builder(Optional.empty());
        arbeidsforholdInformasjon.getArbeidsforholdReferanser().forEach(arbeidsforholdReferanse -> builder.leggTilNyReferanse(mapRefDto(arbeidsforholdReferanse)));
        arbeidsforholdInformasjon.getOverstyringer().forEach(arbeidsforholdOverstyring -> builder.leggTil(mapOverstyringerDto(arbeidsforholdOverstyring)));
        return builder.build();
    }

    private static ArbeidsforholdOverstyringDtoBuilder mapOverstyringerDto(ArbeidsforholdOverstyring arbeidsforholdOverstyring) {
        var builder = ArbeidsforholdOverstyringDtoBuilder.oppdatere(Optional.empty());
        arbeidsforholdOverstyring.getArbeidsforholdOverstyrtePerioder().forEach(arbeidsforholdOverstyrtePerioder -> builder.leggTilOverstyrtPeriode(arbeidsforholdOverstyrtePerioder.getOverstyrtePeriode().getFomDato(), arbeidsforholdOverstyrtePerioder.getOverstyrtePeriode().getTomDato()));
        builder.medHandling(KodeverkTilKalkulusMapper.mapArbeidsforholdHandling(arbeidsforholdOverstyring.getHandling()));
        builder.medArbeidsgiver(mapArbeidsgiver(arbeidsforholdOverstyring.getArbeidsgiver()));
        builder.medAngittStillingsprosent(arbeidsforholdOverstyring.getStillingsprosent() == null ? null : new Stillingsprosent(arbeidsforholdOverstyring.getStillingsprosent().getVerdi()));
        builder.medArbeidsforholdRef(arbeidsforholdOverstyring.getArbeidsforholdRef() == null ? null : mapArbeidsforholdRef(arbeidsforholdOverstyring.getArbeidsforholdRef()));
        builder.medNyArbeidsforholdRef(arbeidsforholdOverstyring.getNyArbeidsforholdRef() == null ? null : mapArbeidsforholdRef(arbeidsforholdOverstyring.getNyArbeidsforholdRef()));

        return builder;
    }

    private static ArbeidsforholdReferanseDto mapRefDto(ArbeidsforholdReferanse arbeidsforholdReferanse) {
        return new ArbeidsforholdReferanseDto(mapArbeidsgiver(arbeidsforholdReferanse.getArbeidsgiver()),
            mapArbeidsforholdRef(arbeidsforholdReferanse.getInternReferanse()),
            mapArbeidsforholdEksternRef(arbeidsforholdReferanse.getEksternReferanse()));
    }

    public static InntektsmeldingAggregatDto mapInntektsmelding(List<Inntektsmelding> inntektsmeldinger,
                                                                Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        var builder = InntektsmeldingAggregatDto.InntektsmeldingAggregatDtoBuilder.ny();
        inntektsmeldinger.forEach(inntektsmelding -> builder.leggTil(mapInntektsmeldingDto(inntektsmelding)));
        arbeidsforholdInformasjon.ifPresent(info -> builder.medArbeidsforholdInformasjonDto(mapArbeidsforholdInformasjon(info)));
        return builder.build();
    }

    public static InntektsmeldingDto mapInntektsmeldingDto(Inntektsmelding inntektsmelding) {
        var builder = InntektsmeldingDtoBuilder.builder();
        builder.medArbeidsgiver(mapArbeidsgiver(inntektsmelding.getArbeidsgiver()));
        builder.medArbeidsforholdId(mapArbeidsforholdRef(inntektsmelding.getArbeidsforholdRef()));
        builder.medRefusjon(mapTilBeløp(inntektsmelding.getRefusjonBeløpPerMnd()), inntektsmelding.getRefusjonOpphører());
        builder.medBeløp(mapTilBeløp(inntektsmelding.getInntektBeløp()));
        inntektsmelding.getNaturalYtelser().stream().map(IAYMapperTilKalkulus::mapNaturalYtelse).forEach(builder::leggTil);
        inntektsmelding.getStartDatoPermisjon().ifPresent(builder::medStartDatoPermisjon);
        inntektsmelding.getEndringerRefusjon().forEach(refusjon -> builder.leggTil(mapRefusjon(refusjon)));
        return builder.build(true);
    }

    private static NaturalYtelseDto mapNaturalYtelse(NaturalYtelse naturalYtelse) {
        return new NaturalYtelseDto(naturalYtelse.getPeriode().getFomDato(), naturalYtelse.getPeriode().getTomDato(), mapTilBeløp(naturalYtelse.getBeloepPerMnd()), KodeverkTilKalkulusMapper.mapNaturalytelsetype(naturalYtelse.getType()));
    }

    private static RefusjonDto mapRefusjon(Refusjon refusjon) {
        return new RefusjonDto(mapTilBeløp(refusjon.getRefusjonsbeløp()), refusjon.getFom());
    }

    private static AktivitetsAvtaleDtoBuilder mapAktivitetsAvtale(AktivitetsAvtale aktivitetsAvtale) {
        return AktivitetsAvtaleDtoBuilder.ny()
            .medPeriode(Intervall.fraOgMedTilOgMed(aktivitetsAvtale.getPeriode().getFomDato(), aktivitetsAvtale.getPeriode().getTomDato()))
            .medSisteLønnsendringsdato(aktivitetsAvtale.getSisteLønnsendringsdato())
            .medErAnsettelsesPeriode(aktivitetsAvtale.erAnsettelsesPeriode());
    }

    private static YrkesaktivitetDto mapYrkesaktivitet(Yrkesaktivitet yrkesaktivitet, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        finnBekreftetPermisjon(yrkesaktivitet, arbeidsforholdOverstyringer);
        var dtoBuilder = YrkesaktivitetDtoBuilder.oppdatere(Optional.empty());
        yrkesaktivitet.getAlleAktivitetsAvtaler().forEach(aktivitetsAvtale -> dtoBuilder.leggTilAktivitetsAvtale(mapAktivitetsAvtale(aktivitetsAvtale)));
        dtoBuilder.medArbeidsforholdId(mapArbeidsforholdRef(yrkesaktivitet.getArbeidsforholdRef()));
        dtoBuilder.medArbeidsgiver(yrkesaktivitet.getArbeidsgiver() == null ? null : mapArbeidsgiver(yrkesaktivitet.getArbeidsgiver()));
        dtoBuilder.medArbeidType(KodeverkTilKalkulusMapper.mapArbeidtype(yrkesaktivitet.getArbeidType()));
        yrkesaktivitet.getPermisjon().stream()
            .filter(perm -> erRelevantForBeregning(perm, finnBekreftetPermisjon(yrkesaktivitet, arbeidsforholdOverstyringer)))
            .forEach(perm -> dtoBuilder.leggTilPermisjon(mapPermisjon(perm)));
        return dtoBuilder.build();
    }

    private static boolean erRelevantForBeregning(Permisjon perm, Optional<BekreftetPermisjon> bekreftetPermisjon) {
        if (perm.getPermisjonsbeskrivelseType() == null || !perm.getPermisjonsbeskrivelseType().erRelevantForBeregningEllerArbeidsforhold()
            || !erFullPermisjon(perm)) {
            return false;
        }
        return bekreftetPermisjon
            .map(b -> Objects.equals(b.getPeriode(), perm.getPeriode()) && BekreftetPermisjonStatus.BRUK_PERMISJON.equals(b.getStatus()))
            .orElse(true);
    }

    private static boolean erFullPermisjon(Permisjon perm) {
        return perm.getProsentsats() != null && perm.getProsentsats().getVerdi().compareTo(BigDecimal.valueOf(100)) >= 0;
    }

    private static Optional<BekreftetPermisjon> finnBekreftetPermisjon(Yrkesaktivitet yrkesaktivitet, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        return arbeidsforholdOverstyringer.stream()
            .filter(os -> Objects.equals(os.getArbeidsgiver(), yrkesaktivitet.getArbeidsgiver())
                && os.getArbeidsforholdRef().gjelderFor(yrkesaktivitet.getArbeidsforholdRef()))
            .map(ArbeidsforholdOverstyring::getBekreftetPermisjon)
            .flatMap(Optional::stream)
            .findFirst();
    }

    private static PermisjonDtoBuilder mapPermisjon(Permisjon perm) {
        return PermisjonDtoBuilder.ny()
            .medProsentsats(mapTilProsent(perm.getProsentsats()))
            .medPeriode(mapDatoIntervall(perm.getPeriode()))
            .medPermisjonsbeskrivelseType(mapPermisjontype(perm.getPermisjonsbeskrivelseType()));
    }

    private static PermisjonsbeskrivelseType mapPermisjontype(no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType permisjonsbeskrivelseType) {
            return switch (permisjonsbeskrivelseType) {
                case UDEFINERT -> PermisjonsbeskrivelseType.UDEFINERT;
                case PERMISJON -> PermisjonsbeskrivelseType.PERMISJON;
                case UTDANNINGSPERMISJON -> PermisjonsbeskrivelseType.UTDANNINGSPERMISJON;
                case UTDANNINGSPERMISJON_LOVFESTET -> PermisjonsbeskrivelseType.UTDANNINGSPERMISJON_LOVFESTET;
                case UTDANNINGSPERMISJON_IKKE_LOVFESTET -> PermisjonsbeskrivelseType.UTDANNINGSPERMISJON_IKKE_LOVFESTET;
                case VELFERDSPERMISJON -> PermisjonsbeskrivelseType.VELFERDSPERMISJON;
                case ANNEN_PERMISJON_LOVFESTET -> PermisjonsbeskrivelseType.ANNEN_PERMISJON_LOVFESTET;
                case ANNEN_PERMISJON_IKKE_LOVFESTET -> PermisjonsbeskrivelseType.ANNEN_PERMISJON_IKKE_LOVFESTET;
                case PERMISJON_MED_FORELDREPENGER -> PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER;
                case PERMITTERING -> PermisjonsbeskrivelseType.PERMITTERING;
                case PERMISJON_VED_MILITÆRTJENESTE -> PermisjonsbeskrivelseType.PERMISJON_VED_MILITÆRTJENESTE;
            };
    }

    private static InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder mapAktørInntekt(AktørInntekt aktørInntekt){
        var builder = InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder.oppdatere(Optional.empty());
        aktørInntekt.getInntekt().forEach(inntekt -> builder.leggTilInntekt(mapInntekt(inntekt)));
        return builder;
    }

    private static InntektDtoBuilder mapInntekt(Inntekt inntekt) {
        var builder = InntektDtoBuilder.oppdatere(Optional.empty());
        inntekt.getAlleInntektsposter().forEach(inntektspost -> builder.leggTilInntektspost(mapInntektspost(inntektspost)));
        builder.medArbeidsgiver(inntekt.getArbeidsgiver() == null ? null : mapArbeidsgiver(inntekt.getArbeidsgiver()));
        builder.medInntektsKilde(KodeverkTilKalkulusMapper.mapInntektskilde(inntekt.getInntektsKilde()));
        return builder;
    }

    private static InntektspostDtoBuilder mapInntektspost(Inntektspost inntektspost) {
        var builder = InntektspostDtoBuilder.ny();
        builder.medBeløp(mapTilBeløp(inntektspost.getBeløp()));
        builder.medInntektspostType(KodeverkTilKalkulusMapper.mapInntektspostType(inntektspost.getInntektspostType()));
        builder.medPeriode(inntektspost.getPeriode().getFomDato(), inntektspost.getPeriode().getTomDato());
        builder.medSkatteOgAvgiftsregelType(inntektspost.getSkatteOgAvgiftsregelType() == null ? null : KodeverkTilKalkulusMapper.mapSkatteOgAvgitsregelType(inntektspost.getSkatteOgAvgiftsregelType()));
        builder.medInntektYtelse(inntektspost.getInntektYtelseType() == null ? null : KodeverkTilKalkulusMapper.mapInntektytelseType(inntektspost.getInntektYtelseType()));

        return builder;
    }


    private static InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder mapAktørArbeid(AktørArbeid aktørArbeid,
                                                                                        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        var builder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty());
        aktørArbeid.hentAlleYrkesaktiviteter().forEach(yrkesaktivitet -> builder.leggTilYrkesaktivitet(mapYrkesaktivitet(yrkesaktivitet, arbeidsforholdOverstyringer)));
        return builder;
    }

    private static InntektArbeidYtelseAggregatBuilder mapAggregat(InntektArbeidYtelseAggregat aggregat,
                                                                  VersjonTypeDto versjonTypeDto,
                                                                  no.nav.foreldrepenger.domene.typer.AktørId aktørId,
                                                                  List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), versjonTypeDto);
        aggregat.getAktørArbeid()
            .stream()
            .filter(aa -> aa.getAktørId().equals(aktørId))
            .findFirst()
            .ifPresent(aktørArbeid -> builder.leggTilAktørArbeid(mapAktørArbeid(aktørArbeid, arbeidsforholdOverstyringer)));
        aggregat.getAktørInntekt()
            .stream()
            .filter(ai -> ai.getAktørId().equals(aktørId))
            .findFirst()
            .ifPresent(aktørInntekt -> builder.leggTilAktørInntekt(mapAktørInntekt(aktørInntekt)));
        aggregat.getAktørYtelse()
            .stream()
            .filter(ay -> ay.getAktørId().equals(aktørId))
            .findFirst()
            .ifPresent(aktørYtelse -> builder.leggTilAktørYtelse(mapAktørYtelse(aktørYtelse)));
        return builder;

    }

    private static InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder mapAktørYtelse(AktørYtelse aktørYtelse) {
        var builder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty());
        aktørYtelse.getAlleYtelser().forEach(ytelse -> builder.leggTilYtelse(mapYtelse(ytelse)));
        return builder;

    }

    private static YtelseDtoBuilder mapYtelse(Ytelse ytelse) {
        var builder = YtelseDtoBuilder.ny();
        ytelse.getYtelseAnvist().forEach(ytelseAnvist -> builder.leggTilYtelseAnvist(mapYtelseAnvist(ytelseAnvist)));
        ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getVedtaksDagsats).map(Beløp::getVerdi).ifPresent(b -> builder.medVedtaksDagsats(mapTilBeløp(b)));
        builder.medPeriode(mapDatoIntervall(ytelse.getPeriode()));
        builder.medYtelseKilde(KodeverkTilKalkulusMapper.mapYtelseKilde(ytelse.getKilde()));
        builder.medYtelseType(KodeverkTilKalkulusMapper.mapYtelsetype(ytelse.getRelatertYtelseType()));
        return builder;
    }

    private static YtelseAnvistDto mapYtelseAnvist(YtelseAnvist ytelseAnvist) {
        var builder = YtelseAnvistDtoBuilder.ny();
        builder.medAnvistPeriode(Intervall.fraOgMedTilOgMed(ytelseAnvist.getAnvistFOM(), ytelseAnvist.getAnvistTOM()));
        ytelseAnvist.getBeløp().ifPresent(beløp -> builder.medBeløp(mapTilBeløp(beløp.getVerdi())));
        ytelseAnvist.getDagsats().ifPresent(dagsats -> builder.medDagsats(mapTilBeløp(dagsats.getVerdi())));
        ytelseAnvist.getUtbetalingsgradProsent().ifPresent(stillingsprosent -> builder.medUtbetalingsgradProsent(mapTilProsent(stillingsprosent)));
        return builder.build();
    }

    private static Stillingsprosent mapTilProsent(no.nav.foreldrepenger.domene.typer.Stillingsprosent prosent) {
        return prosent == null ? null : Stillingsprosent.fra(prosent.getVerdi());
    }

    private static no.nav.folketrygdloven.kalkulator.modell.typer.Beløp mapTilBeløp(BigDecimal verdi) {
        return no.nav.folketrygdloven.kalkulator.modell.typer.Beløp.fra(verdi);
    }

    private static no.nav.folketrygdloven.kalkulator.modell.typer.Beløp mapTilBeløp(Beløp beløp) {
        return beløp == null ? null : no.nav.folketrygdloven.kalkulator.modell.typer.Beløp.fra(beløp.getVerdi());
    }

}
