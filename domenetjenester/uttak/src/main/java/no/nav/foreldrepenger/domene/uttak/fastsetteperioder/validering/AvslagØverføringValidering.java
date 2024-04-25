package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;

class AvslagØverføringValidering implements OverstyrUttakPerioderValidering {

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        for (var p : nyePerioder) {
            if (p.isOverføringAvslått() && trekkerDagerFraSøktKonto(p)) {
                throw OverstyrUttakValideringFeil.trekkerDagerFraSøktKontoVedAvslagAvOverføring(p);
            }
        }
    }

    private boolean trekkerDagerFraSøktKonto(ForeldrepengerUttakPeriode p) {
        var søktKonto = p.getSøktKonto();
        var kontoerSomTrekkesDager = p.getAktiviteter()
            .stream()
            .filter(a -> a.getTrekkdager().merEnn0())
            .map(ForeldrepengerUttakPeriodeAktivitet::getTrekkonto)
            .collect(Collectors.toSet());
        return kontoerSomTrekkesDager.contains(søktKonto);
    }
}
