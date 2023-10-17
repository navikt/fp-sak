package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;

public class LoggRefusjonsavvikTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(LoggRefusjonsavvikTjeneste.class);

    private static final BigDecimal PROSENT_AVVIK_REFUSJON_GRENSE = BigDecimal.valueOf(50);


    public static void finnOgLoggAvvik(String saksnummer,
                                       LocalDate stp,
                                       List<Inntektsmelding> nyeInntektsmeldinger,
                                       List<Inntektsmelding> gamleInntektsmeldinger) {
        var nyttSett = finnAlleEndringsdatoerForRefusjon(nyeInntektsmeldinger);
        var gammeltSett = finnAlleEndringsdatoerForRefusjon(gamleInntektsmeldinger);
        var alleEndringsdatoer = Stream.concat(nyttSett.stream(), gammeltSett.stream()).collect(Collectors.toSet());
        alleEndringsdatoer.add(stp);
        Set<RefusjonDiff> alleEndringerIRefusjon = new HashSet<>();
        alleEndringsdatoer.forEach(endringsdato -> {
            var nyRefusjonPåDato = summerRefusjonPåDatoForAlleIM(endringsdato, nyeInntektsmeldinger);
            var gammelRefusjonPåDato = summerRefusjonPåDatoForAlleIM(endringsdato, gamleInntektsmeldinger);
            if (nyRefusjonPåDato.compareTo(gammelRefusjonPåDato) < 0) {
                var diffSum = gammelRefusjonPåDato.subtract(nyRefusjonPåDato);
                var endringProsent = diffSum.divide(gammelRefusjonPåDato, RoundingMode.HALF_EVEN).multiply(BigDecimal.valueOf(100));
                if (endringProsent.compareTo(PROSENT_AVVIK_REFUSJON_GRENSE) >=0) {
                    alleEndringerIRefusjon.add(new RefusjonDiff(saksnummer, stp, endringsdato, endringProsent, diffSum));
                }
            }
        });
        alleEndringerIRefusjon.forEach(endring -> LOG.info("Fant avvik i refusjon: {}", endring));
    }

    private static BigDecimal summerRefusjonPåDatoForAlleIM(LocalDate endringsdato, List<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .map(im -> finnRefusjonPåDatoForIM(endringsdato, im))
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal finnRefusjonPåDatoForIM(LocalDate endringsdato, Inntektsmelding im) {
        if (im.getRefusjonOpphører() != null &&
            !im.getRefusjonOpphører().isAfter(endringsdato)) {
            return BigDecimal.ZERO;
        }
        if (im.getEndringerRefusjon().isEmpty()) {
            return im.getRefusjonBeløpPerMnd().getVerdi();
        }
        return im.getEndringerRefusjon().stream()
            .filter(endring -> !endring.getFom().isAfter(endringsdato))
            .max(Comparator.comparing(Refusjon::getFom))
            .map(endring -> endring.getRefusjonsbeløp().getVerdi())
            .orElse(BigDecimal.ZERO);
    }

    private static Set<LocalDate> finnAlleEndringsdatoerForRefusjon(List<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .map(LoggRefusjonsavvikTjeneste::finnEndringsdatoerForRefusjon)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private static Set<LocalDate> finnEndringsdatoerForRefusjon(Inntektsmelding im) {
        return im.getEndringerRefusjon().stream().map(Refusjon::getFom).collect(Collectors.toSet());
    }

    private record RefusjonDiff(String saksnummer, LocalDate skjæringstidspunkt, LocalDate endringsdato, BigDecimal endringProsent, BigDecimal endringSum) {
        @Override
        public String toString() {
            return "RefusjonDiff{" + "saksnummer='" + saksnummer + '\'' + ", skjæringstidspunkt=" + skjæringstidspunkt + ", endringsdato="
                + endringsdato + ", endringProsent=" + endringProsent + ", endringSum=" + endringSum + '}';
        }
    }
}
