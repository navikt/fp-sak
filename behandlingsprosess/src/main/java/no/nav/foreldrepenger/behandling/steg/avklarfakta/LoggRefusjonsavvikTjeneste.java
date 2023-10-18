package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.vedtak.konfig.Tid;

public class LoggRefusjonsavvikTjeneste {
    private static final BigDecimal PROSENT_AVVIK_REFUSJON_GRENSE = BigDecimal.valueOf(50);


    public static List<RefusjonDiff> finnOgLoggAvvik(String saksnummer,
                                                     LocalDate stp,
                                                     List<Inntektsmelding> nyeInntektsmeldinger,
                                                     List<Inntektsmelding> gamleInntektsmeldinger) {
        var nyttSett = finnAlleEndringsdatoerForRefusjon(nyeInntektsmeldinger);
        var gammeltSett = finnAlleEndringsdatoerForRefusjon(gamleInntektsmeldinger);
        var alleEndringsdatoer = Stream.concat(nyttSett.stream(), gammeltSett.stream()).collect(Collectors.toSet());
        alleEndringsdatoer.add(stp);
        List<RefusjonDiff> alleEndringerIRefusjon = new ArrayList<>();
        alleEndringsdatoer.forEach(endringsdato -> {
            var nyRefusjonPåDato = summerRefusjonPåDatoForAlleIM(endringsdato, nyeInntektsmeldinger);
            var gammelRefusjonPåDato = summerRefusjonPåDatoForAlleIM(endringsdato, gamleInntektsmeldinger);
            if (nyRefusjonPåDato.compareTo(gammelRefusjonPåDato) < 0) {
                var diffSum = gammelRefusjonPåDato.subtract(nyRefusjonPåDato);
                var endringProsent = diffSum.multiply(BigDecimal.valueOf(100)).divide(gammelRefusjonPåDato, 2, RoundingMode.HALF_EVEN);
                if (endringProsent.compareTo(PROSENT_AVVIK_REFUSJON_GRENSE) >=0) {
                    alleEndringerIRefusjon.add(new RefusjonDiff(saksnummer, stp, endringsdato, endringProsent, diffSum));
                }
            }
        });
        return alleEndringerIRefusjon;
    }

    private static BigDecimal summerRefusjonPåDatoForAlleIM(LocalDate endringsdato, List<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .map(im -> finnRefusjonPåDatoForIM(endringsdato, im))
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal finnRefusjonPåDatoForIM(LocalDate endringsdato, Inntektsmelding im) {
        if (im.getRefusjonOpphører() != null &&
            im.getRefusjonOpphører().isBefore(endringsdato)) {
            return BigDecimal.ZERO;
        }
        if (im.getEndringerRefusjon().isEmpty()) {
            return im.getRefusjonBeløpPerMnd().getVerdi();
        }
        return im.getEndringerRefusjon().stream()
            .filter(endring -> !endring.getFom().isAfter(endringsdato))
            .max(Comparator.comparing(Refusjon::getFom))
            .map(endring -> endring.getRefusjonsbeløp().getVerdi())
            .orElse(im.getRefusjonBeløpPerMnd().getVerdi());
    }

    private static Set<LocalDate> finnAlleEndringsdatoerForRefusjon(List<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .map(LoggRefusjonsavvikTjeneste::finnEndringsdatoerForRefusjon)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private static Set<LocalDate> finnEndringsdatoerForRefusjon(Inntektsmelding im) {
        var settMedEndringer = im.getEndringerRefusjon()
            .stream()
            .map(Refusjon::getFom)
            .collect(Collectors.toSet());
        if (im.getRefusjonOpphører() != null && !im.getRefusjonOpphører().equals(Tid.TIDENES_ENDE)) {
            settMedEndringer.add(im.getRefusjonOpphører().plusDays(1));
        }
        return settMedEndringer;
    }

    protected record RefusjonDiff(String saksnummer, LocalDate skjæringstidspunkt, LocalDate endringsdato, BigDecimal endringProsent, BigDecimal endringSum) {
        @Override
        public String toString() {
            return "RefusjonDiff{" + "saksnummer='" + saksnummer + '\'' + ", skjæringstidspunkt=" + skjæringstidspunkt + ", endringsdato="
                + endringsdato + ", endringProsent=" + endringProsent + ", endringSum=" + endringSum + '}';
        }
    }
}
