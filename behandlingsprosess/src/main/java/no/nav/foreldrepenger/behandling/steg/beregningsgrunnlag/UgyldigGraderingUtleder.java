package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.behandling.Skjæringstidspunkt;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AndelGradering;
import no.nav.folketrygdloven.kalkulator.modell.iay.AktivitetsAvtaleDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.AktørArbeidDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YrkesaktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;

public class UgyldigGraderingUtleder {

    private UgyldigGraderingUtleder() {
        // Skjuler default konstruktør
    }

    protected static Optional<ArbeidGraderingMap> finnFørsteUgyldigeAndel(BeregningsgrunnlagInput input) {
        var ytelsespesifiktGrunnlag = input.getYtelsespesifiktGrunnlag();
        if (ytelsespesifiktGrunnlag == null || input.getIayGrunnlag() == null) {
            return Optional.empty();
        }
        if (ytelsespesifiktGrunnlag instanceof ForeldrepengerGrunnlag fpg) {
            Set<AndelGradering> graderinger = fpg.getAktivitetGradering() == null ? Collections.emptySet() : fpg.getAktivitetGradering().getAndelGradering();
            var andelGraderingDatoMap = graderinger.stream()
                .filter(grad -> grad.getArbeidsgiver() != null)
                .map(UgyldigGraderingUtleder::mapTilStartGraderingArbeidsforholdRef)
                .flatMap(Collection::stream)
                .toList();
            var yrkesaktiviteter = input.getIayGrunnlag()
                .getAktørArbeidFraRegister()
                .map(AktørArbeidDto::hentAlleYrkesaktiviteter)
                .orElse(Collections.emptySet());
            List<OpptjeningAktiviteterDto.OpptjeningPeriodeDto> opptjeningsperioder =
                input.getOpptjeningAktiviteter() == null ? Collections.emptyList() : input.getOpptjeningAktiviteter().getOpptjeningPerioder();
            return finnUgyldigAndel(andelGraderingDatoMap, yrkesaktiviteter, opptjeningsperioder, input.getKoblingReferanse().getSkjæringstidspunkt());
        }
        return Optional.empty();

    }

    private static Optional<ArbeidGraderingMap> finnUgyldigAndel(List<ArbeidGraderingMap> andelGraderingDatoMap,
                                                                 Collection<YrkesaktivitetDto> yrkesaktiviteter,
                                                                 List<OpptjeningAktiviteterDto.OpptjeningPeriodeDto> opptjeningsperioder,
                                                                 Skjæringstidspunkt skjæringstidspunkt) {
        var stpOpptjening = skjæringstidspunkt == null ? null : skjæringstidspunkt.getSkjæringstidspunktOpptjening();
        if (stpOpptjening == null) {
            return Optional.empty();
        }
        for (var andel : andelGraderingDatoMap) {
            var andelFinnesIIAY = finnesAndelIIAY(andel, yrkesaktiviteter, stpOpptjening);
            var andelFinnesIOpptjening = finnesAndelIOpptjening(andel, opptjeningsperioder);
            if (!andelFinnesIIAY && !andelFinnesIOpptjening) {
                return Optional.of(andel);
            }
        }
        return Optional.empty();
    }

    private static boolean finnesAndelIOpptjening(ArbeidGraderingMap andel, List<OpptjeningAktiviteterDto.OpptjeningPeriodeDto> opptjeningsperioder) {
        var matchendeAndeler = opptjeningsperioder.stream()
            .filter(opp -> Objects.equals(opp.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null), andel.arbeidsgiverIdent()))
            .filter(opp -> opp.getArbeidsforholdId().gjelderFor(andel.internRef))
            .toList();
        return !matchendeAndeler.isEmpty();
    }

    private static boolean finnesAndelIIAY(ArbeidGraderingMap andel, Collection<YrkesaktivitetDto> yrkesaktiviteter, LocalDate stpOpptjening) {
        var matchendeYrkesaktiviteter = yrkesaktiviteter.stream()
            .filter(ya -> ya.getArbeidsgiver() != null && Objects.equals(ya.getArbeidsgiver().getIdentifikator(), andel.arbeidsgiverIdent))
            .filter(ya -> ya.getArbeidsforholdRef().gjelderFor(andel.internRef))
            .toList();
        var alleAnsettelsesperioder = matchendeYrkesaktiviteter.stream()
            .map(YrkesaktivitetDto::getAlleAnsettelsesperioder)
            .flatMap(Collection::stream)
            .toList();
        return finnesAnsettelsesperiodeSomSlutterEtterSTP(alleAnsettelsesperioder, stpOpptjening);
    }

    private static boolean finnesAnsettelsesperiodeSomSlutterEtterSTP(Collection<AktivitetsAvtaleDto> ansettelsesperioder, LocalDate stpOpptjening) {
        return ansettelsesperioder.stream().anyMatch(ap -> !ap.getPeriode().getTomDato().isBefore(stpOpptjening));
    }

    private static List<ArbeidGraderingMap> mapTilStartGraderingArbeidsforholdRef(AndelGradering andelGradering) {
        var startdatoerGradering = andelGradering.getGraderinger()
            .stream()
            .filter(grad -> grad.getArbeidstidProsent() != null && grad.getArbeidstidProsent().compareTo(BigDecimal.ZERO) > 0
                && grad.getArbeidstidProsent().compareTo(BigDecimal.valueOf(100)) < 0)
            .map(grad -> grad.getPeriode().getFomDato())
            .collect(Collectors.toSet());
        return startdatoerGradering.stream()
            .map(g -> new ArbeidGraderingMap(andelGradering.getArbeidsgiver().getIdentifikator(), andelGradering.getArbeidsforholdRef(), g))
            .collect(Collectors.toList());
    }

    protected record ArbeidGraderingMap(String arbeidsgiverIdent, InternArbeidsforholdRefDto internRef, LocalDate startdatoGradering){};

}
