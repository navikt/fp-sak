package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;

class FinnOmSøkerHarArbeidsforholdOgInntekt {

    private FinnOmSøkerHarArbeidsforholdOgInntekt() {
        // skjul public constructor
    }

    static boolean finn(Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlagOptional, LocalDate skjæringstidspunkt, AktørId aktørId) {

        if (inntektArbeidYtelseGrunnlagOptional.isPresent()) {
            var grunnlag = inntektArbeidYtelseGrunnlagOptional.get();
            var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(skjæringstidspunkt);

            if (filter.getYrkesaktiviteter().isEmpty()) {
                return false;
            }

            var arbeidsgivere = finnRelevanteArbeidsgivereMedLøpendeAvtaleEllerAvtaleSomErGyldigPåStp(skjæringstidspunkt, filter);
            if (arbeidsgivere.isEmpty()) {
                return false;
            }

            var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(skjæringstidspunkt).filterPensjonsgivende();
            if (inntektFilter.isEmpty()) {
                return false;
            }
            return sjekkOmGjelderRelevantArbeidsgiverOgNærSkjæringstidspunktet(inntektFilter, skjæringstidspunkt, arbeidsgivere);
        }
        return false;
    }

    private static List<Arbeidsgiver> finnRelevanteArbeidsgivereMedLøpendeAvtaleEllerAvtaleSomErGyldigPåStp(LocalDate skjæringstidspunkt,
                                                                                                            YrkesaktivitetFilter filter) {
        return filter.getYrkesaktiviteter().stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .filter(ya -> filter.getAnsettelsesPerioder(ya).stream()
                .anyMatch(aktivitetsAvtale -> periodeOverlapperMedEllerGårOver(skjæringstidspunkt, aktivitetsAvtale)))
            .map(Yrkesaktivitet::getArbeidsgiver)
            .collect(toList());
    }

    private static boolean periodeOverlapperMedEllerGårOver(LocalDate skjæringstidspunkt, AktivitetsAvtale aktivitetsAvtale) {
        return aktivitetsAvtale.getErLøpende() && aktivitetsAvtale.getPeriode().getFomDato().isBefore(skjæringstidspunkt)
            || aktivitetsAvtale.getPeriode().getFomDato().isBefore(skjæringstidspunkt) && aktivitetsAvtale.getPeriode()
            .getTomDato()
            .isAfter(skjæringstidspunkt);
    }

    private static boolean sjekkOmGjelderRelevantArbeidsgiverOgNærSkjæringstidspunktet(InntektFilter filter, LocalDate skjæringstidspunkt,
                                                                                       List<Arbeidsgiver> aktørArbeid) {
        var iDag = LocalDate.now();
        return filter.anyMatchFilter((inntekt, inntektspost) -> aktørArbeid.contains(inntekt.getArbeidsgiver())
            && ErInntektNærSkjæringstidspunkt.erNær(inntektspost, skjæringstidspunkt, iDag));
    }
}
