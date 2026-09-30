package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;

/**
 * Finner yrkesaktivitetene (arbeidsforholdene) fra aa-reg som er relevante når vi vurderer om det kreves inntektsmelding
 * fra en arbeidsgiver på skjæringstidspunktet.
 */
final class RelevanteYrkesaktiviteterForInntektsmelding {

    private static final Set<ArbeidType> AA_REG_TYPER = Set.of(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ArbeidType.MARITIMT_ARBEIDSFORHOLD,
        ArbeidType.FORENKLET_OPPGJØRSORDNING);

    private RelevanteYrkesaktiviteterForInntektsmelding() {
    }

    static YrkesaktivitetFilter lagFilter(InntektArbeidYtelseGrunnlag grunnlag, AktørId aktørId, LocalDate stp) {
        return new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(stp);
    }

    static List<Yrkesaktivitet> finn(InntektArbeidYtelseGrunnlag grunnlag, AktørId aktørId, LocalDate stp) {
        return finn(lagFilter(grunnlag, aktørId, stp), stp);
    }

    static List<Yrkesaktivitet> finn(YrkesaktivitetFilter filter, LocalDate stp) {
        return filter.getYrkesaktiviteter().stream()
            .filter(ya -> AA_REG_TYPER.contains(ya.getArbeidType()))
            .filter(ya -> harRelevantAnsettelsesperiodeSomDekkerAngittDato(filter, ya, stp))
            .toList();
    }

    private static boolean harRelevantAnsettelsesperiodeSomDekkerAngittDato(YrkesaktivitetFilter filter, Yrkesaktivitet yrkesaktivitet, LocalDate dato) {
        if (!yrkesaktivitet.erArbeidsforhold()) {
            return false;
        }
        var ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet);
        var jobberPåStp = ansettelsesPerioder.stream().anyMatch(avtale -> avtale.getPeriode().inkluderer(dato));
        if (jobberPåStp) {
            return true;
        }
        var jobberDagenFørStp = ansettelsesPerioder.stream().anyMatch(avtale -> avtale.getPeriode().inkluderer(dato.minusDays(1)));
        return jobberDagenFørStp && filter.getAlleYrkesaktiviteter().stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .filter(aktivitet -> Objects.equals(aktivitet.getArbeidsgiver(), yrkesaktivitet.getArbeidsgiver()))
            .flatMap(aktivitet -> filter.getAnsettelsesPerioder(aktivitet).stream())
            .anyMatch(avtale -> avtale.getPeriode().getFomDato().equals(dato));
    }
}
