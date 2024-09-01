package no.nav.foreldrepenger.domene.mappers.input;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
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

    public static List<KravperioderPrArbeidsforhold> map(BehandlingReferanse referanse, Skjæringstidspunkt stp,
                                                         Collection<Inntektsmelding> alleInntektsmeldingerPåSak,
                                                         InntektArbeidYtelseGrunnlag grunnlagDto) {
        var aktiveInntektsmeldinger = grunnlagDto.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(Collections.emptyList());
        var filter = new YrkesaktivitetFilter(grunnlagDto.getArbeidsforholdInformasjon(),
            grunnlagDto.getAktørArbeidFraRegister(referanse.aktørId()));
        var yrkesaktiviteter = filter.getYrkesaktiviteter();
        if (yrkesaktiviteter.isEmpty()) {
            return Collections.emptyList();
        }
        var sisteIMPrArbeidsforhold = finnSisteInntektsmeldingMedRefusjonPrArbeidsforhold(aktiveInntektsmeldinger);
        var gruppertPrArbeidsforhold = finnInntektsmeldingMedRefusjonPrArbeidsforhold(alleInntektsmeldingerPåSak);

        return gruppertPrArbeidsforhold
            .entrySet()
            .stream()
            .filter(e -> sisteIMPrArbeidsforhold.containsKey(e.getKey()))
            .map(e -> mapTilKravPrArbeidsforhold(stp, yrkesaktiviteter, sisteIMPrArbeidsforhold, e))
            .toList();
    }

    private static Map<Kravnøkkel, List<Inntektsmelding>> finnInntektsmeldingMedRefusjonPrArbeidsforhold(Collection<Inntektsmelding> inntektsmeldinger) {
        var inntektsmeldingerMedRefusjonskrav = filtrerKunRefusjon(inntektsmeldinger);
        return grupper(inntektsmeldingerMedRefusjonskrav);
    }

    private static List<Inntektsmelding> filtrerKunRefusjon(Collection<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .filter(im -> im.getRefusjonBeløpPerMnd() != null && !im.getRefusjonBeløpPerMnd().erNullEllerNulltall() || im.getEndringerRefusjon()
                .stream()
                .anyMatch(e -> !e.getRefusjonsbeløp().erNullEllerNulltall()))
            .toList();
    }

    private static Map<Kravnøkkel, Inntektsmelding> finnSisteInntektsmeldingMedRefusjonPrArbeidsforhold(Collection<Inntektsmelding> inntektsmeldinger) {
        return grupperEneste(filtrerKunRefusjon(inntektsmeldinger));
    }

    private static KravperioderPrArbeidsforhold mapTilKravPrArbeidsforhold(Skjæringstidspunkt stp,
                                                                           Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                                           Map<Kravnøkkel, Inntektsmelding> sisteIMPrArbeidsforhold,
                                                                           Map.Entry<Kravnøkkel, List<Inntektsmelding>> e) {
        var alleTidligereKravPerioder = lagPerioderForAlle(stp, yrkesaktiviteter, e.getValue());
        var sistePerioder = lagPerioderForKrav(
            sisteIMPrArbeidsforhold.get(e.getKey()),
            stp.getSkjæringstidspunktOpptjening(),
            yrkesaktiviteter);
        return new KravperioderPrArbeidsforhold(
            mapTilAktør(e.getKey().arbeidsgiver),
            mapReferanse(e.getKey().referanse),
            alleTidligereKravPerioder,
            sistePerioder);
    }

    private static Aktør mapTilAktør(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver.getErVirksomhet()) {
            return new Organisasjon(arbeidsgiver.getIdentifikator());
        }
        return new AktørIdPersonident(arbeidsgiver.getIdentifikator());
    }

    private static List<PerioderForKrav> lagPerioderForAlle(Skjæringstidspunkt stp,
                                                            Collection<Yrkesaktivitet> yrkesaktiviteter, List<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .map(im -> lagPerioderForKrav(im, stp.getSkjæringstidspunktOpptjening(), yrkesaktiviteter))
            .toList();
    }

    private static Map<Kravnøkkel, List<Inntektsmelding>> grupper(List<Inntektsmelding> inntektsmeldingerMedRefusjonskrav) {
        Map<Kravnøkkel, List<Inntektsmelding>> resultMap = inntektsmeldingerMedRefusjonskrav.stream()
            .map(im -> new Kravnøkkel(im.getArbeidsgiver(), im.getArbeidsforholdRef()))
            .distinct()
            .collect(Collectors.toMap(n -> n, n -> new ArrayList<>()));
        inntektsmeldingerMedRefusjonskrav.forEach(im -> {
            var nøkler = finnKeysSomSkalHaInntektsmelding(resultMap, im);
            nøkler.forEach(n -> resultMap.get(n).add(im));
        });
        return resultMap;
    }

    private static Set<Kravnøkkel> finnKeysSomSkalHaInntektsmelding(Map<Kravnøkkel, List<Inntektsmelding>> resultMap, Inntektsmelding im) {
        return resultMap.keySet().stream().filter(n -> n.arbeidsgiver.equals(im.getArbeidsgiver()) && n.referanse.gjelderFor(im.getArbeidsforholdRef())).collect(Collectors.toSet());
    }

    private static Map<Kravnøkkel, Inntektsmelding> grupperEneste(List<Inntektsmelding> inntektsmeldingerMedRefusjonskrav) {
        return inntektsmeldingerMedRefusjonskrav.stream()
            .collect(Collectors.toMap(im ->
                    new Kravnøkkel(im.getArbeidsgiver(), im.getArbeidsforholdRef()),
                im -> im));
    }

    private static no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto mapReferanse(InternArbeidsforholdRef arbeidsforholdRef) {
        return arbeidsforholdRef.getReferanse() == null ? null : new no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto(arbeidsforholdRef.getReferanse());
    }

    private static PerioderForKrav lagPerioderForKrav(Inntektsmelding im,
                                                         LocalDate skjæringstidspunktBeregning,
                                                         Collection<Yrkesaktivitet> yrkesaktiviteter) {
        var startRefusjon = finnStartdatoRefusjon(im, skjæringstidspunktBeregning, yrkesaktiviteter);
        return new PerioderForKrav(im.getInnsendingstidspunkt().toLocalDate(), mapRefusjonsperioder(im, startRefusjon));
    }

    private static LocalDate finnStartdatoRefusjon(Inntektsmelding im, LocalDate skjæringstidspunktBeregning,
                                                   Collection<Yrkesaktivitet> yrkesaktiviteter) {
        LocalDate startRefusjon;
        var startDatoArbeid = yrkesaktiviteter.stream()
            .filter(y -> y.getArbeidsgiver().getIdentifikator().equals(im.getArbeidsgiver().getIdentifikator()) &&
                y.getArbeidsforholdRef().gjelderFor(im.getArbeidsforholdRef()))
            .flatMap(y -> y.getAlleAktivitetsAvtaler().stream())
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .map(AktivitetsAvtale::getPeriode)
            .filter(periode -> !periode.getTomDato().isBefore(skjæringstidspunktBeregning))
            .map(DatoIntervallEntitet::getFomDato)
            .min(Comparator.naturalOrder())
            .orElse(skjæringstidspunktBeregning);
        if (startDatoArbeid.isAfter(skjæringstidspunktBeregning)) {
            startRefusjon = im.getStartDatoPermisjon().filter(localDate -> !startDatoArbeid.isAfter(localDate)).orElse(startDatoArbeid);
        } else {
            startRefusjon = skjæringstidspunktBeregning;
        }
        return startRefusjon;
    }

    private static List<Refusjonsperiode> mapRefusjonsperioder(Inntektsmelding im, LocalDate startdatoRefusjon) {
        var alleSegmenter = new ArrayList<LocalDateSegment<BigDecimal>>();
        if (im.getRefusjonOpphører() != null && im.getRefusjonOpphører().isBefore(startdatoRefusjon)) {
            return Collections.emptyList();
        }
        if (!(im.getRefusjonBeløpPerMnd() == null || im.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) == 0)) {
            alleSegmenter.add(new LocalDateSegment<>(startdatoRefusjon, Tid.TIDENES_ENDE, im.getRefusjonBeløpPerMnd().getVerdi()));
        }

        alleSegmenter.addAll(im.getEndringerRefusjon().stream().map(e ->
            new LocalDateSegment<>(e.getFom(), Tid.TIDENES_ENDE, e.getRefusjonsbeløp().getVerdi())
        ).toList());

        if (im.getRefusjonOpphører() != null && !im.getRefusjonOpphører().equals(Tid.TIDENES_ENDE)) {
            alleSegmenter.add(new LocalDateSegment<>(im.getRefusjonOpphører().plusDays(1), Tid.TIDENES_ENDE, BigDecimal.ZERO));
        }

        var refusjonTidslinje = new LocalDateTimeline<>(alleSegmenter, (interval, lhs, rhs) -> {
            if (lhs.getFom().isBefore(rhs.getFom())) {
                return new LocalDateSegment<>(interval, rhs.getValue());
            }
            return new LocalDateSegment<>(interval, lhs.getValue());
        });
        return refusjonTidslinje.stream()
            .map(r -> new Refusjonsperiode(new Periode(r.getFom(), r.getTom()), Beløp.fra(r.getValue())))
            .toList();

    }

    public record Kravnøkkel(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef referanse) { }
}
