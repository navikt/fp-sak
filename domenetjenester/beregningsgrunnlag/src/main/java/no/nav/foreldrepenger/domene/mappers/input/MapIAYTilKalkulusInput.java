package no.nav.foreldrepenger.domene.mappers.input;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.EksternArbeidsforholdRef;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.iay.IayProsent;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.AktivitetsAvtaleDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdInformasjonDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdOverstyringDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdReferanseDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.PermisjonDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.YrkesaktivitetDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntekterDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingerDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.NaturalYtelseDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.RefusjonDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingsPostDto;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelserDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektspostType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NaturalYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.PermisjonsbeskrivelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.VirksomhetType;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittEgenNæringDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittOpptjeningDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.AktørInntekt;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.KodeverkTilKalkulusMapper;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class MapIAYTilKalkulusInput {

    private MapIAYTilKalkulusInput() {
        // Skjuler default konstruktør
    }

    public static InntektArbeidYtelseGrunnlagDto mapIAY(InntektArbeidYtelseGrunnlag grunnlag,
                                                 List<Inntektsmelding> inntektsmeldinger,
                                                 BehandlingReferanse referanse) {
        var inntektArbeidYtelseGrunnlagDto = new InntektArbeidYtelseGrunnlagDto();

        grunnlag.getAktørInntektFraRegister(referanse.aktørId())
            .ifPresent(aktørInntekt -> inntektArbeidYtelseGrunnlagDto.medInntekterDto(mapInntektDto(aktørInntekt)));

        grunnlag.getAktørArbeidFraRegister(referanse.aktørId())
            .ifPresent(aktørArbeid -> inntektArbeidYtelseGrunnlagDto.medArbeidDto(mapArbeidDto(aktørArbeid, grunnlag.getArbeidsforholdOverstyringer())));

        grunnlag.getAktørYtelseFraRegister(referanse.aktørId())
            .ifPresent(aktørYtelse -> inntektArbeidYtelseGrunnlagDto.medYtelserDto(mapAktørYtelse(aktørYtelse)));

        grunnlag.getGjeldendeOppgittOpptjening()
            .ifPresent(oo -> inntektArbeidYtelseGrunnlagDto.medOppgittOpptjeningDto(mapTilOppgittOpptjeningDto(oo)));

        inntektArbeidYtelseGrunnlagDto.medInntektsmeldingerDto(mapInntektsmeldingTilDto(inntektsmeldinger));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(grunnlag.getArbeidsforholdInformasjon().map(MapIAYTilKalkulusInput::mapTilArbeidsforholdInformasjonDto).orElse(null));

        return inntektArbeidYtelseGrunnlagDto;
    }

    private static ArbeidsforholdInformasjonDto mapTilArbeidsforholdInformasjonDto(ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        var overstyringer = arbeidsforholdInformasjon.getOverstyringer().stream()
            .map(arbeidsforholdOverstyring -> {
                List<Periode> perioder = Optional.ofNullable(arbeidsforholdOverstyring.getArbeidsforholdOverstyrtePerioder()).orElseGet(List::of)
                    .stream()
                    .filter(p -> p.getOverstyrtePeriode() != null)
                    .map(p -> new Periode(p.getOverstyrtePeriode().getFomDato(), p.getOverstyrtePeriode().getTomDato()))
                    .toList();
                return new ArbeidsforholdOverstyringDto(mapTilAktør(arbeidsforholdOverstyring.getArbeidsgiver()),
                    arbeidsforholdOverstyring.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold() ? new InternArbeidsforholdRefDto(arbeidsforholdOverstyring.getArbeidsforholdRef().getReferanse())
                        : null,
                    KodeverkTilKalkulusMapper.mapArbeidsforholdHandling(arbeidsforholdOverstyring.getHandling()),
                    arbeidsforholdOverstyring.getStillingsprosent() == null ? null : IayProsent.fra(arbeidsforholdOverstyring.getStillingsprosent().getVerdi()),
                    perioder);
            })
            .toList();

        var referanser = arbeidsforholdInformasjon.getArbeidsforholdReferanser().stream().map(ref -> new ArbeidsforholdReferanseDto(mapTilAktør(ref.getArbeidsgiver()),
            ref.getInternReferanse() == null ? null : new InternArbeidsforholdRefDto(ref.getInternReferanse().getReferanse()),
            ref.getEksternReferanse() == null ? null : new EksternArbeidsforholdRef(ref.getEksternReferanse().getReferanse()))).collect(Collectors.toSet());
        if (overstyringer.isEmpty() && referanser.isEmpty()) {
            return null;
        }
        return new ArbeidsforholdInformasjonDto(overstyringer, referanser);
    }

    private static InntektsmeldingerDto mapInntektsmeldingTilDto(List<Inntektsmelding> inntektsmeldinger) {
        var mappedeInntektsmeldinger = inntektsmeldinger.stream().map(inntektsmelding -> {
            Aktør aktør = mapTilAktør(inntektsmelding.getArbeidsgiver());
            var inntektBeløp = no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra((inntektsmelding.getInntektBeløp().getVerdi()));
            var naturalYtelseDtos = inntektsmelding.getNaturalYtelser()
                .stream()
                .map(naturalYtelse -> new NaturalYtelseDto(mapPeriode(naturalYtelse.getPeriode()),
                    no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(naturalYtelse.getBeloepPerMnd().getVerdi()),
                    NaturalYtelseType.fraKode(naturalYtelse.getType().getKode())))
                .toList();

            var refusjonDtos = inntektsmelding.getEndringerRefusjon()
                .stream()
                .map(refusjon -> new RefusjonDto(no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(refusjon.getRefusjonsbeløp().getVerdi()),
                    refusjon.getFom()))
                .toList();

            var internArbeidsforholdRefDto = inntektsmelding.getArbeidsforholdRef()
                .gjelderForSpesifiktArbeidsforhold() ? new InternArbeidsforholdRefDto(inntektsmelding.getArbeidsforholdRef().getReferanse()) : null;
            var startDato = inntektsmelding.getStartDatoPermisjon().isPresent() ? inntektsmelding.getStartDatoPermisjon().get() : null;
            var refusjonOpphører = inntektsmelding.getRefusjonOpphører();
            var refusjonBeløp = inntektsmelding.getRefusjonBeløpPerMnd() != null ? no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(
                inntektsmelding.getRefusjonBeløpPerMnd().getVerdi()) : null;

            return new InntektsmeldingDto(aktør, inntektBeløp, naturalYtelseDtos, refusjonDtos, internArbeidsforholdRefDto, startDato,
                refusjonOpphører, refusjonBeløp, null);
        }).toList();
        return mappedeInntektsmeldinger.isEmpty() ? null : new InntektsmeldingerDto(mappedeInntektsmeldinger);
    }

    private static YtelserDto mapAktørYtelse(AktørYtelse aktørYtelse) {
        return mapYtelseDto(aktørYtelse.getAlleYtelser());
    }

    private static YtelserDto mapYtelseDto(Collection<Ytelse> alleYtelser) {
        List<YtelseDto> ytelserDto = alleYtelser.stream().map(ytelse -> new YtelseDto(
                mapBeløp(ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getVedtaksDagsats)),
                mapYtelseAnvist(ytelse.getYtelseAnvist()),
                KodeverkTilKalkulusMapper.mapYtelsetype(ytelse.getRelatertYtelseType()),
                mapPeriode(ytelse.getPeriode()),
                KodeverkTilKalkulusMapper.mapYtelseKilde(ytelse.getKilde())))
            .toList();

        if (!ytelserDto.isEmpty()) {
            return new YtelserDto(ytelserDto);
        }
        return null;
    }

    private static no.nav.folketrygdloven.kalkulus.felles.v1.Beløp mapBeløp(Optional<Beløp> beløp) {
        return beløp.map(value -> no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra((value.getVerdi()))).orElse(null);
    }

    private static Set<no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto> mapYtelseAnvist(Collection<YtelseAnvist> ytelseAnvist) {
        return ytelseAnvist.stream().map(ya -> {
            var beløpDto = mapBeløp(ya.getBeløp());
            var dagsatsDto = mapBeløp(ya.getDagsats());
            var utbetalingsgrad = ya.getUtbetalingsgradProsent().isPresent() ? IayProsent.fra(ya.getUtbetalingsgradProsent().get().getVerdi()) : null;
            return new YtelseAnvistDto(new Periode(
                ya.getAnvistFOM(), ya.getAnvistTOM()),
                beløpDto,
                dagsatsDto,
                utbetalingsgrad,
                null); // TODO tfp-5742 trenger vi anviste andeler?
        }).collect(Collectors.toSet());
    }

    private static ArbeidDto mapArbeidDto(AktørArbeid aktørArbeid, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        List<YrkesaktivitetDto> yrkesaktivitetDtoer = aktørArbeid.hentAlleYrkesaktiviteter().stream().map(ya -> mapYrkesaktivitetTilDto(ya, arbeidsforholdOverstyringer)).toList();
        if (!yrkesaktivitetDtoer.isEmpty()) {
            return new ArbeidDto(yrkesaktivitetDtoer);
        }
        return null;
    }

    private static YrkesaktivitetDto mapYrkesaktivitetTilDto(Yrkesaktivitet yrkesaktivitet, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        List<AktivitetsAvtaleDto> aktivitetsAvtaleDtos = yrkesaktivitet.getAlleAktivitetsAvtaler().stream().map(aktivitetsAvtale -> new AktivitetsAvtaleDto(mapPeriode(aktivitetsAvtale.getPeriode()),
            aktivitetsAvtale.getSisteLønnsendringsdato(),
            aktivitetsAvtale.getProsentsats() != null ? IayProsent.fra(aktivitetsAvtale.getProsentsats().getVerdi()) : null)
        ).toList();

        String arbeidsforholdRef = yrkesaktivitet.getArbeidsforholdRef().getReferanse();
        List<PermisjonDto> permisjoner = yrkesaktivitet.getPermisjon().stream()
            .filter(p -> erRelevantForBeregning(p, finnBekreftetPermisjon(yrkesaktivitet, arbeidsforholdOverstyringer)))
            .map(MapIAYTilKalkulusInput::mapTilPermisjonDto)
            .toList();
        return new YrkesaktivitetDto(
            mapTilAktør(yrkesaktivitet.getArbeidsgiver()),
            arbeidsforholdRef != null ? new InternArbeidsforholdRefDto(arbeidsforholdRef) : null,
            ArbeidType.fraKode(yrkesaktivitet.getArbeidType().getKode()),
            aktivitetsAvtaleDtos,
            permisjoner);
    }

    private static Optional<BekreftetPermisjon> finnBekreftetPermisjon(Yrkesaktivitet yrkesaktivitet, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        return arbeidsforholdOverstyringer.stream()
            .filter(os -> Objects.equals(os.getArbeidsgiver(), yrkesaktivitet.getArbeidsgiver())
                && os.getArbeidsforholdRef().gjelderFor(yrkesaktivitet.getArbeidsforholdRef()))
            .map(ArbeidsforholdOverstyring::getBekreftetPermisjon)
            .flatMap(Optional::stream)
            .findFirst();
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

    private static PermisjonDto mapTilPermisjonDto(Permisjon permisjon) {
        return new PermisjonDto(
            new Periode(permisjon.getFraOgMed(), permisjon.getTilOgMed()),
            IayProsent.fra(permisjon.getProsentsats().getVerdi()),
            PermisjonsbeskrivelseType.fraKode(permisjon.getPermisjonsbeskrivelseType().getKode())
        );
    }

    private static InntekterDto mapInntektDto(AktørInntekt inntekt) {

        List<UtbetalingDto> utbetalingDtoer = inntekt.getInntekt().stream().map(MapIAYTilKalkulusInput::mapTilUtbetalingDto).toList();
        if (!utbetalingDtoer.isEmpty()) {
            return new InntekterDto(utbetalingDtoer);
        }
        return null;
    }

    private static UtbetalingDto mapTilUtbetalingDto(Inntekt inntekt) {
        UtbetalingDto utbetalingDto = new UtbetalingDto(InntektskildeType.fraKode(inntekt.getInntektsKilde().getKode()),
            inntekt.getAlleInntektsposter().stream().map(MapIAYTilKalkulusInput::mapTilUtbetalingspostDto).toList());
        if (inntekt.getArbeidsgiver() != null) {
            return utbetalingDto.medArbeidsgiver(mapTilAktør(inntekt.getArbeidsgiver()));
        }
        return utbetalingDto;
    }

    private static UtbetalingsPostDto mapTilUtbetalingspostDto(Inntektspost inntektspost) {
        var utbetalingsPostDto = new UtbetalingsPostDto(
            mapPeriode(inntektspost.getPeriode()),
            InntektspostType.fraKode(inntektspost.getInntektspostType().getKode()),
            no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(inntektspost.getBeløp().getVerdi()));
        if (inntektspost.getInntektYtelseType() != null) {
            var ytelseType = inntektspost.getInntektYtelseType();
            utbetalingsPostDto.setInntektYtelseType(InntektYtelseType.valueOf(ytelseType.name()));
        }
        return utbetalingsPostDto;
    }

    private static OppgittOpptjeningDto mapTilOppgittOpptjeningDto(OppgittOpptjening oppgittOpptjening) {
        if (oppgittOpptjening != null) {
            return new OppgittOpptjeningDto(
                null,
                oppgittOpptjening.getFrilans().map(MapIAYTilKalkulusInput::mapOppgittFrilansOppdragListe).orElse(null),
                mapOppgittEgenNæringListe(oppgittOpptjening.getEgenNæring()),
                mapOppgittArbeidsforholdDto(oppgittOpptjening.getOppgittArbeidsforhold()));
        }
        return null;
    }

    private static List<OppgittEgenNæringDto> mapOppgittEgenNæringListe(List<OppgittEgenNæring> egenNæring) {
        return egenNæring == null ? null : egenNæring.stream().map(MapIAYTilKalkulusInput::mapOppgittEgenNæring).toList();
    }

    private static Periode mapPeriode(DatoIntervallEntitet periode) {
        return new Periode(periode.getFomDato(), periode.getTomDato());
    }

    private static Aktør mapTilAktør(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        return arbeidsgiver.getErVirksomhet() ? new Organisasjon(arbeidsgiver.getOrgnr()) : new AktørIdPersonident(arbeidsgiver.getAktørId().getId());
    }

    private static OppgittEgenNæringDto mapOppgittEgenNæring(OppgittEgenNæring oppgittEgenNæring) {
        return new OppgittEgenNæringDto(
            mapPeriode(oppgittEgenNæring.getPeriode()),
            oppgittEgenNæring.getOrgnr() == null ? null : new Organisasjon(oppgittEgenNæring.getOrgnr()),
            VirksomhetType.fraKode(oppgittEgenNæring.getVirksomhetType().getKode()),
            oppgittEgenNæring.getNyoppstartet(),
            oppgittEgenNæring.getVarigEndring(),
            oppgittEgenNæring.getEndringDato(),
            oppgittEgenNæring.getNyIArbeidslivet(),
            oppgittEgenNæring.getBegrunnelse(),
            no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(oppgittEgenNæring.getBruttoInntekt()));
    }

    private static List<OppgittArbeidsforholdDto> mapOppgittArbeidsforholdDto(List<OppgittArbeidsforhold> arbeidsforhold) {
        if (arbeidsforhold == null) {
            return null;
        }
        return arbeidsforhold.stream().map(MapIAYTilKalkulusInput::mapArbeidsforhold).toList();
    }

    private static OppgittArbeidsforholdDto mapArbeidsforhold(OppgittArbeidsforhold arb) {
        return new OppgittArbeidsforholdDto(mapPeriode(arb.getPeriode()), null);
    }

    private static OppgittFrilansDto mapOppgittFrilansOppdragListe(OppgittFrilans oppgittFrilans) {
        return new OppgittFrilansDto(Boolean.TRUE.equals(oppgittFrilans.getErNyoppstartet()));
    }
}
