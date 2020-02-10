package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;

final class UtledStillingsprosent {

    private UtledStillingsprosent() {
        // Skjul default constructor
    }

    static BigDecimal utled(YrkesaktivitetFilter filter, Yrkesaktivitet yrkesaktivitet, LocalDate skjæringstidspunkt) {
        return utled(filter, Collections.singletonList(yrkesaktivitet), skjæringstidspunkt);
    }

    static BigDecimal utled(YrkesaktivitetFilter filter, List<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt){
        if (yrkesaktiviteter.isEmpty()) {
            return BigDecimal.ZERO;
        }
        final List<Yrkesaktivitet> relevanteyrkesaktiviteter = UtledRelevanteYrkesaktiviteterForStillingsprosent.utled(filter,
            yrkesaktiviteter, skjæringstidspunkt);
        final LocalDate oppstartsdatoNærmestStp = UtledOppstartsdatoNærmestStpFraRelevanteYrkesaktiviteter.utled(filter, 
            relevanteyrkesaktiviteter, skjæringstidspunkt);
        return UtledStillingsprosentFraYrkesaktivitetMedOppstartsdatoNærmestStp.utled(filter, 
            relevanteyrkesaktiviteter, skjæringstidspunkt, oppstartsdatoNærmestStp);
    }

}
