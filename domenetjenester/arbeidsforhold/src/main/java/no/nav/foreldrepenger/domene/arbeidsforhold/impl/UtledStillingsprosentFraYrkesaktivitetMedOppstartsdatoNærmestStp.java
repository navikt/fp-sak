package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

final class UtledStillingsprosentFraYrkesaktivitetMedOppstartsdatoNærmestStp {

    private UtledStillingsprosentFraYrkesaktivitetMedOppstartsdatoNærmestStp() {
        // Skjul default constructor
    }

    static BigDecimal utled(YrkesaktivitetFilter filter, List<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt, LocalDate oppstartsdatoNærmestStp){
        Optional<BigDecimal> stillingsprosent = finnStillingsprosentFraYrkesaktiviteterSomOverlapperStp(filter, yrkesaktiviteter, skjæringstidspunkt, oppstartsdatoNærmestStp);
        if (stillingsprosent.isEmpty()) {
            stillingsprosent = finnStillingsprosentFraYrkesaktivteterSomTilkommerEtterStp(filter, yrkesaktiviteter, skjæringstidspunkt, oppstartsdatoNærmestStp);
        }
        return stillingsprosent.orElse(BigDecimal.ZERO);
    }

    private static Optional<BigDecimal> finnStillingsprosentFraYrkesaktiviteterSomOverlapperStp(YrkesaktivitetFilter filter, 
                                                                                                Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                                                               LocalDate skjæringstidspunkt,
                                                                                               LocalDate oppstartsdatoNærmestStp) {
        
        return filter.getAktivitetsAvtalerForArbeid(yrkesaktiviteter).stream()
            .filter(aa -> aa.getPeriode().inkluderer(skjæringstidspunkt))
            .filter(aa -> !aa.getPeriode().getFomDato().isBefore(oppstartsdatoNærmestStp))
            .map(AktivitetsAvtale::getProsentsats)
            .filter(Objects::nonNull)
            .map(Stillingsprosent::getVerdi)
            .max(BigDecimal::compareTo);
    }

    private static Optional<BigDecimal> finnStillingsprosentFraYrkesaktivteterSomTilkommerEtterStp(YrkesaktivitetFilter filter, List<Yrkesaktivitet> yrkesaktiviteter,
                                                                                                   LocalDate skjæringstidspunkt,
                                                                                                   LocalDate oppstartsdatoNærmestStp) {
        return filter.getAktivitetsAvtalerForArbeid(yrkesaktiviteter).stream()
            .filter(aa -> aa.getPeriode().getFomDato().isAfter(skjæringstidspunkt))
            .filter(aa -> !aa.getPeriode().getFomDato().isAfter(oppstartsdatoNærmestStp))
            .map(AktivitetsAvtale::getProsentsats)
            .filter(Objects::nonNull)
            .map(Stillingsprosent::getVerdi)
            .max(BigDecimal::compareTo);
    }


}
