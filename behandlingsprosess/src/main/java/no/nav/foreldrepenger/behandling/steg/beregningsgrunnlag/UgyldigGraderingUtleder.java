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
            return validerAndelGraderingMotYrkesaktiviteter(andelGraderingDatoMap, yrkesaktiviteter, input.getKoblingReferanse().getSkjæringstidspunkt());
        }
        return Optional.empty();

    }

    private static Optional<ArbeidGraderingMap> validerAndelGraderingMotYrkesaktiviteter(List<ArbeidGraderingMap> andelGraderingDatoMap,
                                                                                  Collection<YrkesaktivitetDto> yrkesaktiviteter,
                                                                                  Skjæringstidspunkt skjæringstidspunkt) {
        var stpOpptjening = skjæringstidspunkt == null ? null : skjæringstidspunkt.getSkjæringstidspunktOpptjening();
        if (stpOpptjening == null) {
            return Optional.empty();
        }
        for (ArbeidGraderingMap andel : andelGraderingDatoMap) {
            var ugyldigAndel = finnAndelUtenAnsettelsesperiode(andel, yrkesaktiviteter, stpOpptjening);
            if (ugyldigAndel.isPresent()) {
                return ugyldigAndel;
            }
        }
        return Optional.empty();
    }

    private static Optional<ArbeidGraderingMap> finnAndelUtenAnsettelsesperiode(ArbeidGraderingMap andel, Collection<YrkesaktivitetDto> yrkesaktiviteter, LocalDate stpOpptjening) {
        var yrkesaktivitet = yrkesaktiviteter.stream()
            .filter(ya -> ya.getArbeidsgiver() != null && Objects.equals(ya.getArbeidsgiver().getIdentifikator(), andel.arbeidsgiverIdent))
            .filter(ya -> Objects.equals(ya.getArbeidsforholdRef().getReferanse(), andel.internRef))
            .findFirst();
        var ansettelsesperioder = yrkesaktivitet.map(YrkesaktivitetDto::getAlleAnsettelsesperioder).orElse(Collections.emptySet());
        if (!finnesAnsettelsesperiodeSomSlutterEtterSTP(ansettelsesperioder, stpOpptjening)) {
            return Optional.of(andel);

        }
        return Optional.empty();
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
            .map(g -> new ArbeidGraderingMap(andelGradering.getArbeidsgiver().getIdentifikator(), andelGradering.getArbeidsforholdRef().getReferanse(), g))
            .collect(Collectors.toList());
    }

    protected record ArbeidGraderingMap(String arbeidsgiverIdent, String internRef, LocalDate startdatoGradering){};

}
