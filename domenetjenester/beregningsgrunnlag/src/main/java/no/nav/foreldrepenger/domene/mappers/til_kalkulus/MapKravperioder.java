package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.beregning.v1.KravperioderPrArbeidsforhold;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PerioderForKrav;
import no.nav.folketrygdloven.kalkulus.beregning.v1.Refusjonsperiode;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.Beløp;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.konfig.Tid;

public class MapKravperioder {

    // TODO: Vi må utvide InntektsmeldingDto til å ha med innsendingsdato (her innsendingstidspunkt.toLocalDate())
    // TODO: Vi må utvide KalkulatorInputDto med alleInntektsmeldingerPåSak
    // TODO: Vi trenger ikke BehandlingReferanse lenger
    // TODO: Vurdere å endre til å bruke skjæringstidspunktBeregning i stedet, siden vi flytter oss til et senere steg

    public static List<KravperioderPrArbeidsforhold> map(BehandlingReferanse referanse,
                                                         LocalDate skjæringstidspunktOpptjening,
                                                         Collection<Inntektsmelding> alleInntektsmeldingerPåSak,
                                                         InntektArbeidYtelseGrunnlag grunnlagDto) {
        // Henter yrkesaktiviteter for aktør, hvis ingen, returner tom liste
        var yrkesaktiviteter = new YrkesaktivitetFilter(grunnlagDto.getArbeidsforholdInformasjon(),
            grunnlagDto.getAktørArbeidFraRegister(referanse.aktørId())).getYrkesaktiviteter();
        if (yrkesaktiviteter.isEmpty()) {
            return Collections.emptyList();
        }

        var sisteIMPrArbeidsforhold = finnSisteInntektsmeldingMedRefusjonPrArbeidsforhold(grunnlagDto);
        var gruppertPrArbeidsforhold = grupperInntektsmeldingerMedRefusjonPrArbeidsforhold(alleInntektsmeldingerPåSak);

        return gruppertPrArbeidsforhold.entrySet()
            .stream()
            .filter(kravnøkkelOgInntektsmeldinger -> sisteIMPrArbeidsforhold.containsKey(kravnøkkelOgInntektsmeldinger.getKey()))
            .map(kravnøkkelOgInntektsmeldinger -> mapTilKravPrArbeidsforhold(skjæringstidspunktOpptjening, yrkesaktiviteter, sisteIMPrArbeidsforhold,
                kravnøkkelOgInntektsmeldinger))
            .flatMap(Optional::stream)
            .toList();
    }

    private static Map<Kravnøkkel, Inntektsmelding> finnSisteInntektsmeldingMedRefusjonPrArbeidsforhold(InntektArbeidYtelseGrunnlag grunnlagDto) {
        var aktiveInntektsmeldinger = grunnlagDto.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(Collections.emptyList());
        var inntektsmeldingerMedRefusjonskrav = filtrerKunRefusjon(aktiveInntektsmeldinger);
        return inntektsmeldingerMedRefusjonskrav.stream()
            .collect(Collectors.toMap(im -> new Kravnøkkel(im.getArbeidsgiver(), im.getArbeidsforholdRef()), im -> im));
    }

    private static Map<Kravnøkkel, List<Inntektsmelding>> grupperInntektsmeldingerMedRefusjonPrArbeidsforhold(Collection<Inntektsmelding> inntektsmeldinger) {
        var inntektsmeldingerMedRefusjonskrav = filtrerKunRefusjon(inntektsmeldinger);
        var grupperteInntektsmeldinger = lagKravnøklerForInntektsmeldinger(inntektsmeldingerMedRefusjonskrav);
        inntektsmeldingerMedRefusjonskrav.forEach(im -> finnKeysSomSkalHaInntektsmelding(grupperteInntektsmeldinger, im).forEach(
            kravnøkkel -> grupperteInntektsmeldinger.get(kravnøkkel).add(im)));
        return grupperteInntektsmeldinger;
    }

    private static List<Inntektsmelding> filtrerKunRefusjon(Collection<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream().filter(MapKravperioder::harRefusjonsbeløpUliktNull).toList();
    }

