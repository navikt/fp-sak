package no.nav.foreldrepenger.domene.fp;

import no.nav.folketrygdloven.kalkulator.modell.iay.InntektDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektspostDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.Beløp;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.BesteberegningInntektEntitet;
import no.nav.foreldrepenger.domene.modell.BesteberegningMånedsgrunnlagEntitet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BesteberegningAvvikskontrollerer {
    private static final Logger LOG = LoggerFactory.getLogger(BesteberegningAvvikskontrollerer.class);
    private static final BigDecimal AVVIKSGRENSE = BigDecimal.valueOf(25);
    private static final int MÅNEDER_SIDEN_STP_SOM_BRUKES_FOR_SNITT = 17;

    private BesteberegningAvvikskontrollerer() {
        // Skjuler default
    }

    public static Map<Arbeidsgiver, BigDecimal> finnArbeidsgivereMedAvvikendeInntekt(Long behandlingId,
                                                                                     LocalDate skjæringstidspunkt,
                                                                                     Set<BesteberegningMånedsgrunnlagEntitet> seksBesteMnd,
                                                                                     Collection<InntektDto> inntekterFraRegister) {
        List<BesteberegningInntektEntitet> besteInntekterFraBesteberegning = seksBesteMnd.stream()
            .map(BesteberegningMånedsgrunnlagEntitet::getInntekter)
            .flatMap(Collection::stream).collect(Collectors.toList());
        Set<Arbeidsgiver> arbeidsgivereIBesteberegning = besteInntekterFraBesteberegning.stream()
            .map(BesteberegningInntektEntitet::getArbeidsgiver)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Arbeidsgiver, BigDecimal> avvikPrAG = beregnAvvikPrArbeidsgiver(skjæringstidspunkt, besteInntekterFraBesteberegning,
            arbeidsgivereIBesteberegning,
            inntekterFraRegister);

        avvikPrAG.forEach((ag, avvik) -> {
            if (avvik.compareTo(AVVIKSGRENSE) > 0) {
                String msg = String.format("FP-98256: Det er et avvik på %s prosent hos arbeidsgiver %s på behandling %s", avvik, ag, behandlingId);
                LOG.info(msg);
            }
        });
        // Returnerer noe selv om det ikke er nødvendig for å kunne teste
        return avvikPrAG;
    }

    private static Map<Arbeidsgiver, BigDecimal> beregnAvvikPrArbeidsgiver(LocalDate skjæringstidspunkt,
                                                                           List<BesteberegningInntektEntitet> besteInntekterFraBesteberegning,
                                                                           Set<Arbeidsgiver> arbeidsgivereIBesteberegning,
                                                                           Collection<InntektDto> inntekterFraRegister) {
        Map<Arbeidsgiver, BigDecimal> avvikPrAG = new HashMap<>();
        arbeidsgivereIBesteberegning.forEach(ag -> {
            Collection<InntektspostDto> alleInntektsposterHosAG = inntekterFraRegister.stream()
                .filter(innt -> Objects.equals(innt.getInntektsKilde(), InntektskildeType.INNTEKT_BEREGNING))
                .filter(innt -> innt.getArbeidsgiver() != null && innt.getArbeidsgiver().getIdentifikator().equals(ag.getIdentifikator()))
                .findFirst()
                .map(InntektDto::getAlleInntektsposter)
                .orElse(Collections.emptyList());
            BigDecimal snittlønnSiste10Mnd = finnSnittlønnSiste10Mnd(skjæringstidspunkt, alleInntektsposterHosAG);
            BigDecimal høyesteBBInntekt = finnHøyesteMånedInntektFraBesteberegning(ag, besteInntekterFraBesteberegning);
            BigDecimal diff = høyesteBBInntekt.subtract(snittlønnSiste10Mnd).abs();
            BigDecimal avvikProsent = diff.divide(snittlønnSiste10Mnd, 2, RoundingMode.HALF_EVEN).multiply(BigDecimal.valueOf(100));
            avvikPrAG.put(ag, avvikProsent);
        });
        return avvikPrAG;
    }

    private static BigDecimal finnHøyesteMånedInntektFraBesteberegning(Arbeidsgiver ag, List<BesteberegningInntektEntitet> besteInntekter) {
        return besteInntekter.stream()
            .filter(bi -> Objects.equals(bi.getArbeidsgiver(), ag))
            .map(BesteberegningInntektEntitet::getInntekt)
            .max(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal finnSnittlønnSiste10Mnd(LocalDate skjæringstidspunkt, Collection<InntektspostDto> alleInntektsposterPåAG) {
        LocalDate førsteDagInntektSkalMed = skjæringstidspunkt.minusMonths(MÅNEDER_SIDEN_STP_SOM_BRUKES_FOR_SNITT).withDayOfMonth(1);
        BigDecimal totalInntekt = BigDecimal.ZERO;
        int månederMedInntekt = 0;
        for (int i = 0; i < MÅNEDER_SIDEN_STP_SOM_BRUKES_FOR_SNITT; i++) {
            BigDecimal inntekt = finnInntektIMåned(alleInntektsposterPåAG, førsteDagInntektSkalMed.plusMonths(i));
            if (inntekt.compareTo(BigDecimal.ZERO) > 0) {
                månederMedInntekt++;
                totalInntekt = totalInntekt.add(inntekt);
            }
        }
        if (månederMedInntekt == 0) {
            return BigDecimal.ZERO;
        }
        return totalInntekt.divide(BigDecimal.valueOf(månederMedInntekt), 10, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal finnInntektIMåned(Collection<InntektspostDto> alleInntektsposterPåAG, LocalDate dato) {
        return alleInntektsposterPåAG.stream().filter(innt -> innt.getPeriode().inkluderer(dato))
            .map(InntektspostDto::getBeløp)
            .map(Beløp::getVerdi)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    }
}

