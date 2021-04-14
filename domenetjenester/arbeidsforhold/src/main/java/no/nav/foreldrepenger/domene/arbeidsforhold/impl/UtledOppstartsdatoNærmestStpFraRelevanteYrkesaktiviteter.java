package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;

final class UtledOppstartsdatoNærmestStpFraRelevanteYrkesaktiviteter {

    private UtledOppstartsdatoNærmestStpFraRelevanteYrkesaktiviteter() {
        // Skjul default constructor
    }

    static LocalDate utled(YrkesaktivitetFilter filter, List<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt) {
        var fomDato = finnOppstartsdatoNærmestStpForYrkesaktiviteterSomInkludererStp(filter, yrkesaktiviteter, skjæringstidspunkt);
        if (fomDato.isEmpty()) {
            fomDato = finnOppstartsdatoNærmestStpForYrkesaktiviteterSomTilkommerEtterStp(filter, yrkesaktiviteter, skjæringstidspunkt);
        }
        return fomDato.orElse(Tid.TIDENES_BEGYNNELSE);
    }

    private static Optional<LocalDate> finnOppstartsdatoNærmestStpForYrkesaktiviteterSomInkludererStp(YrkesaktivitetFilter filter,
            List<Yrkesaktivitet> yrkesaktiviteter,
            LocalDate skjæringstidspunkt) {
        return filter.getAktivitetsAvtalerForArbeid(yrkesaktiviteter).stream()
                .map(AktivitetsAvtale::getPeriode)
                .filter(periode -> periode.inkluderer(skjæringstidspunkt))
                .map(DatoIntervallEntitet::getFomDato)
                .max(LocalDate::compareTo);
    }

    private static Optional<LocalDate> finnOppstartsdatoNærmestStpForYrkesaktiviteterSomTilkommerEtterStp(YrkesaktivitetFilter filter,
            List<Yrkesaktivitet> yrkesaktiviteter,
            LocalDate skjæringstidspunkt) {
        return filter.getAktivitetsAvtalerForArbeid(yrkesaktiviteter).stream()
                .map(AktivitetsAvtale::getPeriode)
                .filter(datoIntervallEntitet -> datoIntervallEntitet.getFomDato().isAfter(skjæringstidspunkt))
                .map(DatoIntervallEntitet::getFomDato)
                .min(LocalDate::compareTo);
    }

}