    private static boolean harRefusjonsbeløpUliktNull(Inntektsmelding im) {
        return !im.getRefusjonBeløpPerMnd().erNullEllerNulltall() || im.getEndringerRefusjon()
            .stream()
            .anyMatch(e -> !e.getRefusjonsbeløp().erNullEllerNulltall());
    }

    private static Map<Kravnøkkel, List<Inntektsmelding>> lagKravnøklerForInntektsmeldinger(List<Inntektsmelding> inntektsmeldingerMedRefusjonskrav) {
        return inntektsmeldingerMedRefusjonskrav.stream()
            .map(im -> new Kravnøkkel(im.getArbeidsgiver(), im.getArbeidsforholdRef()))
            .distinct()
            .collect(Collectors.toMap(n -> n, n -> new ArrayList<>()));
    }

    private static Set<Kravnøkkel> finnKeysSomSkalHaInntektsmelding(Map<Kravnøkkel, List<Inntektsmelding>> kravnøklerOgInntektsmeldinger,
                                                                    Inntektsmelding im) {
        return kravnøklerOgInntektsmeldinger.keySet()
            .stream()
            .filter(nøkkel -> nøkkel.arbeidsgiver.equals(im.getArbeidsgiver()) && nøkkel.referanse.gjelderFor(im.getArbeidsforholdRef()))
            .collect(Collectors.toSet());
    }

    private static Optional<KravperioderPrArbeidsforhold> mapTilKravPrArbeidsforhold(LocalDate skjæringstidspunktOpptjening,
                                                                                     Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                                                     Map<Kravnøkkel, Inntektsmelding> sisteIMPrArbeidsforhold,
                                                                                     Map.Entry<Kravnøkkel, List<Inntektsmelding>> kravnøkkelOgInntektsmeldinger) {
        var alleTidligereKravPerioder = lagPerioderForAlle(skjæringstidspunktOpptjening, yrkesaktiviteter, kravnøkkelOgInntektsmeldinger.getValue());
        var sistePerioder = lagPerioderForKrav(sisteIMPrArbeidsforhold.get(kravnøkkelOgInntektsmeldinger.getKey()), skjæringstidspunktOpptjening,
            yrkesaktiviteter);
        // Her kan vi ende opp uten refusjonsperioder hvis stp har flyttet seg til å være før opphørsdato i inntektsmeldingen,
        // legger på filtrering for å ikke ta med disse da de er uinteressante
        if (alleTidligereKravPerioder.isEmpty() || sistePerioder.getRefusjonsperioder().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new KravperioderPrArbeidsforhold(mapTilAktør(kravnøkkelOgInntektsmeldinger.getKey().arbeidsgiver),
            mapReferanse(kravnøkkelOgInntektsmeldinger.getKey().referanse), alleTidligereKravPerioder, sistePerioder));
    }

    private static List<PerioderForKrav> lagPerioderForAlle(LocalDate skjæringstidspunktOpptjening,
                                                            Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                            List<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream().map(im -> lagPerioderForKrav(im, skjæringstidspunktOpptjening, yrkesaktiviteter))
            // Her kan vi ende opp uten refusjonsperioder hvis stp har flyttet seg til å være før opphørsdato i inntektsmeldingen,
            // legger på filtrering for å ikke ta med disse da de er uinteressante
            .filter(kp -> !kp.getRefusjonsperioder().isEmpty()).toList();
    }

    private static PerioderForKrav lagPerioderForKrav(Inntektsmelding im,
                                                      LocalDate skjæringstidspunktOpptjening,
                                                      Collection<Yrkesaktivitet> yrkesaktiviteter) {
        var startRefusjon = finnStartdatoRefusjon(im, skjæringstidspunktOpptjening, yrkesaktiviteter);
        return new PerioderForKrav(im.getInnsendingstidspunkt().toLocalDate(), mapRefusjonsperioder(im, startRefusjon));
    }

    private static LocalDate finnStartdatoRefusjon(Inntektsmelding im,
                                                   LocalDate skjæringstidspunktOpptjening,
                                                   Collection<Yrkesaktivitet> yrkesaktiviteter) {
        var startDatoArbeid = yrkesaktiviteter.stream()
            .filter(y -> y.getArbeidsgiver().getIdentifikator().equals(im.getArbeidsgiver().getIdentifikator()) && y.getArbeidsforholdRef()
                .gjelderFor(im.getArbeidsforholdRef()))
            .flatMap(y -> y.getAlleAktivitetsAvtaler().stream())
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .map(AktivitetsAvtale::getPeriode)
            .filter(periode -> !periode.getTomDato().isBefore(skjæringstidspunktOpptjening))
            .map(DatoIntervallEntitet::getFomDato)
            .min(Comparator.naturalOrder())
            .orElse(skjæringstidspunktOpptjening);

        return startDatoArbeid.isAfter(skjæringstidspunktOpptjening) ? im.getStartDatoPermisjon()
            .filter(startDatoPermisjon -> !startDatoArbeid.isAfter(startDatoPermisjon))
            .orElse(startDatoArbeid) : skjæringstidspunktOpptjening;
    }

    private static List<Refusjonsperiode> mapRefusjonsperioder(Inntektsmelding im, LocalDate startdatoRefusjon) {
        if (opphørerRefusjonFørStartdato(im, startdatoRefusjon)) {
            return Collections.emptyList();
        }

        var refusjonTidslinje = new LocalDateTimeline<>(opprettRefusjonSegmenter(im, startdatoRefusjon),
            (interval, lhs, rhs) -> lhs.getFom().isBefore(rhs.getFom()) ? new LocalDateSegment<>(interval, rhs.getValue()) : new LocalDateSegment<>(
                interval, lhs.getValue()));

        return refusjonTidslinje.stream()
            .map(segment -> new Refusjonsperiode(new Periode(segment.getFom(), segment.getTom()), Beløp.fra(segment.getValue())))
            .toList();
    }

    private static boolean opphørerRefusjonFørStartdato(Inntektsmelding im, LocalDate startdatoRefusjon) {
        return im.getRefusjonOpphører() != null && im.getRefusjonOpphører().isBefore(startdatoRefusjon);
    }

    private static ArrayList<LocalDateSegment<BigDecimal>> opprettRefusjonSegmenter(Inntektsmelding im, LocalDate startdatoRefusjon) {
        var segmenter = new ArrayList<LocalDateSegment<BigDecimal>>();
        if (erRefusjonsbeløpUliktNull(im)) {
            leggTilSegment(segmenter, startdatoRefusjon, im.getRefusjonBeløpPerMnd().getVerdi());
        }

        im.getEndringerRefusjon().forEach(endring -> leggTilSegment(segmenter, endring.getFom(), endring.getRefusjonsbeløp().getVerdi()));

        if (harRefusjonOpphørsdato(im)) {
            leggTilSegment(segmenter, im.getRefusjonOpphører().plusDays(1), BigDecimal.ZERO);
        }
        return segmenter;
    }

    private static boolean erRefusjonsbeløpUliktNull(Inntektsmelding im) {
        return !(im.getRefusjonBeløpPerMnd() == null || im.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) == 0);
    }

    private static boolean harRefusjonOpphørsdato(Inntektsmelding im) {
        return im.getRefusjonOpphører() != null && !im.getRefusjonOpphører().equals(Tid.TIDENES_ENDE);
    }

    private static void leggTilSegment(ArrayList<LocalDateSegment<BigDecimal>> segmenter, LocalDate fom, BigDecimal verdi) {
        segmenter.add(new LocalDateSegment<>(fom, Tid.TIDENES_ENDE, verdi));
    }

    private static Aktør mapTilAktør(Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver.getErVirksomhet() ? new Organisasjon(arbeidsgiver.getIdentifikator()) : new AktørIdPersonident(
            arbeidsgiver.getIdentifikator());
    }

    private static no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto mapReferanse(InternArbeidsforholdRef arbeidsforholdRef) {
        return arbeidsforholdRef.getReferanse() == null ? null : new no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto(
            arbeidsforholdRef.getReferanse());
    }

    public record Kravnøkkel(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef referanse) {
    }
}
